package no.mnemonic.act.platform.seb.producer.v1.converters;

import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.seb.model.v1.ObjectInfoSEB;
import no.mnemonic.act.platform.seb.producer.v1.resolvers.ObjectTypeInfoResolver;

import javax.inject.Inject;
import java.util.function.Function;

public class ObjectInfoConverter implements Function<ObjectRecord, ObjectInfoSEB> {

  private final ObjectTypeInfoResolver typeResolver;

  @Inject
  public ObjectInfoConverter(ObjectTypeInfoResolver typeResolver) {
    this.typeResolver = typeResolver;
  }

  @Override
  public ObjectInfoSEB apply(ObjectRecord record) {
    if (record == null) return null;

    return ObjectInfoSEB.builder()
            .setId(record.getId())
            .setType(typeResolver.apply(record.getTypeID()))
            .setValue(record.getValue())
            .build();
  }
}
