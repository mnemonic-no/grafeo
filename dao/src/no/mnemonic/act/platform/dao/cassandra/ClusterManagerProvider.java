package no.mnemonic.act.platform.dao.cassandra;

import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class ClusterManagerProvider implements Provider<ClusterManager> {

  private ClusterManager manager;

  @Override
  public synchronized ClusterManager get() {
    // Set up cluster manager, but only once!
    if (manager == null) {
      // For now just statically define all settings. At one point this should be read from a config file.
      manager = ClusterManager.builder()
              .setClusterName("ACT Cluster")
              .setPort(9042)
              .addContactPoint("127.0.0.1")
              .build();

      manager.start();
    }

    return manager;
  }

}
