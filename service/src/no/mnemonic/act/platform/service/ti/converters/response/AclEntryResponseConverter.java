package no.mnemonic.act.platform.service.ti.converters.response;

import no.mnemonic.act.platform.api.model.v1.AclEntry;
import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.service.ti.resolvers.response.OriginByIdResponseResolver;
import no.mnemonic.act.platform.service.ti.resolvers.response.SubjectByIdResponseResolver;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.function.Function;

public class AclEntryResponseConverter implements Function<FactAclEntryRecord, AclEntry> {

  private final OriginByIdResponseResolver originConverter;
  private final SubjectByIdResponseResolver subjectConverter;

  @Inject
  public AclEntryResponseConverter(OriginByIdResponseResolver originConverter,
                                   SubjectByIdResponseResolver subjectConverter) {
    this.originConverter = originConverter;
    this.subjectConverter = subjectConverter;
  }

  @Override
  public AclEntry apply(FactAclEntryRecord record) {
    if (record == null) return null;
    return AclEntry.builder()
            .setId(record.getId())
            .setOrigin(ObjectUtils.ifNotNull(originConverter.apply(record.getOriginID()), Origin::toInfo))
            .setSubject(ObjectUtils.ifNotNull(subjectConverter.apply(record.getSubjectID()), Subject::toInfo))
            .setTimestamp(record.getTimestamp())
            .build();
  }
}
