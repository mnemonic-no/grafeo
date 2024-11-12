package no.mnemonic.services.grafeo.dao.cassandra;

import no.mnemonic.commons.jupiter.docker.CassandraDockerExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

public abstract class AbstractManagerTest {

  private static ClusterManager clusterManager;
  private FactManager factManager;
  private ObjectManager objectManager;
  private OriginManager originManager;

  @RegisterExtension
  public static CassandraDockerExtension cassandra = CassandraDockerExtension.builder()
          .setImageName("cassandra")
          .setSkipPullDockerImage(true)
          .setExposedPortsRange("15000-25000")
          .addApplicationPort(9042)
          .skipReachabilityCheck()
          .setTruncateScript("truncate.cql")
          .build();

  @BeforeAll
  public static void setup() {
    clusterManager = ClusterManager.builder()
            .setDataCenter("datacenter1")
            .setPort(cassandra.getExposedHostPort(9042))
            .addContactPoint(cassandra.getExposedHost())
            .build();
    clusterManager.startComponent();
  }

  @AfterAll
  public static void teardown() {
    clusterManager.stopComponent();
  }

  @BeforeEach
  public void initialize() {
    factManager = new FactManager(clusterManager);
    objectManager = new ObjectManager(clusterManager);
    originManager = new OriginManager(clusterManager);

    factManager.startComponent();
    objectManager.startComponent();
    originManager.startComponent();
  }

  protected FactManager getFactManager() {
    return factManager;
  }

  protected ObjectManager getObjectManager() {
    return objectManager;
  }

  protected OriginManager getOriginManager() {
    return originManager;
  }
}
