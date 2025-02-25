package no.mnemonic.services.grafeo.dao.cassandra;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@Testcontainers
public abstract class AbstractManagerTest {

  private static ClusterManager clusterManager;
  private FactManager factManager;
  private ObjectManager objectManager;
  private OriginManager originManager;

  @Container
  public static final CassandraContainer cassandra = new CassandraContainer("cassandra")
          .withCopyFileToContainer(MountableFile.forClasspathResource("truncate.cql"), "/tmp/");

  @BeforeAll
  public static void setup() {
    clusterManager = ClusterManager.builder()
            .setDataCenter(cassandra.getLocalDatacenter())
            .setPort(cassandra.getMappedPort(9042))
            .addContactPoint(cassandra.getHost())
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

  @AfterEach
  public void cleanup() throws Exception {
    // Clean up database after each run.
    cassandra.execInContainer("cqlsh", "-f", "/tmp/truncate.cql");
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
