package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.api.request.v1.CreateMetaFactRequest;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.contexts.TriggerContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.TiServiceEvent;
import no.mnemonic.act.platform.service.ti.handlers.FactCreateHandler;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactRequestResolver;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactTypeRequestResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public class FactCreateMetaDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final TriggerContext triggerContext;
  private final FactTypeRequestResolver factTypeRequestResolver;
  private final FactRequestResolver factRequestResolver;
  private final FactCreateHandler factCreateHandler;

  private FactTypeEntity requestedFactType;
  private OriginEntity requestedOrigin;
  private Organization requestedOrganization;

  private Clock clock = Clock.systemUTC();

  @Inject
  public FactCreateMetaDelegate(TiSecurityContext securityContext,
                                TriggerContext triggerContext,
                                FactTypeRequestResolver factTypeRequestResolver,
                                FactRequestResolver factRequestResolver,
                                FactCreateHandler factCreateHandler) {
    this.securityContext = securityContext;
    this.triggerContext = triggerContext;
    this.factTypeRequestResolver = factTypeRequestResolver;
    this.factRequestResolver = factRequestResolver;
    this.factCreateHandler = factCreateHandler;
  }

  public Fact handle(CreateMetaFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch referenced Fact and verify that it exists.
    FactRecord referencedFact = factRequestResolver.resolveFact(request.getFact());
    // Verify that user is allowed to access the referenced Fact.
    securityContext.checkReadPermission(referencedFact);

    // Resolve some objects which are required later on. This will also validate those request parameters.
    requestedOrigin = factCreateHandler.resolveOrigin(request.getOrigin());
    requestedOrganization = factCreateHandler.resolveOrganization(request.getOrganization(), requestedOrigin);
    requestedFactType = factTypeRequestResolver.resolveFactType(request.getType());

    // Verify that user is allowed to add Facts for the requested organization.
    securityContext.checkPermission(TiFunctionConstants.addThreatIntelFact, requestedOrganization.getId());

    // Validate that requested Fact matches its FactType.
    factCreateHandler.assertValidFactValue(requestedFactType, request.getValue());
    assertValidFactBinding(request, referencedFact);

    // Save everything in database.
    Fact addedFact = factCreateHandler.saveFact(toFactRecord(request, referencedFact), request.getComment(),
            ListUtils.list(factCreateHandler.resolveSubjects(request.getAcl()), Subject::getId));

    // Register TriggerEvent before returning added Fact.
    registerTriggerEvent(addedFact);

    return addedFact;
  }

  private void assertValidFactBinding(CreateMetaFactRequest request, FactRecord referencedFact) throws InvalidArgumentException {
    // Validate that the referenced Fact has the correct type according to the requested FactType.
    boolean valid = !CollectionUtils.isEmpty(requestedFactType.getRelevantFactBindings()) && requestedFactType.getRelevantFactBindings()
            .stream()
            .anyMatch(b -> Objects.equals(b.getFactTypeID(), referencedFact.getTypeID()));
    if (!valid) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("Requested binding between Facts is not allowed for FactType with id = %s.", requestedFactType.getId()),
                      "invalid.meta.fact.binding", "type", request.getType());
    }
  }

  private FactRecord toFactRecord(CreateMetaFactRequest request, FactRecord referencedFact) throws InvalidArgumentException {
    // Ensure that 'timestamp' and 'lastSeenTimestamp' are the same for newly created Facts.
    final long now = clock.millis();
    return new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(requestedFactType.getId())
            .setValue(request.getValue())
            .setInReferenceToID(referencedFact.getId())
            .setOrganizationID(requestedOrganization.getId())
            .setAddedByID(securityContext.getCurrentUserID())
            .setLastSeenByID(securityContext.getCurrentUserID())
            .setOriginID(requestedOrigin.getId())
            .setTrust(requestedOrigin.getTrust())
            .setConfidence(ObjectUtils.ifNull(request.getConfidence(), requestedFactType.getDefaultConfidence()))
            .setAccessMode(factCreateHandler.resolveAccessMode(referencedFact, request.getAccessMode()))
            .setTimestamp(now)
            .setLastSeenTimestamp(now)
            .setFlags(referencedFact.isSet(FactRecord.Flag.TimeGlobalIndex) ? SetUtils.set(FactRecord.Flag.TimeGlobalIndex) : SetUtils.set());
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

  FactCreateMetaDelegate withClock(Clock clock) {
    this.clock = clock;
    return this;
  }
}
