package no.mnemonic.act.platform.test.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import no.mnemonic.act.platform.auth.properties.PropertiesBasedAccessController;
import no.mnemonic.act.platform.dao.cassandra.ClusterManager;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.entity.cassandra.*;
import no.mnemonic.act.platform.rest.RestModule;
import no.mnemonic.act.platform.rest.container.ApiServer;
import no.mnemonic.act.platform.service.ServiceModule;
import no.mnemonic.commons.junit.docker.CassandraDockerResource;
import no.mnemonic.commons.testtools.AvailablePortFinder;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.auth.AccessController;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.UUID;

public abstract class AbstractIT {

  private static final String ACL_FILE = ClassLoader.getSystemResource("acl.properties").getPath();
  private static final int API_SERVER_PORT = AvailablePortFinder.getAvailablePort(8000);

  private final static ObjectMapper mapper = new ObjectMapper();

  private static PropertiesBasedAccessController accessController;
  private static ClusterManager clusterManager;
  private static ObjectManager objectManager;
  private static FactManager factManager;
  private static ApiServer apiServer;

  @ClassRule
  public static CassandraDockerResource cassandra = CassandraDockerResource.builder()
          .setImageName("cassandra")
          .addApplicationPort(9042)
          .setSetupScript("setup.cql")
          .setTruncateScript("truncate.cql")
          .build();

  @Before
  public void setup() {
    Injector injector = Guice.createInjector(new ModuleIT());
    accessController = (PropertiesBasedAccessController) injector.getInstance(AccessController.class);
    clusterManager = injector.getInstance(ClusterManager.class);
    objectManager = injector.getInstance(ObjectManager.class);
    factManager = injector.getInstance(FactManager.class);
    apiServer = injector.getInstance(ApiServer.class);

    // Start up everything in correct order.
    accessController.startComponent();
    clusterManager.startComponent();
    objectManager.startComponent();
    factManager.startComponent();
    apiServer.startComponent();
  }

  @After
  public void teardown() {
    // Stop everything in correct order.
    apiServer.stopComponent();
    factManager.stopComponent();
    objectManager.stopComponent();
    clusterManager.stopComponent();
    accessController.stopComponent();
    // Truncate database.
    cassandra.truncate();
  }

  /* Getters */

  ObjectManager getObjectManager() {
    return objectManager;
  }

  FactManager getFactManager() {
    return factManager;
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

  /* Helpers for Cassandra */

  ObjectTypeEntity createObjectType() {
    ObjectTypeEntity entity = new ObjectTypeEntity()
            .setId(UUID.randomUUID())
            .setName("ObjectType")
            .setEntityHandler("IdentityHandler")
            .setValidator("TrueValidator");

    return getObjectManager().saveObjectType(entity);
  }

  FactTypeEntity createFactType() {
    return createFactType(createObjectType().getId());
  }

  FactTypeEntity createFactType(UUID objectTypeID) {
    FactTypeEntity.FactObjectBindingDefinition binding = new FactTypeEntity.FactObjectBindingDefinition()
            .setObjectTypeID(objectTypeID)
            .setDirection(Direction.BiDirectional);

    FactTypeEntity entity = new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setName("FactType")
            .setEntityHandler("IdentityHandler")
            .setValidator("TrueValidator")
            .setRelevantObjectBindings(ListUtils.list(binding));

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
            .setDirection(factType.getRelevantObjectBindings().get(0).getDirection());

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
            .setDirection(factType.getRelevantObjectBindings().get(0).getDirection()));

    return getFactManager().saveFact(preparation.prepare(fact));
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
      bind(String.class).annotatedWith(Names.named("cassandra.cluster.name")).toInstance("ActIntegrationTest");
      bind(String.class).annotatedWith(Names.named("cassandra.contact.points")).toInstance("localhost");
      bind(String.class).annotatedWith(Names.named("cassandra.port")).toInstance(String.valueOf(cassandra.getExposedHostPort(9042)));
      bind(String.class).annotatedWith(Names.named("api.server.port")).toInstance(String.valueOf(API_SERVER_PORT));
    }
  }

}
