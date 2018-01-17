package no.mnemonic.act.platform.dao.cassandra.accessors;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity.TABLE;

@Accessor
public interface ObjectTypeAccessor {

  @Query("SELECT * FROM " + KEY_SPACE + "." + TABLE)
  Result<ObjectTypeEntity> fetch();

  @Query("SELECT * FROM " + KEY_SPACE + "." + TABLE + " WHERE name = :name")
  ObjectTypeEntity getByName(@Param("name") String name);

}
