package no.mnemonic.services.grafeo.service.implementation.converters.response;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.grafeo.api.model.v1.AclEntry;
import no.mnemonic.services.grafeo.api.model.v1.Origin;
import no.mnemonic.services.grafeo.api.model.v1.Subject;
import no.mnemonic.services.grafeo.dao.api.record.FactAclEntryRecord;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.OriginByIdResponseResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.SubjectByIdResponseResolver;

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
