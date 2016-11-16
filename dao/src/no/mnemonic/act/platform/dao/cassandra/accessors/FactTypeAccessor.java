package no.mnemonic.act.platform.dao.cassandra.accessors;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import no.mnemonic.act.platform.entity.cassandra.FactTypeEntity;

import static no.mnemonic.act.platform.entity.cassandra.CassandraEntity.KEY_SPACE;
import static no.mnemonic.act.platform.entity.cassandra.FactTypeEntity.TABLE;

@Accessor
public interface FactTypeAccessor {

  @Query("SELECT * FROM " + KEY_SPACE + "." + TABLE)
  Result<FactTypeEntity> fetch();

  @Query("SELECT * FROM " + KEY_SPACE + "." + TABLE + " WHERE name = :name")
  FactTypeEntity getByName(@Param("name") String name);

}
