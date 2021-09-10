package no.mnemonic.act.platform.dao.cassandra;

import no.mnemonic.commons.junit.docker.CassandraDockerResource;
import org.junit.*;

public abstract class AbstractManagerTest {

  private static ClusterManager clusterManager;
  private FactManager factManager;
  private ObjectManager objectManager;
  private OriginManager originManager;

  @ClassRule
  public static CassandraDockerResource cassandra = CassandraDockerResource.builder()
          .setImageName("cassandra")
          .setExposedPortsRange("15000-25000")
          .addApplicationPort(9042)
          .skipReachabilityCheck()
          .setTruncateScript("truncate.cql")
          .build();

  @BeforeClass
  public static void setup() {
    clusterManager = ClusterManager.builder()
            .setDataCenter("datacenter1")
            .setPort(cassandra.getExposedHostPort(9042))
            .addContactPoint(cassandra.getExposedHost())
            .build();
    clusterManager.startComponent();
  }

  @Before
  public void initialize() {
    factManager = new FactManager(clusterManager);
    objectManager = new ObjectManager(clusterManager);
    originManager = new OriginManager(clusterManager);

    factManager.startComponent();
    objectManager.startComponent();
    originManager.startComponent();
  }

  @After
  public void cleanup() {
    // Truncate database.
    cassandra.truncate();
  }

  @AfterClass
  public static void teardown() {
    clusterManager.stopComponent();
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
