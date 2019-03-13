package no.mnemonic.act.platform.test.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.auth.properties.PropertiesBasedAccessController;
import no.mnemonic.act.platform.dao.cassandra.ClusterManager;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.dao.elastic.ClientFactory;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.rest.RestModule;
import no.mnemonic.act.platform.rest.container.ApiServer;
import no.mnemonic.act.platform.service.ServiceModule;
import no.mnemonic.commons.junit.docker.CassandraDockerResource;
import no.mnemonic.commons.junit.docker.DockerTestUtils;
import no.mnemonic.commons.junit.docker.ElasticSearchDockerResource;
import no.mnemonic.commons.testtools.AvailablePortFinder;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.triggers.pipeline.api.TriggerEventConsumer;
import no.mnemonic.services.triggers.pipeline.worker.InMemoryQueueWorker;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public abstract class AbstractIT {

  private static final String ACL_FILE = ClassLoader.getSystemResource("acl.properties").getPath();
  private static final String RESOURCES_FOLDER = ClassLoader.getSystemResource("").getPath();
  private static final int API_SERVER_PORT = AvailablePortFinder.getAvailablePort(8000);

  private final static ObjectMapper mapper = new ObjectMapper();

  private static PropertiesBasedAccessController accessController;
  private static InMemoryQueueWorker triggerEventConsumer;
  private static ClusterManager clusterManager;
  private static ObjectManager objectManager;
  private static FactManager factManager;
  private static ClientFactory clientFactory;
  private static FactSearchManager factSearchManager;
  private static ApiServer apiServer;

  @ClassRule
  public static CassandraDockerResource cassandra = CassandraDockerResource.builder()
          .setImageName("cassandra")
          .setExposedPortsRange("15000-25000")
          .addApplicationPort(9042)
          .setSetupScript("setup.cql")
          .setTruncateScript("truncate.cql")
          .build();

  @ClassRule
  public static ElasticSearchDockerResource elastic = ElasticSearchDockerResource.builder()
          // Need to specify the exact version here because Elastic doesn't publish images with the 'latest' tag.
          // Usually this should be the same version as the ElasticSearch client used.
          .setImageName("elasticsearch/elasticsearch:6.6.1")
          .setExposedPortsRange("15000-25000")
          .addApplicationPort(9200)
          .addEnvironmentVariable("xpack.security.enabled", "false")
          .addEnvironmentVariable("xpack.monitoring.enabled", "false")
          .addEnvironmentVariable("xpack.ml.enabled", "false")
          .addEnvironmentVariable("xpack.graph.enabled", "false")
          .addEnvironmentVariable("xpack.watcher.enabled", "false")
          .build();

  @Before
  public void setup() {
    Injector injector = Guice.createInjector(new ModuleIT());
    accessController = (PropertiesBasedAccessController) injector.getInstance(AccessController.class);
    triggerEventConsumer = (InMemoryQueueWorker) injector.getInstance(TriggerEventConsumer.class);
    clusterManager = injector.getInstance(ClusterManager.class);
    objectManager = injector.getInstance(ObjectManager.class);
    factManager = injector.getInstance(FactManager.class);
    clientFactory = injector.getInstance(ClientFactory.class);
    factSearchManager = injector.getInstance(FactSearchManager.class);
    apiServer = injector.getInstance(ApiServer.class);

    factSearchManager.setTestEnvironment(true);

    // Start up everything in correct order.
    accessController.startComponent();
    triggerEventConsumer.startComponent();
    clusterManager.startComponent();
    objectManager.startComponent();
    factManager.startComponent();
    clientFactory.startComponent();
    factSearchManager.startComponent();
    apiServer.startComponent();
  }

  @After
  public void teardown() {
    // Stop everything in correct order.
    apiServer.stopComponent();
    factSearchManager.stopComponent();
    clientFactory.stopComponent();
    factManager.stopComponent();
    objectManager.stopComponent();
    clusterManager.stopComponent();
    triggerEventConsumer.stopComponent();
    accessController.stopComponent();
    // Truncate database.
    cassandra.truncate();
    // Truncate indices.
    elastic.deleteIndices();
  }

  /* Getters */

  ObjectManager getObjectManager() {
    return objectManager;
  }

  FactManager getFactManager() {
    return factManager;
  }

  FactSearchManager getFactSearchManager() {
    return factSearchManager;
  }

  /* Helpers for REST */

  Invocation.Builder request(String url) {
    return request(url, 1);
  }

  Invocation.Builder request(String url, long actUserID) {
    return ClientBuilder.newClient()
            .target("http://localhost:" + API_SERVER_PORT + url)
            .request()
            .header("ACT-User-ID", actUserID);
  }

  JsonNode getPayload(Response response) throws IOException {
    // Return the payload in the "data" field of the returned ResultStash.
    return mapper.readTree(response.readEntity(String.class)).get("data");
  }

  UUID getIdFromModel(JsonNode node) {
    return UUID.fromString(node.get("id").textValue());
  }

  /* Helpers for assertions */

  void fetchAndAssertSingle(String url, UUID id) throws Exception {
    Response response = request(url).get();
    assertEquals(200, response.getStatus());
    assertEquals(id, getIdFromModel(getPayload(response)));
  }

  void fetchAndAssertList(String url, UUID id) throws Exception {
    Response response = request(url).get();
    assertEquals(200, response.getStatus());
    ArrayNode data = (ArrayNode) getPayload(response);
    assertEquals(1, data.size());
    assertEquals(id, getIdFromModel(data.get(0)));
  }

  void fetchAndAssertList(String url, ValidatingRequest request, UUID id) throws Exception {
    Response response = request(url).post(Entity.json(request));
    assertEquals(200, response.getStatus());
    ArrayNode data = (ArrayNode) getPayload(response);
    assertEquals(1, data.size());
    assertEquals(id, getIdFromModel(data.get(0)));
  }

  /* Helpers for Cassandra */

  ObjectTypeEntity createObjectType() {
    ObjectTypeEntity entity = new ObjectTypeEntity()
            .setId(UUID.randomUUID())
            .setName("ObjectType")
            .setValidator("TrueValidator");

    return getObjectManager().saveObjectType(entity);
  }

  FactTypeEntity createFactType() {
    return createFactType(createObjectType().getId());
  }

  FactTypeEntity createFactType(UUID objectTypeID) {
    FactTypeEntity.FactObjectBindingDefinition binding = new FactTypeEntity.FactObjectBindingDefinition()
            .setSourceObjectTypeID(objectTypeID)
            .setBidirectionalBinding(true);

    FactTypeEntity entity = new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setName("FactType")
            .setValidator("TrueValidator")
            .setRelevantObjectBindings(SetUtils.set(binding));

    return getFactManager().saveFactType(entity);
  }

  FactTypeEntity createMetaFactType(UUID referencedFactTypeID) {
    FactTypeEntity entity = new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setName("MetaFactType")
            .setValidator("TrueValidator")
            .addRelevantFactBinding(new FactTypeEntity.MetaFactBindingDefinition().setFactTypeID(referencedFactTypeID));

    return getFactManager().saveFactType(entity);
  }

  FactEntity createFact() {
    return createFact(createObject(createObjectType().getId()));
  }

  FactEntity createFact(ObjectEntity object) {
    return createFact(object, createFactType(object.getTypeID()), f -> f);
  }

  FactEntity createFact(ObjectEntity object, FactTypeEntity factType, ObjectPreparation<FactEntity> preparation) {
    FactEntity.FactObjectBinding binding = new FactEntity.FactObjectBinding()
            .setObjectID(object.getId())
            .setDirection(Direction.BiDirectional);

    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(factType.getId())
            .setValue("factValue")
            .setOrganizationID(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .setSourceID(UUID.randomUUID())
            .setAccessMode(AccessMode.RoleBased)
            .setTimestamp(123456789)
            .setLastSeenTimestamp(987654321)
            .setBindings(ListUtils.list(binding));

    getObjectManager().saveObjectFactBinding(new ObjectFactBindingEntity()
            .setObjectID(object.getId())
            .setFactID(fact.getId())
            .setDirection(Direction.BiDirectional));

    fact = getFactManager().saveFact(preparation.prepare(fact));

    getFactSearchManager().indexFact(new FactDocument()
            .setId(fact.getId())
            .setTypeID(factType.getId())
            .setTypeName(factType.getName())
            .setValue(fact.getValue())
            .setInReferenceTo(fact.getInReferenceToID())
            .setOrganizationID(fact.getOrganizationID())
            .setOrganizationName("organizationName")
            .setSourceID(fact.getSourceID())
            .setSourceName("sourceName")
            .setAccessMode(FactDocument.AccessMode.valueOf(fact.getAccessMode().name()))
            .setTimestamp(fact.getTimestamp())
            .setLastSeenTimestamp(fact.getLastSeenTimestamp())
            .addObject(new ObjectDocument()
                    .setId(object.getId())
                    .setTypeID(object.getTypeID())
                    .setValue(object.getValue())
                    .setDirection(ObjectDocument.Direction.valueOf(binding.getDirection().name()))
            )
    );

    return fact;
  }

  FactEntity createMetaFact(FactEntity referencedFact, FactTypeEntity metaFactType, ObjectPreparation<FactEntity> preparation) {
    FactEntity meta = new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(metaFactType.getId())
            .setValue("factValue")
            .setOrganizationID(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .setSourceID(UUID.randomUUID())
            .setAccessMode(AccessMode.RoleBased)
            .setTimestamp(123456789)
            .setLastSeenTimestamp(987654321)
            .setInReferenceToID(referencedFact.getId());

    getFactManager().saveMetaFactBinding(new MetaFactBindingEntity()
            .setFactID(referencedFact.getId())
            .setMetaFactID(meta.getId())
    );

    meta = getFactManager().saveFact(preparation.prepare(meta));

    getFactSearchManager().indexFact(new FactDocument()
            .setId(meta.getId())
            .setTypeID(metaFactType.getId())
            .setTypeName(metaFactType.getName())
            .setValue(meta.getValue())
            .setInReferenceTo(meta.getInReferenceToID())
            .setOrganizationID(meta.getOrganizationID())
            .setOrganizationName("organizationName")
            .setSourceID(meta.getSourceID())
            .setSourceName("sourceName")
            .setAccessMode(FactDocument.AccessMode.valueOf(meta.getAccessMode().name()))
            .setTimestamp(meta.getTimestamp())
            .setLastSeenTimestamp(meta.getLastSeenTimestamp())
    );

    return meta;
  }

  ObjectEntity createObject() {
    return createObject(createObjectType().getId());
  }

  ObjectEntity createObject(UUID objectTypeID) {
    return createObject(objectTypeID, o -> o);
  }

  ObjectEntity createObject(UUID objectTypeID, ObjectPreparation<ObjectEntity> preparation) {
    ObjectEntity object = new ObjectEntity()
            .setId(UUID.randomUUID())
            .setTypeID(objectTypeID)
            .setValue("objectValue");

    return getObjectManager().saveObject(preparation.prepare(object));
  }

  interface ObjectPreparation<T> {
    T prepare(T e);
  }

  private static class ModuleIT extends AbstractModule {
    @Override
    protected void configure() {
      install(new ServiceModule());
      install(new RestModule());
      // Configuration
      bind(String.class).annotatedWith(Names.named("access.controller.properties.file")).toInstance(ACL_FILE);
      bind(String.class).annotatedWith(Names.named("access.controller.read.interval")).toInstance("60000");
      bind(String.class).annotatedWith(Names.named("trigger.administration.service.configuration.directory")).toInstance(RESOURCES_FOLDER);
      bind(String.class).annotatedWith(Names.named("cassandra.cluster.name")).toInstance("ActIntegrationTest");
      bind(String.class).annotatedWith(Names.named("cassandra.contact.points")).toInstance(DockerTestUtils.getDockerHost());
      bind(String.class).annotatedWith(Names.named("cassandra.port")).toInstance(String.valueOf(cassandra.getExposedHostPort(9042)));
      bind(String.class).annotatedWith(Names.named("elasticsearch.contact.points")).toInstance(DockerTestUtils.getDockerHost());
      bind(String.class).annotatedWith(Names.named("elasticsearch.port")).toInstance(String.valueOf(elastic.getExposedHostPort(9200)));
      bind(String.class).annotatedWith(Names.named("api.server.port")).toInstance(String.valueOf(API_SERVER_PORT));
      bind(String.class).annotatedWith(Names.named("cors.allowed.origins")).toInstance("http://www.example.org");
    }
  }

}
