package no.mnemonic.act.platform.dao.cassandra;

import no.mnemonic.act.platform.entity.cassandra.ObjectByTypeValueEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectFactBindingEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import no.mnemonic.act.platform.entity.handlers.EntityHandler;
import no.mnemonic.act.platform.entity.handlers.EntityHandlerFactory;
import no.mnemonic.commons.testtools.cassandra.CassandraTestResource;
import no.mnemonic.commons.testtools.cassandra.CassandraTruncateRule;
import org.junit.*;

import javax.inject.Provider;

import static no.mnemonic.act.platform.entity.cassandra.CassandraEntity.KEY_SPACE;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractManagerTest {

  private static final ClusterManagerProvider clusterManagerProvider = new ClusterManagerProvider();
  private ObjectManager objectManager;
  private EntityHandler entityHandler;

  @ClassRule
  public static CassandraTestResource cassandra = CassandraTestResource.builder()
          .setClusterName("ACT Cluster")
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
          .build();

  @BeforeClass
  public static void startup() {
    // Just to get the cluster initialized once before all tests.
    clusterManagerProvider.get();
  }

  @AfterClass
  public static void shutdown() {
    clusterManagerProvider.get().stop();
  }

  @Before
  public void initialize() {
    EntityHandlerFactory factory = mock(EntityHandlerFactory.class);
    entityHandler = mock(EntityHandler.class);
    objectManager = new ObjectManager(clusterManagerProvider, factory);

    when(factory.get(any(), any())).thenReturn(entityHandler);
    when(entityHandler.encode(any())).then(returnsFirstArg());
    when(entityHandler.decode(any())).then(returnsFirstArg());
  }

  protected ObjectManager getObjectManager() {
    return objectManager;
  }

  protected EntityHandler getEntityHandler() {
    return entityHandler;
  }

  private static class ClusterManagerProvider implements Provider<ClusterManager> {
    private ClusterManager manager;

    @Override
    public synchronized ClusterManager get() {
      if (manager == null) {
        manager = ClusterManager.builder()
                .setClusterName(cassandra.getClusterName())
                .setPort(cassandra.getPort())
                .addContactPoint("127.0.0.1")
                .build();

        manager.start();
      }

      return manager;
    }
  }

}
