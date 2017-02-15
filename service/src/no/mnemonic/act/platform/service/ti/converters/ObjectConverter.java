package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.entity.cassandra.ObjectEntity;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.UUID;
import java.util.function.Function;

public class ObjectConverter implements Converter<ObjectEntity, Object> {

  private final Function<UUID, ObjectType> objectTypeConverter;

  private ObjectConverter(Function<UUID, ObjectType> objectTypeConverter) {
    this.objectTypeConverter = objectTypeConverter;
  }

  @Override
  public Class<ObjectEntity> getSourceType() {
    return ObjectEntity.class;
  }

  @Override
  public Class<Object> getTargetType() {
    return Object.class;
  }

  @Override
  public Object apply(ObjectEntity entity) {
    if (entity == null) return null;
    // Don't include statistics by default.
    return Object.builder()
            .setId(entity.getId())
            .setType(objectTypeConverter.apply(entity.getTypeID()).toInfo())
            .setValue(entity.getValue())
            .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Function<UUID, ObjectType> objectTypeConverter;

    private Builder() {
    }

    public ObjectConverter build() {
      ObjectUtils.notNull(objectTypeConverter, "Cannot instantiate ObjectConverter without 'objectTypeConverter'.");
      return new ObjectConverter(objectTypeConverter);
    }

    public Builder setObjectTypeConverter(Function<UUID, ObjectType> objectTypeConverter) {
      this.objectTypeConverter = objectTypeConverter;
      return this;
    }
  }

}
