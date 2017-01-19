package no.mnemonic.act.platform.dao.cassandra;

import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

public class ClusterManagerProvider implements Provider<ClusterManager> {

  @Inject
  @Named("cassandra.cluster.name")
  private String clusterName;
  @Inject
  @Named("cassandra.port")
  private String port;
  @Inject
  @Named("cassandra.contact.points")
  private String contactPoints;

  @Override
  public ClusterManager get() {
    return ClusterManager.builder()
            .setClusterName(clusterName)
            .setPort(Integer.parseInt(port))
            .setContactPoints(SetUtils.set(contactPoints.split(",")))
            .build();
  }

}
