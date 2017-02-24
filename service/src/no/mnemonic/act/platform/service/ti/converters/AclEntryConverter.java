package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.AclEntry;
import no.mnemonic.act.platform.api.model.v1.Source;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.entity.cassandra.FactAclEntity;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.UUID;
import java.util.function.Function;

public class AclEntryConverter implements Converter<FactAclEntity, AclEntry> {

  private final Function<UUID, Source> sourceConverter;
  private final Function<UUID, Subject> subjectConverter;

  private AclEntryConverter(Function<UUID, Source> sourceConverter, Function<UUID, Subject> subjectConverter) {
    this.sourceConverter = sourceConverter;
    this.subjectConverter = subjectConverter;
  }

  @Override
  public Class<FactAclEntity> getSourceType() {
    return FactAclEntity.class;
  }

  @Override
  public Class<AclEntry> getTargetType() {
    return AclEntry.class;
  }

  @Override
  public AclEntry apply(FactAclEntity entity) {
    if (entity == null) return null;
    return AclEntry.builder()
            .setId(entity.getId())
            .setSource(ObjectUtils.ifNotNull(sourceConverter.apply(entity.getSourceID()), Source::toInfo))
            .setSubject(ObjectUtils.ifNotNull(subjectConverter.apply(entity.getSubjectID()), Subject::toInfo))
            .setTimestamp(entity.getTimestamp())
            .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Function<UUID, Source> sourceConverter;
    private Function<UUID, Subject> subjectConverter;

    private Builder() {
    }

    public AclEntryConverter build() {
      ObjectUtils.notNull(sourceConverter, "Cannot instantiate AclEntryConverter without 'sourceConverter'.");
      ObjectUtils.notNull(subjectConverter, "Cannot instantiate AclEntryConverter without 'subjectConverter'.");
      return new AclEntryConverter(sourceConverter, subjectConverter);
    }

    public Builder setSourceConverter(Function<UUID, Source> sourceConverter) {
      this.sourceConverter = sourceConverter;
      return this;
    }

    public Builder setSubjectConverter(Function<UUID, Subject> subjectConverter) {
      this.subjectConverter = subjectConverter;
      return this;
    }
  }

}
