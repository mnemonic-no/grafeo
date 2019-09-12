package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.request.v1.RetractFactRequest;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.MetaFactBindingEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.contexts.TriggerContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.TiServiceEvent;
import no.mnemonic.act.platform.service.ti.helpers.FactCreateHelper;
import no.mnemonic.act.platform.service.ti.helpers.FactStorageHelper;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeResolver;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class FactRetractDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final TriggerContext triggerContext;
  private final FactManager factManager;
  private final FactTypeResolver factTypeResolver;
  private final FactCreateHelper factCreateHelper;
  private final FactStorageHelper factStorageHelper;
  private final Function<FactEntity, Fact> factConverter;

  private FactTypeEntity retractionFactType;
  private OriginEntity requestedOrigin;
  private Organization requestedOrganization;

  @Inject
  public FactRetractDelegate(TiSecurityContext securityContext,
                             TriggerContext triggerContext,
                             FactManager factManager,
                             FactTypeResolver factTypeResolver,
                             FactCreateHelper factCreateHelper,
                             FactStorageHelper factStorageHelper,
                             Function<FactEntity, Fact> factConverter) {
    this.securityContext = securityContext;
    this.triggerContext = triggerContext;
    this.factManager = factManager;
    this.factTypeResolver = factTypeResolver;
    this.factCreateHelper = factCreateHelper;
    this.factStorageHelper = factStorageHelper;
    this.factConverter = factConverter;
  }

  public Fact handle(RetractFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch Fact to retract and verify that it exists.
    FactEntity factToRetract = fetchExistingFact(request.getFact());
    // Verify that user is allowed to access the Fact to retract.
    securityContext.checkReadPermission(factToRetract);

    // Resolve some objects which are required later on. This will also validate those request parameters.
    retractionFactType = factTypeResolver.resolveRetractionFactType();
    requestedOrigin = factCreateHelper.resolveOrigin(request.getOrigin());
    requestedOrganization = factCreateHelper.resolveOrganization(request.getOrganization(), requestedOrigin);

    // Verify that user is allowed to add Facts for the requested organization.
    securityContext.checkPermission(TiFunctionConstants.addFactObjects, requestedOrganization.getId());

    // Save everything in database.
    FactEntity retractionFact = saveRetractionFact(request, factToRetract);
    List<UUID> subjectsAddedToAcl = factStorageHelper.saveInitialAclForNewFact(retractionFact, request.getAcl());
    factStorageHelper.saveCommentForFact(retractionFact, request.getComment());
    // Index everything into ElasticSearch.
    indexCreatedFact(retractionFact, retractionFactType, subjectsAddedToAcl);
    reindexExistingFact(factToRetract.getId(), d -> d.setRetracted(true));

    // Register TriggerEvent before returning Retraction Fact.
    Fact retractionFactParameter = factConverter.apply(retractionFact);
    Fact retractedFactParameter = factConverter.apply(factToRetract);
    registerTriggerEvent(retractionFactParameter, retractedFactParameter);

    return retractionFactParameter;
  }

  private FactEntity saveRetractionFact(RetractFactRequest request, FactEntity factToRetract) throws InvalidArgumentException {
    FactEntity retractionFact = new FactEntity()
            .setId(UUID.randomUUID()) // Need to provide client-generated ID.
            .setTypeID(retractionFactType.getId())
            .setInReferenceToID(factToRetract.getId())
            .setOrganizationID(requestedOrganization.getId())
            .setAddedByID(securityContext.getCurrentUserID())
            .setOriginID(requestedOrigin.getId())
            .setTrust(requestedOrigin.getTrust())
            .setConfidence(ObjectUtils.ifNull(request.getConfidence(), retractionFactType.getDefaultConfidence()))
            .setAccessMode(resolveAccessMode(factToRetract, request.getAccessMode()))
            .setTimestamp(System.currentTimeMillis())
            .setLastSeenTimestamp(System.currentTimeMillis());
    retractionFact = factManager.saveFact(retractionFact);

    // Save retraction Fact as a meta Fact of the retracted Fact.
    factManager.saveMetaFactBinding(new MetaFactBindingEntity()
            .setFactID(factToRetract.getId())
            .setMetaFactID(retractionFact.getId())
    );

    return retractionFact;
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
