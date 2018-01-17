package no.mnemonic.act.platform.dao.cassandra.accessors;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectByTypeValueEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectFactBindingEntity;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;

@Accessor
public interface ObjectAccessor {

  @Query("SELECT * FROM " + KEY_SPACE + "." + ObjectEntity.TABLE)
  Result<ObjectEntity> fetch();

  @Query("SELECT * FROM " + KEY_SPACE + "." + ObjectByTypeValueEntity.TABLE + " WHERE object_type_id = :type AND object_value = :value")
  ObjectByTypeValueEntity getObjectByTypeValue(@Param("type") UUID objectTypeID, @Param("value") String objectValue);

  @Query("SELECT * FROM " + KEY_SPACE + "." + ObjectFactBindingEntity.TABLE + " WHERE object_id = :id")
  Result<ObjectFactBindingEntity> fetchObjectFactBindings(@Param("id") UUID id);

}
