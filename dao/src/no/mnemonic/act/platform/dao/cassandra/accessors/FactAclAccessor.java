package no.mnemonic.act.platform.dao.cassandra.accessors;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import no.mnemonic.act.platform.entity.cassandra.FactAclEntity;

import java.util.UUID;

import static no.mnemonic.act.platform.entity.cassandra.CassandraEntity.KEY_SPACE;
import static no.mnemonic.act.platform.entity.cassandra.FactAclEntity.TABLE;

@Accessor
public interface FactAclAccessor {

  @Query("SELECT * FROM " + KEY_SPACE + "." + TABLE + " WHERE fact_id = :id")
  Result<FactAclEntity> fetch(@Param("id") UUID id);

}
