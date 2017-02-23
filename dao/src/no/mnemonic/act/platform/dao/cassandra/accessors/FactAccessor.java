package no.mnemonic.act.platform.dao.cassandra.accessors;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;

import java.util.UUID;

import static no.mnemonic.act.platform.entity.cassandra.CassandraEntity.KEY_SPACE;
import static no.mnemonic.act.platform.entity.cassandra.FactEntity.TABLE;

@Accessor
public interface FactAccessor {

  @Query("SELECT * FROM " + KEY_SPACE + "." + TABLE + " WHERE value = :value")
  Result<FactEntity> fetchByValue(@Param("value") String value);

  @Query("UPDATE " + KEY_SPACE + "." + TABLE + " SET last_seen_timestamp = :timestamp WHERE id = :id")
  void refreshLastSeenTimestamp(@Param("id") UUID id, @Param("timestamp") long timestamp);

}
