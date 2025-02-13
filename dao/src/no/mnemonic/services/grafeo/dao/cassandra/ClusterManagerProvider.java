package no.mnemonic.services.grafeo.dao.cassandra;

import no.mnemonic.commons.utilities.collections.SetUtils;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;

public class ClusterManagerProvider implements Provider<ClusterManager> {

  @Inject
  @Named("grafeo.cassandra.data.center")
  private String dataCenter;
  @Inject
  @Named("grafeo.cassandra.port")
  private String port;
  @Inject
  @Named("grafeo.cassandra.contact.points")
  private String contactPoints;

  @Override
  public ClusterManager get() {
    return ClusterManager.builder()
            .setDataCenter(dataCenter)
            .setPort(Integer.parseInt(port))
            .setContactPoints(SetUtils.set(contactPoints.split(",")))
            .build();
  }

}
