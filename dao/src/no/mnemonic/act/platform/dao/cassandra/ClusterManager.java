package no.mnemonic.act.platform.dao.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import no.mnemonic.act.platform.dao.cassandra.entity.AccessMode;
import no.mnemonic.act.platform.dao.cassandra.entity.CassandraEnumCodec;
import no.mnemonic.act.platform.dao.cassandra.entity.Direction;
import no.mnemonic.act.platform.dao.cassandra.entity.SourceEntity;
import no.mnemonic.act.platform.dao.cassandra.mapper.CassandraMapper;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.stream.Collectors;

public class ClusterManager implements LifecycleAspect {

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
      session = CqlSession.builder()
              .withLocalDatacenter(dataCenter)
              .addContactPoints(contactPoints.stream()
                      .map(cp -> new InetSocketAddress(cp, port))
                      .collect(Collectors.toSet()))
              // Register any custom type codecs.
              .addTypeCodecs(new CassandraEnumCodec<>(AccessMode.class, AccessMode.getValueMap()))
              .addTypeCodecs(new CassandraEnumCodec<>(Direction.class, Direction.getValueMap()))
              .addTypeCodecs(new CassandraEnumCodec<>(SourceEntity.Type.class, SourceEntity.Type.getValueMap()))
              .build();

      cassandraMapper = CassandraMapper.builder(session).build();
    }
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
