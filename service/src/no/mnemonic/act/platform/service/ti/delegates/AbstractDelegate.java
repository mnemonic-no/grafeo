package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.UUID;

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
}
