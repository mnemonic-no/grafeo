package no.mnemonic.act.platform.dao.cassandra;

import no.mnemonic.act.platform.entity.handlers.EntityHandler;
import no.mnemonic.act.platform.entity.handlers.EntityHandlerFactory;
import no.mnemonic.commons.junit.docker.CassandraDockerResource;
import org.junit.*;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractManagerTest {

  private static ClusterManager clusterManager;
  private FactManager factManager;
  private ObjectManager objectManager;
  private EntityHandler entityHandler;

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
    EntityHandlerFactory factory = mock(EntityHandlerFactory.class);
    entityHandler = mock(EntityHandler.class);
    factManager = new FactManager(clusterManager, factory);
    objectManager = new ObjectManager(clusterManager, factory);

    factManager.startComponent();
    objectManager.startComponent();

    when(factory.get(any(), any())).thenReturn(entityHandler);
    when(entityHandler.encode(any())).then(returnsFirstArg());
    when(entityHandler.decode(any())).then(returnsFirstArg());
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

  protected EntityHandler getEntityHandler() {
    return entityHandler;
  }

}
