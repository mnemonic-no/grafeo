package no.mnemonic.act.platform.service.ti.delegates;

import com.google.common.collect.Streams;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.FactObjectBindingDefinition;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.dao.elastic.document.SearchResult;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The AbstractDelegate provides common methods used by multiple delegates.
 */
abstract class AbstractDelegate {

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
   * Assert that a FactType does not yet exist (by name).
   *
   * @param name Name of FactType
   * @throws InvalidArgumentException Thrown if FactType already exists
   */
  void assertFactTypeNotExists(String name) throws InvalidArgumentException {
    if (TiRequestContext.get().getFactManager().getFactType(name) != null) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("FactType with name = %s already exists.", name), "fact.type.exist", "name", name);
    }
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
   * Assert that ObjectTypes exist when a binding definition between a FactType and an ObjectType should be created.
   *
   * @param bindingDefinitions FactType/ObjectType binding definitions
   * @param propertyName       Property name
   * @throws InvalidArgumentException Thrown if an ObjectType part of a binding definition does not exist
   */
  void assertObjectTypesToBindExist(List<FactObjectBindingDefinition> bindingDefinitions, String propertyName) throws InvalidArgumentException {
    InvalidArgumentException ex = new InvalidArgumentException();

    for (int i = 0; i < bindingDefinitions.size(); i++) {
      UUID sourceObjectType = bindingDefinitions.get(i).getSourceObjectType();
      UUID destinationObjectType = bindingDefinitions.get(i).getDestinationObjectType();

      // At least one of 'sourceObjectType' or 'destinationObjectType' must be specified. If only one is specified
      // the binding definition is of cardinality 1. If both are specified it's of cardinality 2.
      if (sourceObjectType == null && destinationObjectType == null) {
        ex.addValidationError("Object binding definition must specify at least one of 'sourceObjectType' or 'destinationObjectType'.",
                "invalid.object.binding.definition", String.format("%s[%d]", propertyName, i), "NULL");
      }

      if (sourceObjectType != null && TiRequestContext.get().getObjectManager().getObjectType(sourceObjectType) == null) {
        ex.addValidationError(String.format("ObjectType with id = %s does not exist.", sourceObjectType),
                "object.type.not.exist", String.format("%s[%d].sourceObjectType", propertyName, i), sourceObjectType.toString());
      }

      if (destinationObjectType != null && TiRequestContext.get().getObjectManager().getObjectType(destinationObjectType) == null) {
        ex.addValidationError(String.format("ObjectType with id = %s does not exist.", destinationObjectType),
                "object.type.not.exist", String.format("%s[%d].destinationObjectType", propertyName, i), destinationObjectType.toString());
      }
    }

    if (ex.hasErrors()) throw ex; // Fail if any binding definition is invalid.
  }

  /**
   * Assert that an EntityHandler exists.
   *
   * @param entityHandler          Name of EntityHandler
   * @param entityHandlerParameter Parameter of EntityHandler
   * @throws InvalidArgumentException Thrown if EntityHandler does not exist
   */
  void assertEntityHandlerExists(String entityHandler, String entityHandlerParameter) throws InvalidArgumentException {
    try {
      TiRequestContext.get().getEntityHandlerFactory().get(entityHandler, entityHandlerParameter);
    } catch (IllegalArgumentException ex) {
      // An IllegalArgumentException will be thrown if an EntityHandler cannot be found.
      throw new InvalidArgumentException()
              .addValidationError(ex.getMessage(), "entity.handler.not.exist", "entityHandler", entityHandler);
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
   * Convert FactObjectBindingDefinitions from a request to entities.
   *
   * @param bindingDefinitions Definitions as part of a request
   * @return Definitions converted to entities
   */
  List<FactTypeEntity.FactObjectBindingDefinition> convertFactObjectBindingDefinitions(List<FactObjectBindingDefinition> bindingDefinitions) {
    return bindingDefinitions.stream()
            .map(r -> new FactTypeEntity.FactObjectBindingDefinition()
                    .setSourceObjectTypeID(r.getSourceObjectType())
                    .setDestinationObjectTypeID(r.getDestinationObjectType())
                    .setBidirectionalBinding(r.isBidirectionalBinding()))
            .collect(Collectors.toList());
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

    for (FactEntity.FactObjectBinding objectBinding : fact.getBindings()) {
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
    // Search for Facts in ElasticSearch and pick out all Fact IDs.
    SearchResult<FactDocument> searchResult = TiRequestContext.get().getFactSearchManager().searchFacts(criteria);
    List<UUID> factID = searchResult.getValues()
            .stream()
            .map(FactDocument::getId)
            .collect(Collectors.toList());

    // Use the Fact IDs to look up the authoritative data in Cassandra,
    // and make sure that a user has access to all returned Facts.
    List<Fact> facts = Streams.stream(TiRequestContext.get().getFactManager().getFacts(factID))
            .filter(fact -> TiSecurityContext.get().hasReadPermission(fact))
            .map(TiRequestContext.get().getFactConverter())
            .collect(Collectors.toList());

    return ResultSet.<Fact>builder()
            .setCount(searchResult.getCount())
            .setLimit(searchResult.getLimit())
            .setValues(facts)
            .build();
  }

}
