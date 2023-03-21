package no.mnemonic.services.grafeo.dao.cassandra.mapper;

import com.datastax.oss.driver.api.core.PagingIterable;
import com.datastax.oss.driver.api.mapper.annotations.*;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;

import java.util.UUID;

import static com.datastax.oss.driver.api.mapper.entity.saving.NullSavingStrategy.DO_NOT_SET;
import static no.mnemonic.services.grafeo.dao.cassandra.entity.CassandraEntity.KEY_SPACE;

@Dao
@DefaultNullSavingStrategy(DO_NOT_SET)
public interface FactTypeDao {

  @Insert
  void save(FactTypeEntity entity);

  @Select
  FactTypeEntity get(UUID id);

  @Query("SELECT * FROM " + KEY_SPACE + "." + FactTypeEntity.TABLE + " WHERE name = :name")
  FactTypeEntity get(String name);

  @Query("SELECT * FROM " + KEY_SPACE + "." + FactTypeEntity.TABLE)
  PagingIterable<FactTypeEntity> fetch();

}
