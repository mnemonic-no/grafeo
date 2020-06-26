package no.mnemonic.act.platform.dao.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.common.io.CharStreams;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.dao.cassandra.mapper.CassandraMapper;
import no.mnemonic.commons.component.ComponentException;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

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

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;

public class ClusterManager implements LifecycleAspect {

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
