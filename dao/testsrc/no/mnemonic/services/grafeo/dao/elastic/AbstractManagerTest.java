package no.mnemonic.services.grafeo.dao.elastic;

import no.mnemonic.services.grafeo.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.services.grafeo.dao.elastic.document.FactDocument;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static no.mnemonic.services.grafeo.dao.elastic.DocumentTestUtils.createFactDocument;

@Testcontainers
public abstract class AbstractManagerTest {

  static final long DAY1 = Instant.parse("2022-01-01T12:00:00.000Z").toEpochMilli();
  static final long DAY2 = Instant.parse("2022-01-02T12:00:00.000Z").toEpochMilli();
  static final long DAY3 = Instant.parse("2022-01-03T12:00:00.000Z").toEpochMilli();

  private static ClientFactory clientFactory;
  private FactSearchManager factSearchManager;

  // Need to specify the exact version here because Elastic doesn't publish images with the 'latest' tag.
  // Usually this should be the same version as the ElasticSearch client used.
  @Container
  public static final ElasticsearchContainer elastic = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.17.4")
          .withEnv("xpack.security.enabled", "false")
          // Required for deleting all indices at once using "_all".
          .withEnv("action.destructive_requires_name", "false");

  @BeforeAll
  public static void setup() {
    clientFactory = ClientFactory.builder()
            .setPort(elastic.getMappedPort(9200))
            .addContactPoint(elastic.getHost())
            .build();
    clientFactory.startComponent();
  }

  @AfterAll
  public static void teardown() {
    clientFactory.stopComponent();
  }

  @BeforeEach
  public void initialize() {
    factSearchManager = new FactSearchManager(clientFactory)
            .setTestEnvironment(true)
            .setSearchScrollExpiration("5s")
            .setSearchScrollSize(1);
    factSearchManager.startComponent();
  }

  @AfterEach
  public void cleanup() throws Exception {
    // Clean up all indices after each run.
    elastic.execInContainer("curl", "--silent", "--show-error", "-XDELETE", "localhost:9200/_all");
  }

  protected FactSearchManager getFactSearchManager() {
    return factSearchManager;
  }

  FactSearchCriteria createFactSearchCriteria(ObjectPreparation<FactSearchCriteria.Builder> preparation) {
    FactSearchCriteria.Builder builder = FactSearchCriteria.builder()
            .setAccessControlCriteria(createAccessControlCriteria())
            .setIndexSelectCriteria(createIndexSelectCriteria(DAY1, DAY3));
    if (preparation != null) {
      builder = preparation.prepare(builder);
    }
    return builder.build();
  }

  AccessControlCriteria createAccessControlCriteria() {
    return AccessControlCriteria.builder()
            .addCurrentUserIdentity(UUID.randomUUID())
            .addAvailableOrganizationID(UUID.randomUUID())
            .build();
  }

  IndexSelectCriteria createIndexSelectCriteria(long start, long end) {
    return IndexSelectCriteria.builder()
            .setIndexStartTimestamp(start)
            .setIndexEndTimestamp(end)
            .build();
  }

  FactDocument indexFact(ObjectPreparation<FactDocument> preparation) {
    return indexFact(preparation, FactSearchManager.TargetIndex.Daily);
  }

  FactDocument indexFact(ObjectPreparation<FactDocument> preparation, FactSearchManager.TargetIndex index) {
    FactDocument document = preparation != null ? preparation.prepare(createFactDocument(DAY2)) : createFactDocument(DAY2);
    return getFactSearchManager().indexFact(document, index);
  }

  <T> T first(Iterable<T> iterable) {
    return iterable.iterator().next();
  }

  interface ObjectPreparation<T> {
    T prepare(T e);
  }

}
