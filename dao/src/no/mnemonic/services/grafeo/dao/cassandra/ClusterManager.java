package no.mnemonic.services.grafeo.dao.cassandra;

import com.codahale.metrics.*;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metrics.DefaultNodeMetric;
import com.datastax.oss.driver.api.core.metrics.DefaultSessionMetric;
import com.datastax.oss.driver.api.core.metrics.NodeMetric;
import com.datastax.oss.driver.api.core.metrics.SessionMetric;
import com.google.common.io.CharStreams;
import no.mnemonic.commons.component.ComponentException;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.metrics.*;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.dao.cassandra.entity.*;
import no.mnemonic.services.grafeo.dao.cassandra.mapper.CassandraMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static no.mnemonic.services.grafeo.dao.cassandra.entity.CassandraEntity.KEY_SPACE;

public class ClusterManager implements LifecycleAspect, MetricAspect {

  private static final long INITIALIZATION_TIMEOUT = TimeUnit.MINUTES.toMillis(2);
  private static final Logger LOGGER = Logging.getLogger(ClusterManager.class);
  private static final String KEYSPACE_CQL = "keyspace.cql";

  private CqlSession session;
  private CassandraMapper cassandraMapper;

  private final String dataCenter;
  private final int port;
  private final Set<String> contactPoints;

  private ClusterManager(String dataCenter, int port, Set<String> contactPoints) {
    this.dataCenter = dataCenter;
    this.port = port;
    this.contactPoints = contactPoints;
  }

  @Override
  public void startComponent() {
    initializeSession();

    if (!keyspaceExists()) {
      LOGGER.info("Keyspace '%s' does not exist, create it and initialize schema.", KEY_SPACE);
      initializeKeyspace();
    }
  }

  @Override
  public void stopComponent() {
    // Close session to free up any resources.
    if (session != null) session.close();
  }

  @Override
  public Metrics getMetrics() throws MetricException {
    if (session == null || !session.getMetrics().isPresent()) return new MetricsData();

    com.datastax.oss.driver.api.core.metrics.Metrics driverMetrics = session.getMetrics().get();

    MetricsGroup clientMetrics = new MetricsGroup();
    // First collect metrics from the session.
    clientMetrics.addSubMetrics("session", collectSessionMetrics(driverMetrics));
    // Then collect individual node metrics.
    for (Node node : session.getMetadata().getNodes().values()) {
      // Only collect metrics about nodes from the local data center as the driver won't query nodes from other data centers.
      if (!Objects.equals(node.getDatacenter(), dataCenter)) continue;
      clientMetrics.addSubMetrics("node-" + node.getHostId(), collectNodeMetrics(driverMetrics, node));
    }

    return clientMetrics;
  }

  private void initializeSession() {
    if (session == null) {
      // Configure and create a session connecting to Cassandra.
      CompletableFuture<CqlSession> future = CqlSession.builder()
              .withLocalDatacenter(dataCenter)
              .addContactPoints(contactPoints.stream()
                      .map(cp -> new InetSocketAddress(cp, port))
                      .collect(Collectors.toSet()))
              // Register any custom type codecs.
              .addTypeCodecs(new CassandraEnumCodec<>(AccessMode.class, AccessMode.getValueMap()))
              .addTypeCodecs(new CassandraEnumCodec<>(Direction.class, Direction.getValueMap()))
              .addTypeCodecs(new CassandraEnumCodec<>(FactEntity.Flag.class, FactEntity.Flag.getValueMap()))
              .addTypeCodecs(new CassandraEnumCodec<>(FactTypeEntity.Flag.class, FactTypeEntity.Flag.getValueMap()))
              .addTypeCodecs(new CassandraEnumCodec<>(ObjectTypeEntity.Flag.class, ObjectTypeEntity.Flag.getValueMap()))
              .addTypeCodecs(new CassandraEnumCodec<>(OriginEntity.Type.class, OriginEntity.Type.getValueMap()))
              .addTypeCodecs(new CassandraEnumCodec<>(OriginEntity.Flag.class, OriginEntity.Flag.getValueMap()))
              .buildAsync()
              .toCompletableFuture();

      try {
        // Need to use asynchronous builder to be able to abort connection attempt. With synchronous builder and
        // 'advanced.reconnect-on-init = true' the driver would never stop trying to connect to Cassandra which
        // would block startComponent() forever.
        session = future.get(INITIALIZATION_TIMEOUT, TimeUnit.MILLISECONDS);
        cassandraMapper = CassandraMapper.builder(session).build();
      } catch (ExecutionException | TimeoutException ex) {
        // Abort connection attempt and shut down the component.
        future.cancel(true);
        failComponentOnStart(ex);
      } catch (InterruptedException ex) {
        // Re-interrupt thread and shut down the component.
        Thread.currentThread().interrupt();
        failComponentOnStart(ex);
      }

      LOGGER.info("Initialized connections to Cassandra: %s (port %d)", String.join(",", contactPoints), port);
    }
  }

