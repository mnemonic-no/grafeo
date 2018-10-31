package no.mnemonic.act.platform.dao.cassandra.accessors;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.MetaFactBindingEntity;

import java.util.List;
import java.util.UUID;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;

@Accessor
public interface FactAccessor {

  @Query("SELECT * FROM " + KEY_SPACE + "." + FactEntity.TABLE + " WHERE id IN :id")
  Result<FactEntity> fetchByID(@Param("id") List<UUID> id);

  @Query("UPDATE " + KEY_SPACE + "." + FactEntity.TABLE + " SET last_seen_timestamp = :timestamp WHERE id = :id")
  void refreshLastSeenTimestamp(@Param("id") UUID id, @Param("timestamp") long timestamp);

  @Query("SELECT * FROM " + KEY_SPACE + "." + MetaFactBindingEntity.TABLE + " WHERE fact_id = :id")
  Result<MetaFactBindingEntity> fetchMetaFactBindings(@Param("id") UUID id);

}
