package no.mnemonic.act.platform.dao.cassandra;

import no.mnemonic.act.platform.entity.cassandra.ObjectByTypeValueEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectFactBindingEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import no.mnemonic.commons.testtools.cassandra.CassandraTestResource;
import no.mnemonic.commons.testtools.cassandra.CassandraTruncateRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;

import javax.inject.Provider;

import static no.mnemonic.act.platform.entity.cassandra.CassandraEntity.KEY_SPACE;

public abstract class AbstractManagerTest {

  private static final ClusterManagerProvider clusterManagerProvider = new ClusterManagerProvider();
  private static ObjectManager objectManager;

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
    objectManager = new ObjectManager(clusterManagerProvider);
  }

  @AfterClass
  public static void shutdown() {
    clusterManagerProvider.get().stop();
  }

  protected ObjectManager getObjectManager() {
    return objectManager;
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
