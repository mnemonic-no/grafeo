package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.request.v1.CreateMetaFactRequest;
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
import no.mnemonic.commons.utilities.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.Objects;
import java.util.UUID;

public class FactCreateMetaDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final TriggerContext triggerContext;
  private final ObjectFactDao objectFactDao;
  private final FactTypeResolver factTypeResolver;
  private final FactResolver factResolver;
  private final FactCreateHandler factCreateHandler;
  private final FactConverter factConverter;

  private FactTypeEntity requestedFactType;
  private OriginEntity requestedOrigin;
  private Organization requestedOrganization;

  @Inject
  public FactCreateMetaDelegate(TiSecurityContext securityContext,
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

  public Fact handle(CreateMetaFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch referenced Fact and verify that it exists.
    FactRecord referencedFact = factResolver.resolveFact(request.getFact());
    // Verify that user is allowed to access the referenced Fact.
    securityContext.checkReadPermission(referencedFact);

    // Resolve some objects which are required later on. This will also validate those request parameters.
    requestedOrigin = factCreateHandler.resolveOrigin(request.getOrigin());
    requestedOrganization = factCreateHandler.resolveOrganization(request.getOrganization(), requestedOrigin);
    requestedFactType = factTypeResolver.resolveFactType(request.getType());

    // Verify that user is allowed to add Facts for the requested organization.
    securityContext.checkPermission(TiFunctionConstants.addFactObjects, requestedOrganization.getId());

    // Validate that requested Fact matches its FactType.
    factCreateHandler.assertValidFactValue(requestedFactType, request.getValue());
    assertValidFactBinding(request, referencedFact);

    FactRecord newFact = toFactRecord(request, referencedFact);
    FactRecord existingFact = resolveExistingFact(newFact);
    if (existingFact != null) {
      // Refresh an existing Fact (plus adding any additional ACL entries and comments).
      existingFact = factCreateHandler.withAcl(existingFact, request.getAcl());
      existingFact = factCreateHandler.withComment(existingFact, request.getComment());
      existingFact = objectFactDao.refreshFact(existingFact);
    } else {
      // Or create a new Fact.
      newFact = objectFactDao.storeFact(newFact);
    }

    // Register TriggerEvent before returning added Fact.
    Fact addedFact = factConverter.apply(existingFact != null ? existingFact : newFact);
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
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(requestedFactType.getId())
            .setValue(request.getValue())
            .setInReferenceToID(referencedFact.getId())
            .setOrganizationID(requestedOrganization.getId())
            .setAddedByID(securityContext.getCurrentUserID())
            .setOriginID(requestedOrigin.getId())
            .setTrust(requestedOrigin.getTrust())
            .setConfidence(ObjectUtils.ifNull(request.getConfidence(), requestedFactType.getDefaultConfidence()))
            .setAccessMode(factCreateHandler.resolveAccessMode(referencedFact, request.getAccessMode()))
            .setTimestamp(System.currentTimeMillis())
            .setLastSeenTimestamp(System.currentTimeMillis());
    fact = factCreateHandler.withAcl(fact, request.getAcl());
    fact = factCreateHandler.withComment(fact, request.getComment());

    return fact;
  }

  private FactRecord resolveExistingFact(FactRecord newFact) {
    // Fetch any Facts which are logically the same as the Fact to create, apply permission check and return existing Fact if accessible.
    return objectFactDao.retrieveExistingFacts(newFact)
            .stream()
            .filter(securityContext::hasReadPermission)
            .findFirst()
            .orElse(null);
  }

  private void registerTriggerEvent(Fact addedFact) {
    TiServiceEvent event = TiServiceEvent.forEvent(TiServiceEvent.EventName.FactAdded)
            .setOrganization(ObjectUtils.ifNotNull(addedFact.getOrganization(), Organization.Info::getId))
            .setAccessMode(addedFact.getAccessMode())
            .addContextParameter(TiServiceEvent.ContextParameter.AddedFact.name(), addedFact)
            .build();
    triggerContext.registerTriggerEvent(event);
  }
}
