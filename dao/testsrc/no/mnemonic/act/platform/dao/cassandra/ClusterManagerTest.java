package no.mnemonic.act.platform.dao.cassandra;

import no.mnemonic.commons.junit.docker.DockerResource;
import no.mnemonic.commons.junit.docker.DockerTestUtils;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ClusterManagerTest {

  @ClassRule
  // Don't use CassandraDockerResource in order to test retry configuration logic.
  public static DockerResource cassandra = DockerResource.builder()
          .setImageName("cassandra")
          .setExposedPortsRange("15000-25000")
          .addApplicationPort(9042)
          .build();

  @Test
  public void testStartup() throws Exception {
    ClusterManager manager = ClusterManager.builder()
            .setDataCenter("datacenter1")
            .setPort(cassandra.getExposedHostPort(9042))
            .addContactPoint(DockerTestUtils.getDockerHost())
            .build();
    manager.startComponent();
    assertNotNull(manager.getCassandraMapper());
    assertNotNull(manager.getMetrics());
    manager.stopComponent();
  }
}
