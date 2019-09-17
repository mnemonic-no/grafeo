package no.mnemonic.act.platform.dao.elastic;

import no.mnemonic.commons.junit.docker.DockerResource;
import no.mnemonic.commons.junit.docker.DockerTestUtils;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ClientFactoryTest {

  @ClassRule
  // Don't use ElasticSearchDockerResource in order to test retry logic in startComponent().
  public static DockerResource elastic = DockerResource.builder()
          // Need to specify the exact version here because Elastic doesn't publish images with the 'latest' tag.
          // Usually this should be the same version as the ElasticSearch client used.
          .setImageName("elasticsearch/elasticsearch:6.8.3")
          .setExposedPortsRange("15000-25000")
          .addApplicationPort(9200)
          .addEnvironmentVariable("xpack.security.enabled", "false")
          .addEnvironmentVariable("xpack.monitoring.enabled", "false")
          .addEnvironmentVariable("xpack.ml.enabled", "false")
          .addEnvironmentVariable("xpack.graph.enabled", "false")
          .addEnvironmentVariable("xpack.watcher.enabled", "false")
          .build();

  @Test
  public void testStartup() {
    ClientFactory factory = ClientFactory.builder()
            .setPort(elastic.getExposedHostPort(9200))
            .addContactPoint(DockerTestUtils.getDockerHost())
            .build();
    factory.startComponent();
    assertNotNull(factory.getClient());
    factory.stopComponent();
  }
}
