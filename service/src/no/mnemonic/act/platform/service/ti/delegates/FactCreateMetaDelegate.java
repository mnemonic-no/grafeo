package no.mnemonic.act.platform.service.ti.delegates;

import com.google.common.collect.Streams;
import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.request.v1.CreateMetaFactRequest;
import no.mnemonic.act.platform.dao.api.FactExistenceSearchCriteria;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.MetaFactBindingEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.SearchResult;
import no.mnemonic.act.platform.service.contexts.TriggerContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.TiServiceEvent;
import no.mnemonic.act.platform.service.ti.helpers.FactCreateHelper;
import no.mnemonic.act.platform.service.ti.helpers.FactStorageHelper;
import no.mnemonic.act.platform.service.ti.resolvers.FactTypeResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FactCreateMetaDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final TriggerContext triggerContext;
  private final FactManager factManager;
  private final FactSearchManager factSearchManager;
  private final FactTypeResolver factTypeResolver;
  private final FactCreateHelper factCreateHelper;
  private final FactStorageHelper factStorageHelper;
  private final Function<FactEntity, Fact> factConverter;

  private FactTypeEntity requestedFactType;
  private OriginEntity requestedOrigin;
  private Organization requestedOrganization;

  @Inject
  public FactCreateMetaDelegate(TiSecurityContext securityContext,
                                TriggerContext triggerContext,
                                FactManager factManager,
                                FactSearchManager factSearchManager,
                                FactTypeResolver factTypeResolver,
                                FactCreateHelper factCreateHelper,
                                FactStorageHelper factStorageHelper,
                                Function<FactEntity, Fact> factConverter) {
    this.securityContext = securityContext;
    this.triggerContext = triggerContext;
    this.factManager = factManager;
    this.factSearchManager = factSearchManager;
    this.factTypeResolver = factTypeResolver;
    this.factCreateHelper = factCreateHelper;
    this.factStorageHelper = factStorageHelper;
    this.factConverter = factConverter;
  }

  public Fact handle(CreateMetaFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch referenced Fact and verify that it exists.
    FactEntity referencedFact = fetchExistingFact(request.getFact());
    // Verify that user is allowed to access the referenced Fact.
    securityContext.checkReadPermission(referencedFact);

    // Resolve some objects which are required later on. This will also validate those request parameters.
    requestedOrigin = factCreateHelper.resolveOrigin(request.getOrigin());
    requestedOrganization = factCreateHelper.resolveOrganization(request.getOrganization(), requestedOrigin);
    requestedFactType = factTypeResolver.resolveFactType(request.getType());
    if (Objects.equals(requestedFactType.getId(), factTypeResolver.resolveRetractionFactType().getId())) {
      throw new AccessDeniedException("Not allowed to manually use system-defined Retraction FactType. Use /retract endpoint instead.");
    }

    // Verify that user is allowed to add Facts for the requested organization.
    securityContext.checkPermission(TiFunctionConstants.addFactObjects, requestedOrganization.getId());

    // Validate that requested Fact matches its FactType.
    assertValidFactValue(requestedFactType, request.getValue());
    assertValidFactBinding(request, referencedFact);

    FactEntity metaFact = resolveExistingFact(request, referencedFact);
    if (metaFact != null) {
      // Refresh an existing Fact.
      metaFact = factManager.refreshFact(metaFact.getId());
      List<UUID> subjectsAddedToAcl = factStorageHelper.saveAdditionalAclForFact(metaFact, request.getAcl());
      // Reindex existing Fact in ElasticSearch.
      reindexExistingFact(metaFact, subjectsAddedToAcl);
    } else {
      // Or create a new Fact.
      metaFact = saveFact(request, referencedFact);
      List<UUID> subjectsAddedToAcl = factStorageHelper.saveInitialAclForNewFact(metaFact, request.getAcl());
      // Index new Fact into ElasticSearch.
      indexCreatedFact(metaFact, subjectsAddedToAcl);
    }

    // Always add provided comment.
    factStorageHelper.saveCommentForFact(metaFact, request.getComment());

    // Register TriggerEvent before returning added Fact.
    Fact addedFact = factConverter.apply(metaFact);
    registerTriggerEvent(addedFact);

    return addedFact;
  }

  private void assertValidFactBinding(CreateMetaFactRequest request, FactEntity referencedFact) throws InvalidArgumentException {
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

  private FactEntity resolveExistingFact(CreateMetaFactRequest request, FactEntity referencedFact) throws InvalidArgumentException {
    FactExistenceSearchCriteria criteria = FactExistenceSearchCriteria.builder()
            .setFactValue(request.getValue())
            .setFactTypeID(requestedFactType.getId())
            .setOriginID(requestedOrigin.getId())
            .setOrganizationID(requestedOrganization.getId())
            .setConfidence(resolveConfidence(request))
            .setAccessMode(resolveAccessMode(referencedFact, request.getAccessMode()).name())
            .setInReferenceTo(referencedFact.getId())
            .build();

    // Try to fetch any existing Facts from ElasticSearch.
    SearchResult<FactDocument> result = factSearchManager.retrieveExistingFacts(criteria);
    if (result.getCount() <= 0) {
      return null; // No results, need to create new Fact.
    }

    // Fetch the authorative data from Cassandra, apply permission check and return existing Fact if accessible.
    List<UUID> factID = result.getValues().stream().map(FactDocument::getId).collect(Collectors.toList());
    return Streams.stream(factManager.getFacts(factID))
            .filter(securityContext::hasReadPermission)
            .findFirst()
            .orElse(null);
  }

  private FactEntity saveFact(CreateMetaFactRequest request, FactEntity referencedFact) throws InvalidArgumentException {
    FactEntity metaFact = new FactEntity()
            .setId(UUID.randomUUID()) // Need to provide client-generated ID.
            .setTypeID(requestedFactType.getId())
            .setValue(request.getValue())
            .setInReferenceToID(referencedFact.getId())
            .setOrganizationID(requestedOrganization.getId())
            .setAddedByID(securityContext.getCurrentUserID())
            .setOriginID(requestedOrigin.getId())
            .setTrust(requestedOrigin.getTrust())
            .setConfidence(resolveConfidence(request))
            .setAccessMode(resolveAccessMode(referencedFact, request.getAccessMode()))
            .setTimestamp(System.currentTimeMillis())
            .setLastSeenTimestamp(System.currentTimeMillis());

    metaFact = factManager.saveFact(metaFact);
    // Also save binding between referenced Fact and new meta Fact.
    factManager.saveMetaFactBinding(new MetaFactBindingEntity()
            .setFactID(referencedFact.getId())
            .setMetaFactID(metaFact.getId())
    );

    return metaFact;
  }

  private Float resolveConfidence(CreateMetaFactRequest request) {
    return ObjectUtils.ifNull(request.getConfidence(), requestedFactType.getDefaultConfidence());
  }

  private void reindexExistingFact(FactEntity fact, List<UUID> acl) {
    reindexExistingFact(fact.getId(), document -> {
      // Only 'lastSeenTimestamp' and potentially the ACL have been changed when refreshing a Fact.
      document.setLastSeenTimestamp(fact.getLastSeenTimestamp());
      document.setAcl(SetUtils.union(document.getAcl(), SetUtils.set(acl)));
      return document;
    });
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
