package no.mnemonic.act.platform.dao.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.dao.cassandra.mapper.CassandraMapper;
import no.mnemonic.commons.component.ComponentException;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class ClusterManager implements LifecycleAspect {

  private static final long INITIALIZATION_TIMEOUT = TimeUnit.MINUTES.toMillis(2);
  private static final Logger LOGGER = Logging.getLogger(ClusterManager.class);

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

  private void failComponentOnStart(Exception ex) {
    LOGGER.warning(ex, "Could not connect to Cassandra. Shutting down component.");
    throw new ComponentException("Could not connect to Cassandra. Shutting down component.", ex);
  }

  @Override
  public void stopComponent() {
    // Close session to free up any resources.
    if (session != null) session.close();
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
