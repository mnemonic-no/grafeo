package no.mnemonic.services.grafeo.seb.producer.v1.converters;

import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.seb.model.v1.ObjectInfoSEB;
import no.mnemonic.services.grafeo.seb.producer.v1.resolvers.ObjectTypeInfoDaoResolver;

import javax.inject.Inject;
import java.util.function.Function;

public class ObjectInfoConverter implements Function<ObjectRecord, ObjectInfoSEB> {

  private final ObjectTypeInfoDaoResolver typeResolver;

  @Inject
  public ObjectInfoConverter(ObjectTypeInfoDaoResolver typeResolver) {
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
