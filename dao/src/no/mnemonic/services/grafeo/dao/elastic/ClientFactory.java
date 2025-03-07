package no.mnemonic.services.grafeo.dao.elastic;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import no.mnemonic.commons.component.ComponentException;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.commons.utilities.lambda.LambdaUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Class managing the connections to an ElasticSearch cluster using ElasticSearch's Java API Client.
 */
public class ClientFactory implements LifecycleAspect {

  private static final long INITIALIZATION_TIMEOUT = TimeUnit.MINUTES.toMillis(2);
  private static final long INITIALIZATION_RETRY_WAIT = TimeUnit.SECONDS.toMillis(2);
  private static final long SOCKET_TIMEOUT = TimeUnit.MINUTES.toMillis(1);
  private static final Logger LOGGER = Logging.getLogger(ClientFactory.class);

  private final int port;
  private final Set<String> contactPoints;

  private RestClient restClient;
  private ElasticsearchClient client;
  private ElasticsearchAsyncClient asyncClient;

  private ClientFactory(int port, Set<String> contactPoints) {
    if (port <= 0) throw new IllegalArgumentException("'port' is required!");
    if (CollectionUtils.isEmpty(contactPoints))
      throw new IllegalArgumentException("At least one contact point is required!");
    this.port = port;
    this.contactPoints = contactPoints;
  }

  @Override
  public void startComponent() {
    if (restClient == null) {
      // Create a connection for each contact point.
      HttpHost[] hosts = contactPoints.stream()
              .map(s -> new HttpHost(s, port))
              .toArray(HttpHost[]::new);
      // The RestClient handles the low-level HTTP connections.
      restClient = RestClient.builder(hosts)
              .setRequestConfigCallback(builder -> builder.setSocketTimeout((int) SOCKET_TIMEOUT))
              .build();
      // The Elasticsearch(Async)Client sends the actual requests to ElasticSearch.
      // Both clients share the underlying transport and RestClient.
      RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
      client = new ElasticsearchClient(transport);
      asyncClient = new ElasticsearchAsyncClient(transport);

      // Wait until ElasticSearch becomes available.
      if (!waitForConnection(client)) {
        LOGGER.warning("Could not connect to ElasticSearch. Shutting down component.");
        throw new ComponentException("Could not connect to ElasticSearch. Shutting down component.");
      }

      LOGGER.info("Initialized connections to ElasticSearch: %s (port %d)", String.join(",", contactPoints), port);
    }
  }

  @Override
  public void stopComponent() {
    // Release any connection to ElasticSearch.
    if (client != null) {
      LambdaUtils.tryTo(() -> client.close(), ex -> LOGGER.warning(ex, "Error while closing connections to ElasticSearch."));
    }
    if (asyncClient != null) {
      LambdaUtils.tryTo(() -> asyncClient.close(), ex -> LOGGER.warning(ex, "Error while closing connections to ElasticSearch."));
    }
  }

  private boolean waitForConnection(ElasticsearchClient client) {
    long timeout = System.currentTimeMillis() + INITIALIZATION_TIMEOUT;
    while (System.currentTimeMillis() < timeout) {
      try {
        HealthResponse response = client.cluster().health();
        LOGGER.debug("ElasticSearch cluster (%s) status is %s.", response.clusterName(), response.status());
        // If ElasticSearch is reachable and its status is at least 'yellow' return immediately.
        if (!response.timedOut() && response.status() != HealthStatus.Red) return true;
      } catch (ElasticsearchException | IOException ex) {
        LOGGER.debug(ex, "Could not fetch ElasticSearch cluster health information.");
      }

      try {
        Thread.sleep(INITIALIZATION_RETRY_WAIT);
      } catch (InterruptedException ignored) {
        // Re-interrupt thread and return immediately in order to trigger a component shutdown.
        Thread.currentThread().interrupt();
        return false;
      }

      LOGGER.warning("ElasticSearch cluster is not available. Trying again.");
    }

    return false;
  }

  /**
   * Return the low-level REST client. Useful if access to the underlying plain HTTP connection is required.
   */
  public RestClient getRestClient() {
    return restClient;
  }

  /**
   * Return the high-level ElasticSearch client which models the API of ElasticSearch.
   */
  public ElasticsearchClient getClient() {
    return client;
  }

  /**
   * Return the high-level ElasticSearch client which models the API of ElasticSearch (asynchronous variant).
   */
  public ElasticsearchAsyncClient getAsyncClient() {
    return asyncClient;
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
