package no.mnemonic.act.platform.dao.cassandra.mapper;

import com.datastax.oss.driver.api.core.PagingIterable;
import com.datastax.oss.driver.api.mapper.annotations.*;
import no.mnemonic.act.platform.dao.cassandra.entity.*;

import java.util.List;
import java.util.UUID;

import static com.datastax.oss.driver.api.mapper.entity.saving.NullSavingStrategy.DO_NOT_SET;
import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;

@Dao
@DefaultNullSavingStrategy(DO_NOT_SET)
public interface FactDao {

  /* FactEntity-related methods */

  @Insert
  void save(FactEntity entity);

  @Select
  FactEntity get(UUID id);

  @Query("SELECT * FROM " + KEY_SPACE + "." + FactEntity.TABLE + " WHERE id IN :id")
  PagingIterable<FactEntity> fetchByID(List<UUID> id);

  /* MetaFactBindingEntity-related methods */

  @Insert
  void save(MetaFactBindingEntity entity);

  @Select
  MetaFactBindingEntity getMetaFactBinding(UUID factID, UUID metaFactID);

  @Query("SELECT * FROM " + KEY_SPACE + "." + MetaFactBindingEntity.TABLE + " WHERE fact_id = :id")
  PagingIterable<MetaFactBindingEntity> fetchMetaFactBindings(UUID id);

  /* FactByTimestampEntity-related methods */

  @Insert
  void save(FactByTimestampEntity entity);

  @Select
  FactByTimestampEntity getFactByTimestamp(long hourOfDay, long timestamp, UUID factID);

  @Query("SELECT * FROM " + KEY_SPACE + "." + FactByTimestampEntity.TABLE + " WHERE hour_of_day = :hourOfDay")
  PagingIterable<FactByTimestampEntity> fetchFactByTimestamp(long hourOfDay);

  /* FactAclEntity-related methods */

  @Insert
  void save(FactAclEntity entity);

  @Select
  FactAclEntity getAclEntry(UUID factID, UUID id);

  @Query("SELECT * FROM " + KEY_SPACE + "." + FactAclEntity.TABLE + " WHERE fact_id = :id")
  PagingIterable<FactAclEntity> fetchAcl(UUID id);

  /* FactCommentEntity-related methods */

  @Insert
  void save(FactCommentEntity entity);

  @Select
  FactCommentEntity getComment(UUID factID, UUID id);

  @Query("SELECT * FROM " + KEY_SPACE + "." + FactCommentEntity.TABLE + " WHERE fact_id = :id")
  PagingIterable<FactCommentEntity> fetchComments(UUID id);

}
