package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.request.v1.RetractFactRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.contexts.TriggerContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.TiServiceEvent;
import no.mnemonic.act.platform.service.ti.converters.FactConverter;
import no.mnemonic.act.platform.service.ti.handlers.FactCreateHandler;
import no.mnemonic.act.platform.service.ti.resolvers.FactResolver;
import no.mnemonic.act.platform.service.ti.resolvers.FactTypeResolver;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.UUID;

import static no.mnemonic.act.platform.service.ti.helpers.FactHelper.withAcl;
import static no.mnemonic.act.platform.service.ti.helpers.FactHelper.withComment;

public class FactRetractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final TriggerContext triggerContext;
  private final ObjectFactDao objectFactDao;
  private final FactTypeResolver factTypeResolver;
  private final FactResolver factResolver;
  private final FactCreateHandler factCreateHandler;
  private final FactConverter factConverter;

  private FactTypeEntity retractionFactType;
  private OriginEntity requestedOrigin;
  private Organization requestedOrganization;

  @Inject
  public FactRetractDelegate(TiSecurityContext securityContext,
                             TriggerContext triggerContext,
                             ObjectFactDao objectFactDao,
                             FactTypeResolver factTypeResolver,
                             FactResolver factResolver,
                             FactCreateHandler factCreateHandler,
                             FactConverter factConverter) {
    this.securityContext = securityContext;
    this.triggerContext = triggerContext;
    this.objectFactDao = objectFactDao;
    this.factTypeResolver = factTypeResolver;
    this.factResolver = factResolver;
    this.factCreateHandler = factCreateHandler;
    this.factConverter = factConverter;
  }

  public Fact handle(RetractFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch Fact to retract and verify that it exists.
    FactRecord factToRetract = factResolver.resolveFact(request.getFact());
    // Verify that user is allowed to access the Fact to retract.
    securityContext.checkReadPermission(factToRetract);

    // Resolve some objects which are required later on. This will also validate those request parameters.
    retractionFactType = factTypeResolver.resolveRetractionFactType();
    requestedOrigin = factCreateHandler.resolveOrigin(request.getOrigin());
    requestedOrganization = factCreateHandler.resolveOrganization(request.getOrganization(), requestedOrigin);

    // Verify that user is allowed to add Facts for the requested organization.
    securityContext.checkPermission(TiFunctionConstants.addFactObjects, requestedOrganization.getId());

    // Save everything in database.
    FactRecord retractionFact = saveRetractionFact(request, factToRetract);
    factToRetract = objectFactDao.retractFact(factToRetract);

    // Register TriggerEvent before returning Retraction Fact.
    Fact retractionFactParameter = factConverter.apply(retractionFact);
    Fact retractedFactParameter = factConverter.apply(factToRetract);
    registerTriggerEvent(retractionFactParameter, retractedFactParameter);

    return retractionFactParameter;
  }

  private FactRecord saveRetractionFact(RetractFactRequest request, FactRecord factToRetract) throws InvalidArgumentException {
    FactRecord retractionFact = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(retractionFactType.getId())
            .setInReferenceToID(factToRetract.getId())
            .setOrganizationID(requestedOrganization.getId())
            .setAddedByID(securityContext.getCurrentUserID())
            .setOriginID(requestedOrigin.getId())
            .setTrust(requestedOrigin.getTrust())
            .setConfidence(ObjectUtils.ifNull(request.getConfidence(), retractionFactType.getDefaultConfidence()))
            .setAccessMode(factCreateHandler.resolveAccessMode(factToRetract, request.getAccessMode()))
            .setTimestamp(System.currentTimeMillis())
            .setLastSeenTimestamp(System.currentTimeMillis());
    retractionFact = withAcl(retractionFact, securityContext.getCurrentUserID(), request.getAcl());
    retractionFact = withComment(retractionFact, request.getComment());

    return objectFactDao.storeFact(retractionFact);
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
}
