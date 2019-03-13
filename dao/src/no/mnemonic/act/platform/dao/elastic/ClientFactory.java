package no.mnemonic.act.platform.dao.elastic;

import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.commons.utilities.lambda.LambdaUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class managing the connections to an ElasticSearch cluster using ElasticSearch's Java high-level REST client.
 */
public class ClientFactory implements LifecycleAspect {

  private static final Logger LOGGER = Logging.getLogger(ClientFactory.class);

  private final int port;
  private final Set<String> contactPoints;

  private RestHighLevelClient client;

  private ClientFactory(int port, Set<String> contactPoints) {
    if (port <= 0) throw new IllegalArgumentException("'port' is required!");
    if (CollectionUtils.isEmpty(contactPoints))
      throw new IllegalArgumentException("At least on contact point is required!");
    this.port = port;
    this.contactPoints = contactPoints;
  }

  @Override
  public void startComponent() {
    if (client == null) {
      // Create a connection for each contact point.
      Set<HttpHost> hosts = contactPoints.stream()
              .map(s -> new HttpHost(s, port))
              .collect(Collectors.toSet());
      // Initialize the high-level REST client which sends the actual requests to ElasticSearch.
      client = new RestHighLevelClient(RestClient.builder(hosts.toArray(new HttpHost[contactPoints.size()])));

      LOGGER.info("Initialized connections to ElasticSearch: %s (port %d)", String.join(",", contactPoints), port);
    }
  }

  @Override
  public void stopComponent() {
    // Release any connection to ElasticSearch.
    if (client != null) {
      LambdaUtils.tryTo(() -> client.close(), ex -> LOGGER.warning(ex, "Error while closing connections to ElasticSearch."));
    }
  }

  public RestHighLevelClient getClient() {
    return client;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private int port;
    private Set<String> contactPoints;

    private Builder() {
    }

    public ClientFactory build() {
      return new ClientFactory(port, contactPoints);
    }

    public Builder setPort(int port) {
      this.port = port;
      return this;
    }

    public Builder setContactPoints(Set<String> contactPoints) {
      this.contactPoints = contactPoints;
      return this;
    }

    public Builder addContactPoint(String contactPoint) {
      this.contactPoints = SetUtils.addToSet(this.contactPoints, contactPoint);
      return this;
    }
  }

}
