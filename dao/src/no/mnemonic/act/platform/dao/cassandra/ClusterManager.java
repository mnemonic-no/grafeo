package no.mnemonic.act.platform.dao.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.LoggingRetryPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import no.mnemonic.act.platform.dao.cassandra.entity.AccessMode;
import no.mnemonic.act.platform.dao.cassandra.entity.CassandraEnumCodec;
import no.mnemonic.act.platform.dao.cassandra.entity.Direction;
import no.mnemonic.act.platform.dao.cassandra.entity.SourceEntity;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Set;

public class ClusterManager implements LifecycleAspect {

  private Cluster cluster;
  private MappingManager manager;

  private final String clusterName;
  private final int port;
  private final Set<String> contactPoints;

  private ClusterManager(String clusterName, int port, Set<String> contactPoints) {
    this.clusterName = clusterName;
    this.port = port;
    this.contactPoints = contactPoints;
  }

  @Override
  public void startComponent() {
    if (cluster == null) {
      // Configure and build up the Cassandra cluster.
      cluster = Cluster.builder()
              .withClusterName(clusterName)
              .withPort(port)
              // Wrap default retry policy in LoggingRetryPolicy in order to log retry decisions.
              .withRetryPolicy(new LoggingRetryPolicy(DefaultRetryPolicy.INSTANCE))
              // Wrap DCAwareRoundRobinPolicy which selects local data center nodes in TokenAwarePolicy
              // which in addition prioritizes nodes based on routing information embedded in queries.
              .withLoadBalancingPolicy(new TokenAwarePolicy(DCAwareRoundRobinPolicy.builder().build()))
              .addContactPoints(contactPoints.toArray(new String[0]))
              .build();

      // Register any codecs.
      cluster.getConfiguration().getCodecRegistry()
              .register(new CassandraEnumCodec<>(AccessMode.class, AccessMode.getValueMap()))
              .register(new CassandraEnumCodec<>(Direction.class, Direction.getValueMap()))
              .register(new CassandraEnumCodec<>(SourceEntity.Type.class, SourceEntity.Type.getValueMap()));

      // Create a session.
      manager = new MappingManager(cluster.connect());
    }
  }

  @Override
  public void stopComponent() {
    // Close session and cluster to free up any resources.
    if (manager != null) manager.getSession().close();
    if (cluster != null) cluster.close();
  }

  public <T> Mapper<T> getMapper(Class<T> clazz) {
    return ObjectUtils.ifNotNull(manager, m -> m.mapper(clazz));
  }

  public <T> T getAccessor(Class<T> clazz) {
    return ObjectUtils.ifNotNull(manager, m -> m.createAccessor(clazz));
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String clusterName;
    private int port;
    private Set<String> contactPoints;

    private Builder() {
    }

    public ClusterManager build() {
      return new ClusterManager(clusterName, port, contactPoints);
    }

    public Builder setClusterName(String clusterName) {
      this.clusterName = clusterName;
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
