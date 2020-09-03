package no.mnemonic.act.platform.seb.producer.v1.converters;

import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.seb.model.v1.AclEntrySEB;
import no.mnemonic.act.platform.seb.producer.v1.resolvers.OriginInfoResolver;
import no.mnemonic.act.platform.seb.producer.v1.resolvers.SubjectInfoResolver;

import javax.inject.Inject;
import java.util.function.Function;

public class AclEntryConverter implements Function<FactAclEntryRecord, AclEntrySEB> {

  private final SubjectInfoResolver subjectResolver;
  private final OriginInfoResolver originResolver;

  @Inject
  public AclEntryConverter(SubjectInfoResolver subjectResolver, OriginInfoResolver originResolver) {
    this.subjectResolver = subjectResolver;
    this.originResolver = originResolver;
  }

  @Override
  public AclEntrySEB apply(FactAclEntryRecord record) {
    if (record == null) return null;

    return AclEntrySEB.builder()
            .setId(record.getId())
            .setSubject(subjectResolver.apply(record.getSubjectID()))
            .setOrigin(originResolver.apply(record.getOriginID()))
            .setTimestamp(record.getTimestamp())
            .build();
  }
}