  private void initializeKeyspace() {
    try (InputStream stream = ClusterManager.class.getClassLoader().getResourceAsStream(KEYSPACE_CQL);
         InputStreamReader reader = new InputStreamReader(stream)) {
      // Read the whole .cql file and split on semi-colon which separates CQL queries. Execute each query separately.
      for (String query : CharStreams.toString(reader).split(";")) {
        if (StringUtils.isBlank(query)) continue;
        session.execute(query.trim());
      }
    } catch (DriverException | IOException ex) {
      LOGGER.error(ex, "Error when initializing keyspace and schema.");
      throw new ComponentException("Error when initializing keyspace and schema.", ex);
    }

    LOGGER.info("Successfully initialized keyspace '%s' and schema.", KEY_SPACE);
  }

  private boolean keyspaceExists() {
    // Fetch all existing keyspaces from the system table and check if 'act' exists.
    for (Row row : session.execute("SELECT * FROM system_schema.keyspaces")) {
      if (Objects.equals(KEY_SPACE, row.getString("keyspace_name"))) return true;
    }

    return false;
  }

  private void failComponentOnStart(Exception ex) {
    LOGGER.warning(ex, "Could not connect to Cassandra. Shutting down component.");
    throw new ComponentException("Could not connect to Cassandra. Shutting down component.", ex);
  }

  private MetricsData collectSessionMetrics(com.datastax.oss.driver.api.core.metrics.Metrics driverMetrics) throws MetricException {
    Gauge<?> connectedNodes = getSessionMetric(driverMetrics, DefaultSessionMetric.CONNECTED_NODES, Gauge.class);
    Meter bytesSent = getSessionMetric(driverMetrics, DefaultSessionMetric.BYTES_SENT, Meter.class);
    Meter bytesReceived = getSessionMetric(driverMetrics, DefaultSessionMetric.BYTES_RECEIVED, Meter.class);
    Timer cqlRequests = getSessionMetric(driverMetrics, DefaultSessionMetric.CQL_REQUESTS, Timer.class);
    Counter cqlClientTimeouts = getSessionMetric(driverMetrics, DefaultSessionMetric.CQL_CLIENT_TIMEOUTS, Counter.class);

    return new MetricsData()
            .addData("connectedNodes", (Integer) connectedNodes.getValue())
            .addData("bytesSent", bytesSent.getCount())
            .addData("bytesReceived", bytesReceived.getCount())
            .addData("cqlRequests.count", cqlRequests.getCount())
            .addData("cqlRequests.latency.max", cqlRequests.getSnapshot().getMax())
            .addData("cqlRequests.latency.mean", cqlRequests.getSnapshot().getMean())
            .addData("cqlRequests.latency.median", cqlRequests.getSnapshot().getMedian())
            .addData("cqlRequests.latency.99percentile", cqlRequests.getSnapshot().get99thPercentile())
            .addData("cqlClientTimeouts", cqlClientTimeouts.getCount());
  }

  private MetricsData collectNodeMetrics(com.datastax.oss.driver.api.core.metrics.Metrics driverMetrics, Node node) throws MetricException {
    Gauge<?> openConnections = getNodeMetric(driverMetrics, node, DefaultNodeMetric.OPEN_CONNECTIONS, Gauge.class);
    Gauge<?> availableStreams = getNodeMetric(driverMetrics, node, DefaultNodeMetric.AVAILABLE_STREAMS, Gauge.class);
    Gauge<?> orphanedStreams = getNodeMetric(driverMetrics, node, DefaultNodeMetric.ORPHANED_STREAMS, Gauge.class);
    Gauge<?> inFlight = getNodeMetric(driverMetrics, node, DefaultNodeMetric.IN_FLIGHT, Gauge.class);

    return new MetricsData()
            .addData("openConnections", (Integer) openConnections.getValue())
            .addData("availableStreams", (Integer) availableStreams.getValue())
            .addData("orphanedStreams", (Integer) orphanedStreams.getValue())
            .addData("inFlight", (Integer) inFlight.getValue());
  }

  private <M extends Metric> M getSessionMetric(com.datastax.oss.driver.api.core.metrics.Metrics driverMetrics,
                                                SessionMetric sessionMetric,
                                                Class<M> type) throws MetricException {
    Metric metric = driverMetrics.getSessionMetric(sessionMetric).orElse(null);
    if (!type.isInstance(metric)) {
      throw new MetricException(String.format("Failed to collect metric '%s'. Missing configuration in application.conf?", sessionMetric.getPath()));
    }

    return type.cast(metric);
  }

  private <M extends Metric> M getNodeMetric(com.datastax.oss.driver.api.core.metrics.Metrics driverMetrics,
                                             Node node,
                                             NodeMetric nodeMetric,
                                             Class<M> type) throws MetricException {
    Metric metric = driverMetrics.getNodeMetric(node, nodeMetric).orElse(null);
    if (!type.isInstance(metric)) {
      throw new MetricException(String.format("Failed to collect metric '%s'. Missing configuration in application.conf?", nodeMetric.getPath()));
    }

    return type.cast(metric);
  }

  public CassandraMapper getCassandraMapper() {
    return cassandraMapper;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String dataCenter;
    private int port;
    private Set<String> contactPoints;

    private Builder() {
    }

    public ClusterManager build() {
      return new ClusterManager(dataCenter, port, contactPoints);
    }

    public Builder setDataCenter(String dataCenter) {
      this.dataCenter = dataCenter;
      return this;
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
