package no.mnemonic.act.platform.service.ti.delegates;

import com.google.common.collect.Streams;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.dao.elastic.document.ScrollingSearchResult;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.validators.Validator;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.MapUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;

/**
 * The AbstractDelegate provides common methods used by multiple delegates.
 */
abstract class AbstractDelegate {

  private static final int MAXIMUM_SEARCH_LIMIT = 10_000;
  private static final Map<AccessMode, Integer> ACCESS_MODE_ORDER = MapUtils.map(
          T(AccessMode.Public, 0),
          T(AccessMode.RoleBased, 1),
          T(AccessMode.Explicit, 2)
  );

  /**
   * Fetch an existing FactType by ID.
   *
   * @param id UUID of FactType
   * @return Existing FactType
   * @throws ObjectNotFoundException Thrown if FactType cannot be found
   */
  FactTypeEntity fetchExistingFactType(UUID id) throws ObjectNotFoundException {
    FactTypeEntity entity = TiRequestContext.get().getFactManager().getFactType(id);
    if (entity == null) {
      throw new ObjectNotFoundException(String.format("FactType with id = %s does not exist.", id),
              "fact.type.not.exist", "id", ObjectUtils.ifNotNull(id, Object::toString, "NULL"));
    }
    return entity;
  }

  /**
   * Fetch an existing ObjectType by ID.
   *
   * @param id UUID of ObjectType
   * @return Existing ObjectType
   * @throws ObjectNotFoundException Thrown if ObjectType cannot be found
   */
  ObjectTypeEntity fetchExistingObjectType(UUID id) throws ObjectNotFoundException {
    ObjectTypeEntity entity = TiRequestContext.get().getObjectManager().getObjectType(id);
    if (entity == null) {
      throw new ObjectNotFoundException(String.format("ObjectType with id = %s does not exist.", id),
              "object.type.not.exist", "id", ObjectUtils.ifNotNull(id, Object::toString, "NULL"));
    }
    return entity;
  }

  /**
   * Fetch an existing Fact by ID.
   *
   * @param id UUID of Fact
   * @return Existing Fact
   * @throws ObjectNotFoundException Thrown if Fact cannot be found
   */
  FactEntity fetchExistingFact(UUID id) throws ObjectNotFoundException {
    FactEntity entity = TiRequestContext.get().getFactManager().getFact(id);
    if (entity == null) {
      throw new ObjectNotFoundException(String.format("Fact with id = %s does not exist.", id),
              "fact.not.exist", "id", ObjectUtils.ifNotNull(id, Object::toString, "NULL"));
    }
    return entity;
  }

