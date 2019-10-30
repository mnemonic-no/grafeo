package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.validators.Validator;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.UUID;
import java.util.function.Function;

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

}
