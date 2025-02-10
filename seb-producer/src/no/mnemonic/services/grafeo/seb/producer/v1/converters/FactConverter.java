package no.mnemonic.services.grafeo.seb.producer.v1.converters;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.seb.model.v1.FactSEB;
import no.mnemonic.services.grafeo.seb.producer.v1.resolvers.*;

import jakarta.inject.Inject;
import java.util.function.Function;

public class FactConverter implements Function<FactRecord, FactSEB> {

  private final FactTypeInfoDaoResolver typeResolver;
  private final FactInfoDaoResolver inReferenceToResolver;
  private final OrganizationInfoServiceAccountResolver organizationResolver;
  private final OriginInfoDaoResolver originResolver;
  private final SubjectInfoServiceAccountResolver subjectResolver;
  private final ObjectInfoConverter objectConverter;
  private final AclEntryConverter aclEntryConverter;

  @Inject
  public FactConverter(
          FactTypeInfoDaoResolver typeResolver,
          FactInfoDaoResolver inReferenceToResolver,
          OrganizationInfoServiceAccountResolver organizationResolver,
          OriginInfoDaoResolver originResolver,
          SubjectInfoServiceAccountResolver subjectResolver,
          ObjectInfoConverter objectConverter,
          AclEntryConverter aclEntryConverter
  ) {
    this.typeResolver = typeResolver;
    this.inReferenceToResolver = inReferenceToResolver;
    this.organizationResolver = organizationResolver;
    this.originResolver = originResolver;
    this.subjectResolver = subjectResolver;
    this.objectConverter = objectConverter;
    this.aclEntryConverter = aclEntryConverter;
  }

  @Override
  public FactSEB apply(FactRecord record) {
    if (record == null) return null;

    return FactSEB.builder()
            .setId(record.getId())
            .setType(typeResolver.apply(record.getTypeID()))
            .setValue(record.getValue())
            .setInReferenceTo(inReferenceToResolver.apply(record.getInReferenceToID()))
            .setOrganization(organizationResolver.apply(record.getOrganizationID()))
            .setOrigin(originResolver.apply(record.getOriginID()))
            .setAddedBy(subjectResolver.apply(record.getAddedByID()))
            .setLastSeenBy(subjectResolver.apply(record.getLastSeenByID()))
            .setAccessMode(ObjectUtils.ifNotNull(record.getAccessMode(), mode -> FactSEB.AccessMode.valueOf(mode.name())))
            .setTrust(record.getTrust())
            .setConfidence(record.getConfidence())
            .setTimestamp(record.getTimestamp())
            .setLastSeenTimestamp(record.getLastSeenTimestamp())
            .setSourceObject(objectConverter.apply(record.getSourceObject()))
            .setDestinationObject(objectConverter.apply(record.getDestinationObject()))
            .setBidirectionalBinding(record.isBidirectionalBinding())
            .setFlags(SetUtils.set(record.getFlags(), flag -> FactSEB.Flag.valueOf(flag.name())))
            .setAcl(SetUtils.set(record.getAcl(), aclEntryConverter))
            .build();
  }
}