  /**
   * Assert that an ObjectType does not yet exist (by name).
   *
   * @param name Name of ObjectType
   * @throws InvalidArgumentException Thrown if ObjectType already exists
   */
  void assertObjectTypeNotExists(String name) throws InvalidArgumentException {
    if (TiRequestContext.get().getObjectManager().getObjectType(name) != null) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("ObjectType with name = %s already exists.", name), "object.type.exist", "name", name);
    }
  }

  /**
   * Assert that an ObjectType exists (by name).
   *
   * @param name     Name of ObjectType
   * @param property Name of request property
   * @throws InvalidArgumentException Thrown if ObjectType does not exist
   */
  void assertObjectTypeExists(String name, String property) throws InvalidArgumentException {
    if (TiRequestContext.get().getObjectManager().getObjectType(name) == null) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("ObjectType with name = %s does not exist.", name), "object.type.not.exist", property, name);
    }
  }

  /**
   * Assert that a Validator exists.
   *
   * @param validator          Name of Validator
   * @param validatorParameter Parameter of Validator
   * @throws InvalidArgumentException Thrown if Validator does not exist
   */
  void assertValidatorExists(String validator, String validatorParameter) throws InvalidArgumentException {
    try {
      TiRequestContext.get().getValidatorFactory().get(validator, validatorParameter);
    } catch (IllegalArgumentException ex) {
      // An IllegalArgumentException will be thrown if a Validator cannot be found.
      throw new InvalidArgumentException()
              .addValidationError(ex.getMessage(), "validator.not.exist", "validator", validator);
    }
  }

  /**
   * Assert that a Fact value is valid according to a FactType's validator.
   *
   * @param type  FactType to validate against
   * @param value Value to validate
   * @throws InvalidArgumentException Thrown if value is not valid for the given FactType
   */
  void assertValidFactValue(FactTypeEntity type, String value) throws InvalidArgumentException {
    Validator validator = TiRequestContext.get().getValidatorFactory().get(type.getValidator(), type.getValidatorParameter());
    if (!validator.validate(value)) {
      throw new InvalidArgumentException()
              .addValidationError("Fact did not pass validation against FactType.", "fact.not.valid", "value", value);
    }
  }

  /**
   * Resolve an Organization by its ID. Falls back to the current user's Organization if no 'organizationID' is provided.
   *
   * @param organizationID ID of Organization
   * @return Resolved Organization (currently just its ID, but this might change)
   */
  UUID resolveOrganization(UUID organizationID) {
    // TODO: Verify organization.
    // If no organization is provided use the current user's organization by default.
    return ObjectUtils.ifNull(organizationID, SecurityContext.get().getCurrentUserOrganizationID());
  }

  /**
   * Resolve a Source by its ID. Falls back to the current user if no 'sourceID' is provided.
   *
   * @param sourceID ID of Source
   * @return Resolved Source (currently just its ID, but this might change)
   */
  UUID resolveSource(UUID sourceID) {
    // TODO: Verify source.
    // If no source is provided use the current user as source by default.
    return ObjectUtils.ifNull(sourceID, SecurityContext.get().getCurrentUserID());
  }

  /**
   * Resolve AccessMode from a request and verify that it's not less restrictive than the AccessMode from another Fact.
   * Falls back to AccessMode from referenced Fact if requested AccessMode is not given.
   *
   * @param referencedFact      Fact to validate AccessMode against
   * @param requestedAccessMode Requested AccessMode (might be NULL)
   * @return Resolved AccessMode
   * @throws InvalidArgumentException Thrown if requested AccessMode is less restrictive than AccessMode of referenced Fact
   */
  AccessMode resolveAccessMode(FactEntity referencedFact, no.mnemonic.act.platform.api.request.v1.AccessMode requestedAccessMode)
          throws InvalidArgumentException {
    // If no AccessMode provided fall back to the AccessMode from the referenced Fact.
    AccessMode mode = ObjectUtils.ifNotNull(requestedAccessMode, m -> AccessMode.valueOf(m.name()), referencedFact.getAccessMode());

    // The requested AccessMode of a new Fact should not be less restrictive than the AccessMode of the referenced Fact.
    if (ACCESS_MODE_ORDER.get(mode) < ACCESS_MODE_ORDER.get(referencedFact.getAccessMode())) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("Requested AccessMode cannot be less restrictive than AccessMode of Fact with id = %s.", referencedFact.getId()),
                      "access.mode.too.wide", "accessMode", mode.name());
    }

    return mode;
  }

  /**
   * Index a newly created Fact into ElasticSearch. Only call this method after a Fact and its related data were
   * persisted to Cassandra.
   *
   * @param fact     Fact to index
   * @param factType FactType of Fact to index
   * @param acl      Full access control list of Fact to index (list of Subject IDs)
   */
  void indexCreatedFact(FactEntity fact, FactTypeEntity factType, List<UUID> acl) {
    // TODO: Resolve and index organizationName and sourceName.
    FactDocument document = new FactDocument()
            .setId(fact.getId())
            .setRetracted(false) // A newly created Fact isn't retracted by definition.
            .setTypeID(factType.getId())
            .setTypeName(factType.getName())
            .setValue(fact.getValue())
            .setInReferenceTo(fact.getInReferenceToID())
            .setOrganizationID(fact.getOrganizationID())
            .setSourceID(fact.getSourceID())
            .setAccessMode(FactDocument.AccessMode.valueOf(fact.getAccessMode().name()))
            .setTimestamp(fact.getTimestamp())
            .setLastSeenTimestamp(fact.getLastSeenTimestamp())
            .setAcl(SetUtils.set(acl));

    for (FactEntity.FactObjectBinding objectBinding : SetUtils.set(fact.getBindings())) {
      ObjectEntity object = TiRequestContext.get().getObjectManager().getObject(objectBinding.getObjectID());
      ObjectTypeEntity objectType = TiRequestContext.get().getObjectManager().getObjectType(object.getTypeID());
      document.addObject(new ObjectDocument()
              .setId(object.getId())
              .setTypeID(objectType.getId())
              .setTypeName(objectType.getName())
              .setValue(object.getValue())
              .setDirection(ObjectDocument.Direction.valueOf(objectBinding.getDirection().name()))
      );
    }

    TiRequestContext.get().getFactSearchManager().indexFact(document);
  }

  /**
   * Re-index an already existing Fact in ElasticSearch. If Fact is not found in ElasticSearch the Fact won't be indexed.
   *
   * @param factID          UUID of Fact to re-index
   * @param documentUpdater Callback for updating the indexed document before re-indexing
   */
  void reindexExistingFact(UUID factID, Function<FactDocument, FactDocument> documentUpdater) {
    FactDocument document = TiRequestContext.get().getFactSearchManager().getFact(factID);
    // 'document' should usually not be NULL. In this case skip updating Fact because it isn't indexed.
    ObjectUtils.ifNotNullDo(document, d -> TiRequestContext.get().getFactSearchManager().indexFact(documentUpdater.apply(d)));
  }

  /**
   * Search for Facts based on a given FactSearchCriteria. It searches for Facts in ElasticSearch, fetches the authoritative
   * data from Cassandra, and makes sure that only Facts the user has access to are returned.
   *
   * @param criteria Criteria to search for Facts
   * @return Facts wrapped inside a ResultSet
   */
  ResultSet<Fact> searchForFacts(FactSearchCriteria criteria) {
    // Restrict the number of results returned from ElasticSearch. Right now everything above MAXIMUM_SEARCH_LIMIT will
    // put too much load onto the application and will be very slow. Implements same logic as previously done in ElasticSearch.
    int limit = criteria.getLimit() > 0 && criteria.getLimit() < MAXIMUM_SEARCH_LIMIT ? criteria.getLimit() : MAXIMUM_SEARCH_LIMIT;

    // Search for Facts in ElasticSearch and pick out all Fact IDs.
    ScrollingSearchResult<FactDocument> searchResult = TiRequestContext.get().getFactSearchManager().searchFacts(criteria);
    List<UUID> factID = Streams.stream(searchResult)
            .map(FactDocument::getId)
            .limit(limit)
            .collect(Collectors.toList());

    // Use the Fact IDs to look up the authoritative data in Cassandra,
    // and make sure that a user has access to all returned Facts.
    List<Fact> facts = Streams.stream(TiRequestContext.get().getFactManager().getFacts(factID))
            .filter(fact -> TiSecurityContext.get().hasReadPermission(fact))
            .map(TiRequestContext.get().getFactConverter())
            .collect(Collectors.toList());

    return ResultSet.<Fact>builder()
            .setCount(searchResult.getCount())
            .setLimit(limit)
            .setValues(facts)
            .build();
  }

}
