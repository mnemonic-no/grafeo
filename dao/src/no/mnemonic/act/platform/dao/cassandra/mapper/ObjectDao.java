package no.mnemonic.act.platform.dao.cassandra.mapper;

import com.datastax.oss.driver.api.core.PagingIterable;
import com.datastax.oss.driver.api.mapper.annotations.*;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectByTypeValueEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectFactBindingEntity;

import java.util.List;
import java.util.UUID;

import static com.datastax.oss.driver.api.mapper.entity.saving.NullSavingStrategy.DO_NOT_SET;
import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;

@Dao
@DefaultNullSavingStrategy(DO_NOT_SET)
public interface ObjectDao {

  /* ObjectEntity-related methods */

  @Insert
  void save(ObjectEntity entity);

  @Select
  ObjectEntity get(UUID id);

  @Query("SELECT * FROM " + KEY_SPACE + "." + ObjectEntity.TABLE + " WHERE id IN :id")
  PagingIterable<ObjectEntity> fetchByID(List<UUID> id);

  /* ObjectByTypeValueEntity-related methods */

  @Insert
  void save(ObjectByTypeValueEntity entity);

  @Select
  ObjectByTypeValueEntity getObjectByTypeValue(UUID objectTypeID, String objectValue);

  /* ObjectFactBindingEntity-related methods */

  @Insert
  void save(ObjectFactBindingEntity entity);

  @Select
  ObjectFactBindingEntity getObjectFactBinding(UUID objectID, UUID factID);

  @Query("SELECT * FROM " + KEY_SPACE + "." + ObjectFactBindingEntity.TABLE + " WHERE object_id = :id")
  PagingIterable<ObjectFactBindingEntity> fetchObjectFactBindings(UUID id);

}
