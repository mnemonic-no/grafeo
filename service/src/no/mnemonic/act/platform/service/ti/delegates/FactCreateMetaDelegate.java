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
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.MetaFactBindingEntity;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.SearchResult;
import no.mnemonic.act.platform.service.contexts.TriggerContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.TiServiceEvent;
import no.mnemonic.act.platform.service.ti.helpers.FactStorageHelper;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class FactCreateMetaDelegate extends AbstractDelegate {

  private final FactTypeResolver factTypeResolver;
  private final FactStorageHelper factStorageHelper;

  private FactCreateMetaDelegate(FactTypeResolver factTypeResolver, FactStorageHelper factStorageHelper) {
    this.factTypeResolver = factTypeResolver;
    this.factStorageHelper = factStorageHelper;
  }

  public Fact handle(CreateMetaFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch referenced Fact and verify that it exists.
    FactEntity referencedFact = fetchExistingFact(request.getFact());
    // Verify that user is allowed to access the referenced Fact.
    TiSecurityContext.get().checkReadPermission(referencedFact);
    // Verify that user is allowed to add Facts for the requested organization.
    TiSecurityContext.get().checkPermission(TiFunctionConstants.addFactObjects, resolveOrganization(request.getOrganization()));

    FactTypeEntity type = factTypeResolver.resolveFactType(request.getType());
    if (Objects.equals(type.getId(), factTypeResolver.resolveRetractionFactType().getId())) {
      throw new AccessDeniedException("Not allowed to manually use system-defined Retraction FactType. Use /retract endpoint instead.");
    }

    // Validate that requested Fact matches its FactType.
    assertValidFactValue(type, request.getValue());
    assertValidFactBinding(request, type, referencedFact);

    FactEntity metaFact = resolveExistingFact(request, type, referencedFact);
    if (metaFact != null) {
      // Refresh an existing Fact.
      metaFact = TiRequestContext.get().getFactManager().refreshFact(metaFact.getId());
      List<UUID> subjectsAddedToAcl = factStorageHelper.saveAdditionalAclForFact(metaFact, request.getAcl());
      // Reindex existing Fact in ElasticSearch.
      reindexExistingFact(metaFact, subjectsAddedToAcl);
    } else {
      // Or create a new Fact.
      metaFact = saveFact(request, type, referencedFact);
      List<UUID> subjectsAddedToAcl = factStorageHelper.saveInitialAclForNewFact(metaFact, request.getAcl());
      // Index new Fact into ElasticSearch.
      indexCreatedFact(metaFact, type, subjectsAddedToAcl);
    }

    // Always add provided comment.
    factStorageHelper.saveCommentForFact(metaFact, request.getComment());

    // Register TriggerEvent before returning added Fact.
    Fact addedFact = TiRequestContext.get().getFactConverter().apply(metaFact);
    registerTriggerEvent(addedFact);

    return addedFact;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private FactTypeResolver factTypeResolver;
    private FactStorageHelper factStorageHelper;

    private Builder() {
    }

    public FactCreateMetaDelegate build() {
      ObjectUtils.notNull(factTypeResolver, "Cannot instantiate FactCreateMetaDelegate without 'factTypeResolver'.");
      ObjectUtils.notNull(factStorageHelper, "Cannot instantiate FactCreateMetaDelegate without 'factStorageHelper'.");
      return new FactCreateMetaDelegate(factTypeResolver, factStorageHelper);
    }

    public Builder setFactTypeResolver(FactTypeResolver factTypeResolver) {
      this.factTypeResolver = factTypeResolver;
      return this;
    }

    public Builder setFactStorageHelper(FactStorageHelper factStorageHelper) {
      this.factStorageHelper = factStorageHelper;
      return this;
    }
  }

  private void assertValidFactBinding(CreateMetaFactRequest request, FactTypeEntity type, FactEntity referencedFact) throws InvalidArgumentException {
    // Validate that the referenced Fact has the correct type according to the requested FactType.
    boolean valid = !CollectionUtils.isEmpty(type.getRelevantFactBindings()) && type.getRelevantFactBindings()
            .stream()
            .anyMatch(b -> Objects.equals(b.getFactTypeID(), referencedFact.getTypeID()));
    if (!valid) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("Requested binding between Facts is not allowed for FactType with id = %s.", type.getId()),
                      "invalid.meta.fact.binding", "type", request.getType());
    }
  }

  private FactEntity resolveExistingFact(CreateMetaFactRequest request, FactTypeEntity type, FactEntity referencedFact) throws InvalidArgumentException {
    // Skip confidenceLevel for now as it's currently not provided in the request.
    FactExistenceSearchCriteria criteria = FactExistenceSearchCriteria.builder()
            .setFactValue(request.getValue())
            .setFactTypeID(type.getId())
            .setSourceID(resolveSource(request.getSource()))
            .setOrganizationID(resolveOrganization(request.getOrganization()))
            .setAccessMode(resolveAccessMode(referencedFact, request.getAccessMode()).name())
            .setInReferenceTo(referencedFact.getId())
            .build();

    // Try to fetch any existing Facts from ElasticSearch.
    SearchResult<FactDocument> result = TiRequestContext.get().getFactSearchManager().retrieveExistingFacts(criteria);
    if (result.getCount() <= 0) {
      return null; // No results, need to create new Fact.
    }

    // Fetch the authorative data from Cassandra, apply permission check and return existing Fact if accessible.
    List<UUID> factID = result.getValues().stream().map(FactDocument::getId).collect(Collectors.toList());
    return Streams.stream(TiRequestContext.get().getFactManager().getFacts(factID))
            .filter(fact -> TiSecurityContext.get().hasReadPermission(fact))
            .findFirst()
            .orElse(null);
  }

  private FactEntity saveFact(CreateMetaFactRequest request, FactTypeEntity type, FactEntity referencedFact) throws InvalidArgumentException {
    FactEntity metaFact = new FactEntity()
            .setId(UUID.randomUUID()) // Need to provide client-generated ID.
            .setTypeID(type.getId())
            .setValue(request.getValue())
            .setInReferenceToID(referencedFact.getId())
            .setOrganizationID(resolveOrganization(request.getOrganization()))
            .setSourceID(resolveSource(request.getSource()))
            .setAccessMode(resolveAccessMode(referencedFact, request.getAccessMode()))
            .setTimestamp(System.currentTimeMillis())
            .setLastSeenTimestamp(System.currentTimeMillis());

    metaFact = TiRequestContext.get().getFactManager().saveFact(metaFact);
    // Also save binding between referenced Fact and new meta Fact.
    TiRequestContext.get().getFactManager().saveMetaFactBinding(new MetaFactBindingEntity()
            .setFactID(referencedFact.getId())
            .setMetaFactID(metaFact.getId())
    );

    return metaFact;
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
    TriggerContext.get().registerTriggerEvent(event);
  }

}
