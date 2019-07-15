package no.mnemonic.act.platform.dao.elastic;

import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.commons.junit.docker.DockerTestUtils;
import no.mnemonic.commons.junit.docker.ElasticSearchDockerResource;
import org.junit.*;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.elastic.DocumentTestUtils.createFactDocument;

public abstract class AbstractManagerTest {

  private static ClientFactory clientFactory;
  private FactSearchManager factSearchManager;

  @ClassRule
  public static ElasticSearchDockerResource elastic = ElasticSearchDockerResource.builder()
          // Need to specify the exact version here because Elastic doesn't publish images with the 'latest' tag.
          // Usually this should be the same version as the ElasticSearch client used.
          .setImageName("elasticsearch/elasticsearch:6.8.1")
          .setExposedPortsRange("15000-25000")
          .addApplicationPort(9200)
          .addEnvironmentVariable("xpack.security.enabled", "false")
          .addEnvironmentVariable("xpack.monitoring.enabled", "false")
          .addEnvironmentVariable("xpack.ml.enabled", "false")
          .addEnvironmentVariable("xpack.graph.enabled", "false")
          .addEnvironmentVariable("xpack.watcher.enabled", "false")
          .build();

  @BeforeClass
  public static void setup() {
    clientFactory = ClientFactory.builder()
            .setPort(elastic.getExposedHostPort(9200))
            .addContactPoint(DockerTestUtils.getDockerHost())
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
            .setCurrentUserID(UUID.randomUUID())
            .addAvailableOrganizationID(UUID.randomUUID());
    if (preparation != null) {
      builder = preparation.prepare(builder);
    }
    return builder.build();
  }

  FactDocument indexFact(ObjectPreparation<FactDocument> preparation) {
    FactDocument document = preparation != null ? preparation.prepare(createFactDocument()) : createFactDocument();
    return getFactSearchManager().indexFact(document);
  }

  <T> T first(Iterable<T> iterable) {
    return iterable.iterator().next();
  }

  interface ObjectPreparation<T> {
    T prepare(T e);
  }

}
