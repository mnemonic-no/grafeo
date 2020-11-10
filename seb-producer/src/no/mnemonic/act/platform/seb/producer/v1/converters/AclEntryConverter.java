package no.mnemonic.act.platform.seb.producer.v1.converters;

import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.seb.model.v1.AclEntrySEB;
import no.mnemonic.act.platform.seb.producer.v1.resolvers.OriginInfoDaoResolver;
import no.mnemonic.act.platform.seb.producer.v1.resolvers.SubjectInfoServiceAccountResolver;

import javax.inject.Inject;
import java.util.function.Function;

public class AclEntryConverter implements Function<FactAclEntryRecord, AclEntrySEB> {

  private final SubjectInfoServiceAccountResolver subjectResolver;
  private final OriginInfoDaoResolver originResolver;

  @Inject
  public AclEntryConverter(SubjectInfoServiceAccountResolver subjectResolver, OriginInfoDaoResolver originResolver) {
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
