package no.mnemonic.act.platform.dao.cassandra.accessors;

import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

import java.util.UUID;

import static no.mnemonic.act.platform.entity.cassandra.CassandraEntity.KEY_SPACE;
import static no.mnemonic.act.platform.entity.cassandra.FactEntity.TABLE;

@Accessor
public interface FactAccessor {

  @Query("UPDATE " + KEY_SPACE + "." + TABLE + " SET last_seen_timestamp = :timestamp WHERE id = :id")
  void refreshLastSeenTimestamp(@Param("id") UUID id, @Param("timestamp") long timestamp);

}
