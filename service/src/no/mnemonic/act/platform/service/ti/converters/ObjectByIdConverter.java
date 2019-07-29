package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class ObjectByIdConverter implements Converter<UUID, Object> {

  private final ObjectManager objectManager;
  private final Function<ObjectEntity, Object> objectConverter;

  @Inject
  public ObjectByIdConverter(ObjectManager objectManager, Function<ObjectEntity, Object> objectConverter) {
    this.objectManager = objectManager;
    this.objectConverter = objectConverter;
  }

  @Override
  public Class<UUID> getSourceType() {
    return UUID.class;
  }

  @Override
  public Class<Object> getTargetType() {
    return Object.class;
  }

  @Override
  public Object apply(UUID id) {
    if (id == null) return null;
    return ObjectUtils.ifNotNull(objectManager.getObject(id), objectConverter, Object.builder()
            .setId(id)
            .setValue("N/A")
            .build()
    );
  }
}
