package no.mnemonic.act.platform.test.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import no.mnemonic.act.platform.dao.cassandra.ClusterManager;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.entity.cassandra.*;
import no.mnemonic.act.platform.rest.RestModule;
import no.mnemonic.act.platform.rest.container.ApiServer;
import no.mnemonic.act.platform.service.ServiceModule;
import no.mnemonic.commons.testtools.AvailablePortFinder;
import no.mnemonic.commons.testtools.cassandra.CassandraTestResource;
import no.mnemonic.commons.testtools.cassandra.CassandraTruncateRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static no.mnemonic.act.platform.entity.cassandra.CassandraEntity.KEY_SPACE;

public abstract class AbstractIT {

  private static final String CASSANDRA_CLUSTER_NAME = "ActIntegrationTest";
  private static final String CASSANDRA_CONTACT_POINTS = "localhost";
  private static final int API_SERVER_PORT = AvailablePortFinder.getAvailablePort(8000);

  private final static ObjectMapper mapper = new ObjectMapper();

  private static ClusterManager clusterManager;
  private static ObjectManager objectManager;
  private static FactManager factManager;
  private static ApiServer apiServer;

  @ClassRule
  public static CassandraTestResource cassandra = CassandraTestResource.builder()
          .setClusterName(CASSANDRA_CLUSTER_NAME)
          .setKeyspaceName(KEY_SPACE)
          .setStartupScript("resources/setup.cql")
          .build();

  @Rule
  public CassandraTruncateRule truncateRule = CassandraTruncateRule.builder()
          .setKeyspace(KEY_SPACE)
          .setSession(cassandra.getServer().getNativeSession())
          .addTable(ObjectEntity.TABLE)
          .addTable(ObjectTypeEntity.TABLE)
          .addTable(ObjectByTypeValueEntity.TABLE)
          .addTable(ObjectFactBindingEntity.TABLE)
          .addTable(FactEntity.TABLE)
          .addTable(FactTypeEntity.TABLE)
          .addTable(FactAclEntity.TABLE)
          .addTable(FactCommentEntity.TABLE)
          .build();

  @BeforeClass
  public static void setup() {
    Injector injector = Guice.createInjector(new ModuleIT());
    clusterManager = injector.getInstance(ClusterManager.class);
    objectManager = injector.getInstance(ObjectManager.class);
    factManager = injector.getInstance(FactManager.class);
    apiServer = injector.getInstance(ApiServer.class);

    // Start up everything in correct order.
    clusterManager.startComponent();
    objectManager.startComponent();
    factManager.startComponent();
    apiServer.startComponent();
  }

  @AfterClass
  public static void teardown() {
    // Stop everything in correct order.
    apiServer.stopComponent();
    factManager.stopComponent();
    objectManager.stopComponent();
    clusterManager.stopComponent();
  }

  ObjectManager getObjectManager() {
    return objectManager;
  }

  FactManager getFactManager() {
    return factManager;
  }

  WebTarget target(String url) {
    return ClientBuilder.newClient().target("http://localhost:" + API_SERVER_PORT + url);
  }

  JsonNode getPayload(Response response) throws IOException {
    // Return the payload in the "data" field of the returned ResultStash.
    return mapper.readTree(response.readEntity(String.class)).get("data");
  }

  private static class ModuleIT extends AbstractModule {
    @Override
    protected void configure() {
      install(new ServiceModule());
      install(new RestModule());
      // Configuration
      bind(String.class).annotatedWith(Names.named("cassandra.cluster.name")).toInstance(CASSANDRA_CLUSTER_NAME);
      bind(String.class).annotatedWith(Names.named("cassandra.contact.points")).toInstance(CASSANDRA_CONTACT_POINTS);
      bind(String.class).annotatedWith(Names.named("cassandra.port")).toInstance(String.valueOf(cassandra.getPort()));
      bind(String.class).annotatedWith(Names.named("api.server.port")).toInstance(String.valueOf(API_SERVER_PORT));
    }
  }

}
