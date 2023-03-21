package no.mnemonic.services.grafeo.service.ti.converters.response;

import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.model.v1.Object;
import no.mnemonic.services.grafeo.api.model.v1.*;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.service.ti.TiSecurityContext;
import no.mnemonic.services.grafeo.service.ti.handlers.FactRetractionHandler;
import no.mnemonic.services.grafeo.service.ti.resolvers.response.FactTypeByIdResponseResolver;
import no.mnemonic.services.grafeo.service.ti.resolvers.response.OrganizationByIdResponseResolver;
import no.mnemonic.services.grafeo.service.ti.resolvers.response.OriginByIdResponseResolver;
import no.mnemonic.services.grafeo.service.ti.resolvers.response.SubjectByIdResponseResolver;

import javax.inject.Inject;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class FactResponseConverter implements Function<FactRecord, Fact> {

  private static final Logger LOGGER = Logging.getLogger(FactResponseConverter.class);

  private final FactTypeByIdResponseResolver factTypeConverter;
  private final OriginByIdResponseResolver originConverter;
  private final ObjectResponseConverter objectConverter;
  private final OrganizationByIdResponseResolver organizationConverter;
  private final SubjectByIdResponseResolver subjectConverter;
  private final FactRetractionHandler factRetractionHandler;
  private final ObjectFactDao objectFactDao;
  private final TiSecurityContext securityContext;

  @Inject
  public FactResponseConverter(FactTypeByIdResponseResolver factTypeConverter,
                               OriginByIdResponseResolver originConverter,
                               ObjectResponseConverter objectConverter,
                               OrganizationByIdResponseResolver organizationConverter,
                               SubjectByIdResponseResolver subjectConverter,
                               FactRetractionHandler factRetractionHandler,
                               ObjectFactDao objectFactDao,
                               TiSecurityContext securityContext) {
    this.factTypeConverter = factTypeConverter;
    this.originConverter = originConverter;
    this.objectConverter = objectConverter;
    this.organizationConverter = organizationConverter;
    this.subjectConverter = subjectConverter;
    this.factRetractionHandler = factRetractionHandler;
    this.objectFactDao = objectFactDao;
    this.securityContext = securityContext;
  }

  @Override
  public Fact apply(FactRecord record) {
    if (record == null) return null;
    return Fact.builder()
            .setId(record.getId())
            .setType(ObjectUtils.ifNotNull(factTypeConverter.apply(record.getTypeID()), FactType::toInfo))
            .setValue(record.getValue())
            .setInReferenceTo(ObjectUtils.ifNotNull(convertInReferenceTo(record.getInReferenceToID()), Fact::toInfo))
            .setOrganization(ObjectUtils.ifNotNull(organizationConverter.apply(record.getOrganizationID()), Organization::toInfo))
            .setAddedBy(ObjectUtils.ifNotNull(subjectConverter.apply(record.getAddedByID()), Subject::toInfo))
            .setLastSeenBy(ObjectUtils.ifNotNull(subjectConverter.apply(record.getLastSeenByID()), Subject::toInfo))
            .setOrigin(ObjectUtils.ifNotNull(originConverter.apply(record.getOriginID()), Origin::toInfo))
            .setTrust(record.getTrust())
            .setConfidence(record.getConfidence())
            .setAccessMode(ObjectUtils.ifNotNull(record.getAccessMode(), m -> AccessMode.valueOf(m.name())))
            .setTimestamp(record.getTimestamp())
            .setLastSeenTimestamp(record.getLastSeenTimestamp())
            .setSourceObject(ObjectUtils.ifNotNull(objectConverter.apply(record.getSourceObject()), Object::toInfo))
            .setDestinationObject(ObjectUtils.ifNotNull(objectConverter.apply(record.getDestinationObject()), Object::toInfo))
            .setBidirectionalBinding(record.isBidirectionalBinding())
            .setFlags(convertFlags(record))
            .build();
  }

  private Fact convertInReferenceTo(UUID inReferenceToID) {
    if (inReferenceToID == null) return null;

    FactRecord inReferenceTo = objectFactDao.getFact(inReferenceToID);
    if (inReferenceTo == null || !securityContext.hasReadPermission(inReferenceTo)) {
      // If User doesn't have access to 'inReferenceTo' Fact it shouldn't be returned as part of the converted Fact.
      LOGGER.debug("Removed inReferenceTo Fact from result because user does not have access to it (id = %s).", inReferenceToID);
      return null;
    }

    // Convert 'inReferenceTo' Fact, but avoid resolving recursive 'inReferenceTo' Facts.
    return apply(inReferenceTo.setInReferenceToID(null));
  }

  private Set<Fact.Flag> convertFlags(FactRecord record) {
    return factRetractionHandler.isRetracted(record) ? SetUtils.set(Fact.Flag.Retracted) : SetUtils.set();
  }
}
