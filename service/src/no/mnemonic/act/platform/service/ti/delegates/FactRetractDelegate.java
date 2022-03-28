package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.api.request.v1.RetractFactRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.contexts.TriggerContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.TiServiceEvent;
import no.mnemonic.act.platform.service.ti.converters.response.FactResponseConverter;
import no.mnemonic.act.platform.service.ti.handlers.FactCreateHandler;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactRequestResolver;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactTypeRequestResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.inject.Inject;
import java.time.Clock;
import java.util.UUID;

public class FactRetractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final TriggerContext triggerContext;
  private final ObjectFactDao objectFactDao;
  private final FactTypeRequestResolver factTypeRequestResolver;
  private final FactRequestResolver factRequestResolver;
  private final FactCreateHandler factCreateHandler;
  private final FactResponseConverter factResponseConverter;

  private FactTypeEntity retractionFactType;
  private OriginEntity requestedOrigin;
  private Organization requestedOrganization;

  private Clock clock = Clock.systemUTC();

  @Inject
  public FactRetractDelegate(TiSecurityContext securityContext,
                             TriggerContext triggerContext,
                             ObjectFactDao objectFactDao,
                             FactTypeRequestResolver factTypeRequestResolver,
                             FactRequestResolver factRequestResolver,
                             FactCreateHandler factCreateHandler,
                             FactResponseConverter factResponseConverter) {
    this.securityContext = securityContext;
    this.triggerContext = triggerContext;
    this.objectFactDao = objectFactDao;
    this.factTypeRequestResolver = factTypeRequestResolver;
    this.factRequestResolver = factRequestResolver;
    this.factCreateHandler = factCreateHandler;
    this.factResponseConverter = factResponseConverter;
  }

  public Fact handle(RetractFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch Fact to retract and verify that it exists.
    FactRecord factToRetract = factRequestResolver.resolveFact(request.getFact());
    // Verify that user is allowed to access the Fact to retract.
    securityContext.checkReadPermission(factToRetract);

    // Resolve some objects which are required later on. This will also validate those request parameters.
    retractionFactType = factTypeRequestResolver.resolveRetractionFactType();
    requestedOrigin = factCreateHandler.resolveOrigin(request.getOrigin());
    requestedOrganization = factCreateHandler.resolveOrganization(request.getOrganization(), requestedOrigin);

    // Verify that user is allowed to add Facts for the requested organization.
    securityContext.checkPermission(TiFunctionConstants.addThreatIntelFact, requestedOrganization.getId());

    // Save everything in database.
    Fact retractionFact = factCreateHandler.saveFact(toFactRecord(request, factToRetract), request.getComment(),
            ListUtils.list(factCreateHandler.resolveSubjects(request.getAcl()), Subject::getId));
    factToRetract = objectFactDao.retractFact(factToRetract);

    // Register TriggerEvent before returning Retraction Fact.
    registerTriggerEvent(retractionFact, factResponseConverter.apply(factToRetract));

    return retractionFact;
  }

  private FactRecord toFactRecord(RetractFactRequest request, FactRecord factToRetract) throws InvalidArgumentException {
    // Ensure that 'timestamp' and 'lastSeenTimestamp' are the same for newly created Facts.
    final long now = clock.millis();
    return new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(retractionFactType.getId())
            .setInReferenceToID(factToRetract.getId())
            .setOrganizationID(requestedOrganization.getId())
            .setAddedByID(securityContext.getCurrentUserID())
            .setLastSeenByID(securityContext.getCurrentUserID())
            .setOriginID(requestedOrigin.getId())
            .setTrust(requestedOrigin.getTrust())
            .setConfidence(ObjectUtils.ifNull(request.getConfidence(), retractionFactType.getDefaultConfidence()))
            .setAccessMode(factCreateHandler.resolveAccessMode(factToRetract, request.getAccessMode()))
            .setTimestamp(now)
            .setLastSeenTimestamp(now)
            .addFlag(FactRecord.Flag.TimeGlobalIndex);
  }

  private void registerTriggerEvent(Fact retractionFact, Fact retractedFact) {
    // The AccessMode of the Retraction Fact cannot be less restrictive than the AccessMode of the Fact to retract,
    // thus, it is safe to always include both Facts as context parameters when the event's AccessMode is set to
    // the AccessMode of the Retraction Fact (i.e. to the more restrictive AccessMode).
    TiServiceEvent event = TiServiceEvent.forEvent(TiServiceEvent.EventName.FactRetracted)
            .setOrganization(ObjectUtils.ifNotNull(retractionFact.getOrganization(), Organization.Info::getId))
            .setAccessMode(retractionFact.getAccessMode())
            .addContextParameter(TiServiceEvent.ContextParameter.RetractionFact.name(), retractionFact)
            .addContextParameter(TiServiceEvent.ContextParameter.RetractedFact.name(), retractedFact)
            .build();
    triggerContext.registerTriggerEvent(event);
  }

  /* Setters used for unit testing */

  FactRetractDelegate withClock(Clock clock) {
    this.clock = clock;
    return this;
  }
}
