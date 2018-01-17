package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.FactObjectBindingDefinition;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
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
  void assertObjectTypesToBindExist(Collection<FactObjectBindingDefinition> bindingDefinitions, String propertyName) throws InvalidArgumentException {
    boolean invalid = false;
    InvalidArgumentException ex = new InvalidArgumentException();

    for (FactObjectBindingDefinition definition : bindingDefinitions) {
      if (TiRequestContext.get().getObjectManager().getObjectType(definition.getObjectType()) == null) {
        ex.addValidationError(String.format("ObjectType with id = %s does not exist.", definition.getObjectType()),
                "object.type.not.exist", propertyName, definition.getObjectType().toString());
        invalid = true;
      }
    }

    if (invalid) {
      throw ex;
    }
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
                    .setObjectTypeID(r.getObjectType())
                    .setDirection(Direction.valueOf(r.getDirection().name())))
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
   * Resolve Facts bound to one Object (identified by id). It will only return the Facts the current user has access to.
   *
   * @param objectID ID of Object
   * @return Resolved Facts bound to one Object
   */
  List<FactEntity> resolveFactsForObject(UUID objectID) {
    // Fetch Facts bound to Object, but only keep those the current user has access to.
    return TiRequestContext.get().getObjectManager().fetchObjectFactBindings(objectID)
            .stream()
            .map(binding -> TiRequestContext.get().getFactManager().getFact(binding.getFactID()))
            .filter(fact -> TiSecurityContext.get().hasReadPermission(fact))
            .collect(Collectors.toList());
  }

  /**
   * Verify that the current user has access to an Object. The user must have access to at least one Fact bound to the Object.
   *
   * @param object Object to verify access.
   * @return Facts bound to the Object which are accessible to the current user.
   * @throws AccessDeniedException Thrown if the current user does not have access to the Object.
   */
  List<FactEntity> checkObjectAccess(ObjectEntity object) throws AccessDeniedException {
    if (object == null) {
      // User should not get a different response if an Object is not in the system or if user does not have access to it.
      throw new AccessDeniedException("No access to Object.");
    }

    List<FactEntity> facts = resolveFactsForObject(object.getId());
    if (CollectionUtils.isEmpty(facts)) {
      // User does not have access to any Facts bound to this Object.
      throw new AccessDeniedException("No access to Object.");
    }

    return facts;
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
   * Filter Facts by FactType.
   *
   * @param types FactType names to filter by.
   * @return Predicate which returns true if a Fact matches a FactType.
   */
  Predicate<FactEntity> factTypeFilter(Set<String> types) {
    return fact -> {
      if (CollectionUtils.isEmpty(types)) {
        return true;
      }

      FactTypeEntity factType = TiRequestContext.get().getFactManager().getFactType(fact.getTypeID());
      return types.contains(factType.getName());
    };
  }

  /**
   * Filter Facts by Fact value.
   *
   * @param values Values to filter by.
   * @return Predicate which returns true if a Fact matches a value.
   */
  Predicate<FactEntity> factValueFilter(Set<String> values) {
    return fact -> CollectionUtils.isEmpty(values) || values.contains(fact.getValue());
  }

  /**
   * Filter Facts by Source.
   *
   * @param sources Source UUIDs to filter by.
   * @return Predicate which returns true if a Fact matches a Source.
   */
  Predicate<FactEntity> sourceFilter(Set<String> sources) {
    // TODO: For now match on UUID, but it should match on name once Source is properly implemented.
    return fact -> CollectionUtils.isEmpty(sources) || sources.contains(fact.getSourceID().toString());
  }

  /**
   * Filter Facts which were created before a specific timestamp.
   *
   * @param before Timestamp
   * @return Predicate which returns true if a Fact was created before the given timestamp.
   */
  Predicate<FactEntity> beforeFilter(Long before) {
    return fact -> before == null || fact.getTimestamp() < before;
  }

  /**
   * Filter Facts which were created after a specific timestamp.
   *
   * @param after Timestamp
   * @return Predicate which returns true if a Fact was created after the given timestamp.
   */
  Predicate<FactEntity> afterFilter(Long after) {
    return fact -> after == null || fact.getTimestamp() > after;
  }

}
