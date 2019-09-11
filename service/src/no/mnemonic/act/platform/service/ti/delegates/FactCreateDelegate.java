package no.mnemonic.act.platform.service.ti.delegates;

import com.google.common.collect.Streams;
import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.request.v1.CreateFactRequest;
import no.mnemonic.act.platform.dao.api.FactExistenceSearchCriteria;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.SearchResult;
import no.mnemonic.act.platform.service.contexts.TriggerContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.TiServiceEvent;
import no.mnemonic.act.platform.service.ti.helpers.FactCreateHelper;
import no.mnemonic.act.platform.service.ti.helpers.FactStorageHelper;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeResolver;
import no.mnemonic.act.platform.service.ti.helpers.ObjectResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FactCreateDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final TriggerContext triggerContext;
  private final FactManager factManager;
  private final FactSearchManager factSearchManager;
  private final ObjectManager objectManager;
  private final FactTypeResolver factTypeResolver;
  private final ObjectResolver objectResolver;
  private final FactCreateHelper factCreateHelper;
  private final FactStorageHelper factStorageHelper;
  private final Function<FactEntity, Fact> factConverter;

  private FactTypeEntity requestedFactType;
  private OriginEntity requestedOrigin;
  private Organization requestedOrganization;

  @Inject
  public FactCreateDelegate(TiSecurityContext securityContext,
                            TriggerContext triggerContext,
                            FactManager factManager,
                            FactSearchManager factSearchManager,
                            ObjectManager objectManager,
                            FactTypeResolver factTypeResolver,
                            ObjectResolver objectResolver,
                            FactCreateHelper factCreateHelper,
                            FactStorageHelper factStorageHelper,
                            Function<FactEntity, Fact> factConverter) {
    this.securityContext = securityContext;
    this.triggerContext = triggerContext;
    this.factManager = factManager;
    this.factSearchManager = factSearchManager;
    this.objectManager = objectManager;
    this.factTypeResolver = factTypeResolver;
    this.objectResolver = objectResolver;
    this.factCreateHelper = factCreateHelper;
    this.factStorageHelper = factStorageHelper;
    this.factConverter = factConverter;
  }

  public Fact handle(CreateFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    // First resolve some objects which are required later on. This will also validate those request parameters.
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
    assertValidFactObjectBindings(request);

    FactEntity fact = resolveExistingFact(request);
    if (fact != null) {
      // Refresh an existing Fact.
      fact = factManager.refreshFact(fact.getId());
      List<UUID> subjectsAddedToAcl = factStorageHelper.saveAdditionalAclForFact(fact, request.getAcl());
      // Reindex existing Fact in ElasticSearch.
      reindexExistingFact(fact, subjectsAddedToAcl);
    } else {
      // Or create a new Fact.
      fact = saveFact(request);
      List<UUID> subjectsAddedToAcl = factStorageHelper.saveInitialAclForNewFact(fact, request.getAcl());
      // Index new Fact into ElasticSearch.
      indexCreatedFact(fact, requestedFactType, subjectsAddedToAcl);
    }

    // Always add provided comment.
    factStorageHelper.saveCommentForFact(fact, request.getComment());

    // Register TriggerEvent before returning added Fact.
    Fact addedFact = factConverter.apply(fact);
    registerTriggerEvent(addedFact);

    return addedFact;
  }

  private void assertValidFactObjectBindings(CreateFactRequest request) throws InvalidArgumentException {
    // Validate that either source or destination or both are set. One field can be NULL to support bindings of cardinality 1.
    ObjectEntity source = objectResolver.resolveObject(request.getSourceObject());
    ObjectEntity destination = objectResolver.resolveObject(request.getDestinationObject());
    if (source == null && destination == null) {
      throw new InvalidArgumentException()
              .addValidationError("Requested source Object could not be resolved.", "invalid.source.object", "sourceObject", request.getSourceObject())
              .addValidationError("Requested destination Object could not be resolved.", "invalid.destination.object", "destinationObject", request.getDestinationObject());
    }

    // Validate that the binding between source Object, Fact and destination Object is valid according to the FactType.
    // Both source and destination ObjectTypes must be the same plus the bidirectional binding flag must match.
    boolean valid = !CollectionUtils.isEmpty(requestedFactType.getRelevantObjectBindings()) && requestedFactType.getRelevantObjectBindings()
            .stream()
            .anyMatch(b -> Objects.equals(b.getSourceObjectTypeID(), ObjectUtils.ifNotNull(source, ObjectEntity::getTypeID)) &&
                    Objects.equals(b.getDestinationObjectTypeID(), ObjectUtils.ifNotNull(destination, ObjectEntity::getTypeID)) &&
                    b.isBidirectionalBinding() == request.isBidirectionalBinding());
    if (!valid) {
      String invalidValue = String.format("sourceObject = %s|destinationObject = %s|bidirectionalBinding = %s", request.getSourceObject(), request.getDestinationObject(), request.isBidirectionalBinding());
      throw new InvalidArgumentException()
              .addValidationError(String.format("Requested binding between Fact and Object(s) is not allowed for FactType with id = %s.", requestedFactType.getId()),
                      "invalid.fact.object.binding", "sourceObject|destinationObject|bidirectionalBinding", invalidValue);
    }
  }

  private FactEntity resolveExistingFact(CreateFactRequest request) throws InvalidArgumentException {
    FactExistenceSearchCriteria.Builder criteriaBuilder = FactExistenceSearchCriteria.builder()
            .setFactValue(request.getValue())
            .setFactTypeID(requestedFactType.getId())
            .setOriginID(requestedOrigin.getId())
            .setOrganizationID(requestedOrganization.getId())
            .setAccessMode(request.getAccessMode().name())
            .setConfidence(resolveConfidence(request));
    // Need to resolve bindings in order to get the correct objectID if this isn't provided in the request.
    for (FactEntity.FactObjectBinding binding : resolveFactObjectBindings(request)) {
      criteriaBuilder.addObject(binding.getObjectID(), binding.getDirection().name());
    }

    // Try to fetch any existing Facts from ElasticSearch.
    SearchResult<FactDocument> result = factSearchManager.retrieveExistingFacts(criteriaBuilder.build());
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

  private FactEntity saveFact(CreateFactRequest request) throws InvalidArgumentException {
    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())  // Need to provide client-generated ID.
            .setTypeID(requestedFactType.getId())
            .setValue(request.getValue())
            .setAccessMode(AccessMode.valueOf(request.getAccessMode().name()))
            .setOrganizationID(requestedOrganization.getId())
            .setAddedByID(securityContext.getCurrentUserID())
            .setOriginID(requestedOrigin.getId())
            .setTrust(requestedOrigin.getTrust())
            .setConfidence(resolveConfidence(request))
            .setBindings(resolveFactObjectBindings(request))
            .setTimestamp(System.currentTimeMillis())
            .setLastSeenTimestamp(System.currentTimeMillis());

    fact = factManager.saveFact(fact);
    // Save all bindings between Objects and the created Facts.
    for (FactEntity.FactObjectBinding binding : fact.getBindings()) {
      ObjectFactBindingEntity entity = new ObjectFactBindingEntity()
              .setObjectID(binding.getObjectID())
              .setFactID(fact.getId())
              .setDirection(binding.getDirection());
      objectManager.saveObjectFactBinding(entity);
    }

    return fact;
  }

  private Float resolveConfidence(CreateFactRequest request) {
    return ObjectUtils.ifNull(request.getConfidence(), requestedFactType.getDefaultConfidence());
  }

  private List<FactEntity.FactObjectBinding> resolveFactObjectBindings(CreateFactRequest request) throws InvalidArgumentException {
    List<FactEntity.FactObjectBinding> entityBindings = new ArrayList<>();

    ObjectEntity source = objectResolver.resolveObject(request.getSourceObject());
    ObjectEntity destination = objectResolver.resolveObject(request.getDestinationObject());

    if (source != null) {
      FactEntity.FactObjectBinding entity = new FactEntity.FactObjectBinding()
              .setObjectID(source.getId())
              .setDirection(request.isBidirectionalBinding() ? Direction.BiDirectional : Direction.FactIsDestination);
      entityBindings.add(entity);
    }

    if (destination != null) {
      FactEntity.FactObjectBinding entity = new FactEntity.FactObjectBinding()
              .setObjectID(destination.getId())
              .setDirection(request.isBidirectionalBinding() ? Direction.BiDirectional : Direction.FactIsSource);
      entityBindings.add(entity);
    }

    return entityBindings;
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
