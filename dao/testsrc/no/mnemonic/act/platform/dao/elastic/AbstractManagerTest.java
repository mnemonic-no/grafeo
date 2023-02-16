package no.mnemonic.act.platform.dao.elastic;

import no.mnemonic.act.platform.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.commons.junit.docker.ElasticSearchDockerResource;
import org.junit.*;

import java.time.Instant;
import java.util.UUID;

import static no.mnemonic.act.platform.dao.elastic.DocumentTestUtils.createFactDocument;

public abstract class AbstractManagerTest {

  static final long DAY1 = Instant.parse("2022-01-01T12:00:00.000Z").toEpochMilli();
  static final long DAY2 = Instant.parse("2022-01-02T12:00:00.000Z").toEpochMilli();
  static final long DAY3 = Instant.parse("2022-01-03T12:00:00.000Z").toEpochMilli();

  private static ClientFactory clientFactory;
  private FactSearchManager factSearchManager;

  @ClassRule
  public static ElasticSearchDockerResource elastic = ElasticSearchDockerResource.builder()
          // Need to specify the exact version here because Elastic doesn't publish images with the 'latest' tag.
          // Usually this should be the same version as the ElasticSearch client used.
          .setImageName("elasticsearch/elasticsearch:7.17.9")
          .setExposedPortsRange("15000-25000")
          .addApplicationPort(9200)
          .skipReachabilityCheck()
          .addEnvironmentVariable("discovery.type", "single-node")
          .build();

  @BeforeClass
  public static void setup() {
    clientFactory = ClientFactory.builder()
            .setPort(elastic.getExposedHostPort(9200))
            .addContactPoint(elastic.getExposedHost())
            .build();
    clientFactory.startComponent();
  }

  @Before
  public void initialize() {
    factSearchManager = new FactSearchManager(clientFactory)
            .setTestEnvironment(true)
            .setSearchScrollExpiration("5s")
            .setSearchScrollSize(1);
    factSearchManager.startComponent();
  }

  @After
  public void cleanup() {
    elastic.deleteIndices();
  }

  @AfterClass
  public static void teardown() {
    clientFactory.stopComponent();
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
