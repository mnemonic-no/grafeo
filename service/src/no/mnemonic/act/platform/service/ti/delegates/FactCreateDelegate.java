package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.api.request.v1.CreateFactRequest;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.contexts.TriggerContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.TiServiceEvent;
import no.mnemonic.act.platform.service.ti.handlers.FactCreateHandler;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactTypeRequestResolver;
import no.mnemonic.act.platform.service.ti.resolvers.request.ObjectRequestResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.inject.Inject;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public class FactCreateDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final TriggerContext triggerContext;
  private final FactTypeRequestResolver factTypeRequestResolver;
  private final ObjectRequestResolver objectRequestResolver;
  private final FactCreateHandler factCreateHandler;

  private FactTypeEntity requestedFactType;
  private OriginEntity requestedOrigin;
  private Organization requestedOrganization;

  private Clock clock = Clock.systemUTC();

  @Inject
  public FactCreateDelegate(TiSecurityContext securityContext,
                            TriggerContext triggerContext,
                            FactTypeRequestResolver factTypeRequestResolver,
                            ObjectRequestResolver objectRequestResolver,
                            FactCreateHandler factCreateHandler) {
    this.securityContext = securityContext;
    this.triggerContext = triggerContext;
    this.factTypeRequestResolver = factTypeRequestResolver;
    this.objectRequestResolver = objectRequestResolver;
    this.factCreateHandler = factCreateHandler;
  }

  public Fact handle(CreateFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    // First resolve some objects which are required later on. This will also validate those request parameters.
    requestedOrigin = factCreateHandler.resolveOrigin(request.getOrigin());
    requestedOrganization = factCreateHandler.resolveOrganization(request.getOrganization(), requestedOrigin);
    requestedFactType = factTypeRequestResolver.resolveFactType(request.getType());

    // Verify that user is allowed to add Facts for the requested organization.
    securityContext.checkPermission(TiFunctionConstants.addThreatIntelFact, requestedOrganization.getId());

    // Validate that requested Fact matches its FactType.
    factCreateHandler.assertValidFactValue(requestedFactType, request.getValue());
    assertValidFactObjectBindings(request);

    // Save everything in database.
    Fact addedFact = factCreateHandler.saveFact(toFactRecord(request), request.getComment(),
            ListUtils.list(factCreateHandler.resolveSubjects(request.getAcl()), Subject::getId));

    // Register TriggerEvent before returning added Fact.
    registerTriggerEvent(addedFact);

    return addedFact;
  }

  private void assertValidFactObjectBindings(CreateFactRequest request) throws InvalidArgumentException {
    // Validate that either source or destination or both are set. One field can be NULL to support bindings of cardinality 1.
    ObjectRecord source = objectRequestResolver.resolveObject(request.getSourceObject(), "sourceObject");
    ObjectRecord destination = objectRequestResolver.resolveObject(request.getDestinationObject(), "destinationObject");
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
    ObjectRecord source = objectRequestResolver.resolveObject(request.getSourceObject(), "sourceObject");
    ObjectRecord destination = objectRequestResolver.resolveObject(request.getDestinationObject(), "destinationObject");

    // Ensure that 'timestamp' and 'lastSeenTimestamp' are the same for newly created Facts.
    final long now = clock.millis();
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
            .setTimestamp(now)
            .setLastSeenTimestamp(now)
            .setSourceObject(source)
            .setDestinationObject(destination)
            .setBidirectionalBinding(request.isBidirectionalBinding());
  }

  private void registerTriggerEvent(Fact addedFact) {
    TiServiceEvent event = TiServiceEvent.forEvent(TiServiceEvent.EventName.FactAdded)
            .setOrganization(ObjectUtils.ifNotNull(addedFact.getOrganization(), Organization.Info::getId))
            .setAccessMode(addedFact.getAccessMode())
            .addContextParameter(TiServiceEvent.ContextParameter.AddedFact.name(), addedFact)
            .build();
    triggerContext.registerTriggerEvent(event);
  }

  /* Setters used for unit testing */

  FactCreateDelegate withClock(Clock clock) {
    this.clock = clock;
    return this;
  }
}
