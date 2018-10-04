package no.mnemonic.act.platform.dao.cassandra;

import no.mnemonic.commons.junit.docker.CassandraDockerResource;
import org.junit.*;

public abstract class AbstractManagerTest {

  private static ClusterManager clusterManager;
  private FactManager factManager;
  private ObjectManager objectManager;

  @ClassRule
  public static CassandraDockerResource cassandra = CassandraDockerResource.builder()
          .setImageName("cassandra")
          .addApplicationPort(9042)
          .setSetupScript("setup.cql")
          .setTruncateScript("truncate.cql")
          .build();

  @BeforeClass
  public static void setup() {
    clusterManager = ClusterManager.builder()
            .setClusterName("ACT Cluster")
            .setPort(cassandra.getExposedHostPort(9042))
            .addContactPoint("127.0.0.1")
            .build();
    clusterManager.startComponent();
  }

  @Before
  public void initialize() {
    factManager = new FactManager(clusterManager);
    objectManager = new ObjectManager(clusterManager);

    factManager.startComponent();
    objectManager.startComponent();
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

}
