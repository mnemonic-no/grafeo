package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.request.v1.CreateFactRequest;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.handlers.FactCreateHandler;
import no.mnemonic.act.platform.service.ti.resolvers.FactTypeResolver;
import no.mnemonic.act.platform.service.ti.resolvers.ObjectResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.Objects;
import java.util.UUID;

public class FactCreateDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final FactTypeResolver factTypeResolver;
  private final ObjectResolver objectResolver;
  private final FactCreateHandler factCreateHandler;

  private FactTypeEntity requestedFactType;
  private OriginEntity requestedOrigin;
  private Organization requestedOrganization;

  @Inject
  public FactCreateDelegate(TiSecurityContext securityContext,
                            FactTypeResolver factTypeResolver,
                            ObjectResolver objectResolver,
                            FactCreateHandler factCreateHandler) {
    this.securityContext = securityContext;
    this.factTypeResolver = factTypeResolver;
    this.objectResolver = objectResolver;
    this.factCreateHandler = factCreateHandler;
  }

  public Fact handle(CreateFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    // First resolve some objects which are required later on. This will also validate those request parameters.
    requestedOrigin = factCreateHandler.resolveOrigin(request.getOrigin());
    requestedOrganization = factCreateHandler.resolveOrganization(request.getOrganization(), requestedOrigin);
    requestedFactType = factTypeResolver.resolveFactType(request.getType());

    // Verify that user is allowed to add Facts for the requested organization.
    securityContext.checkPermission(TiFunctionConstants.addFactObjects, requestedOrganization.getId());

    // Validate that requested Fact matches its FactType.
    factCreateHandler.assertValidFactValue(requestedFactType, request.getValue());
    assertValidFactObjectBindings(request);

    FactRecord newFact = toFactRecord(request);
    return factCreateHandler.saveFact(newFact, request.getComment(), request.getAcl());
  }

  private void assertValidFactObjectBindings(CreateFactRequest request) throws InvalidArgumentException {
    // Validate that either source or destination or both are set. One field can be NULL to support bindings of cardinality 1.
    ObjectRecord source = objectResolver.resolveObject(request.getSourceObject());
    ObjectRecord destination = objectResolver.resolveObject(request.getDestinationObject());
    if (source == null && destination == null) {
      throw new InvalidArgumentException()
              .addValidationError("Requested source Object could not be resolved.", "invalid.source.object", "sourceObject", request.getSourceObject())
              .addValidationError("Requested destination Object could not be resolved.", "invalid.destination.object", "destinationObject", request.getDestinationObject());
    }

    // Disallow creating Facts where source and destination are the same Object. Users should create one-legged Facts instead.
    if (source != null && destination != null && Objects.equals(source.getId(), destination.getId())) {
      throw new InvalidArgumentException()
              .addValidationError("Requested source Object is the same as destination Object.", "invalid.source.object", "sourceObject", request.getSourceObject())
              .addValidationError("Requested destination Object is the same as source Object.", "invalid.destination.object", "destinationObject", request.getDestinationObject());
    }

    // Validate that the binding between source Object, Fact and destination Object is valid according to the FactType.
    // Both source and destination ObjectTypes must be the same plus the bidirectional binding flag must match.
    boolean valid = !CollectionUtils.isEmpty(requestedFactType.getRelevantObjectBindings()) && requestedFactType.getRelevantObjectBindings()
            .stream()
            .anyMatch(b -> Objects.equals(b.getSourceObjectTypeID(), ObjectUtils.ifNotNull(source, ObjectRecord::getTypeID)) &&
                    Objects.equals(b.getDestinationObjectTypeID(), ObjectUtils.ifNotNull(destination, ObjectRecord::getTypeID)) &&
                    b.isBidirectionalBinding() == request.isBidirectionalBinding());
    if (!valid) {
      String invalidValue = String.format("sourceObject = %s|destinationObject = %s|bidirectionalBinding = %s", request.getSourceObject(), request.getDestinationObject(), request.isBidirectionalBinding());
      throw new InvalidArgumentException()
              .addValidationError(String.format("Requested binding between Fact and Object(s) is not allowed for FactType with id = %s.", requestedFactType.getId()),
                      "invalid.fact.object.binding", "sourceObject|destinationObject|bidirectionalBinding", invalidValue);
    }
  }

  private FactRecord toFactRecord(CreateFactRequest request) throws InvalidArgumentException {
    ObjectRecord source = objectResolver.resolveObject(request.getSourceObject());
    ObjectRecord destination = objectResolver.resolveObject(request.getDestinationObject());

    return new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(requestedFactType.getId())
            .setValue(request.getValue())
            .setAccessMode(FactRecord.AccessMode.valueOf(request.getAccessMode().name()))
            .setOrganizationID(requestedOrganization.getId())
            .setAddedByID(securityContext.getCurrentUserID())
            .setOriginID(requestedOrigin.getId())
            .setTrust(requestedOrigin.getTrust())
            .setConfidence(ObjectUtils.ifNull(request.getConfidence(), requestedFactType.getDefaultConfidence()))
            .setTimestamp(System.currentTimeMillis())
            .setLastSeenTimestamp(System.currentTimeMillis())
            .setSourceObject(source)
            .setDestinationObject(destination)
            .setBidirectionalBinding(request.isBidirectionalBinding());
  }
}
