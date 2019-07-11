package no.mnemonic.act.platform.dao.cassandra.mapper;

import com.datastax.oss.driver.api.core.PagingIterable;
import com.datastax.oss.driver.api.mapper.annotations.*;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;

import java.util.UUID;

import static com.datastax.oss.driver.api.mapper.entity.saving.NullSavingStrategy.DO_NOT_SET;
import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;

@Dao
@DefaultNullSavingStrategy(DO_NOT_SET)
public interface ObjectTypeDao {

  @Insert
  void save(ObjectTypeEntity entity);

  @Select
  ObjectTypeEntity get(UUID id);

  @Query("SELECT * FROM " + KEY_SPACE + "." + ObjectTypeEntity.TABLE + " WHERE name = :name")
  ObjectTypeEntity get(String name);

  @Query("SELECT * FROM " + KEY_SPACE + "." + ObjectTypeEntity.TABLE)
  PagingIterable<ObjectTypeEntity> fetch();

}
