package no.mnemonic.act.platform.service.ti.helpers;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.exceptions.ImmutableViolationException;
import no.mnemonic.act.platform.entity.cassandra.ObjectEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import no.mnemonic.act.platform.service.validators.Validator;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;

import java.util.UUID;

public class ObjectResolver {

  private final ObjectManager objectManager;
  private final ValidatorFactory validatorFactory;

  public ObjectResolver(ObjectManager objectManager, ValidatorFactory validatorFactory) {
    this.objectManager = objectManager;
    this.validatorFactory = validatorFactory;
  }

  /**
   * Tries to resolve an ObjectEntity. This is performed in three steps:
   * <p>
   * 1. Try to resolve Object by ID.
   * 2. If the first step fails, try to resolve Object by type and value.
   * 3. If the second step fails, a new Object will be created with respect to 'objectType' and 'objectValue'.
   * <p>
   * Creating a new Object will fail if either the requested ObjectType does not exist or the provided Object value
   * does not pass validation against the ObjectType.
   * <p>
   * Either 'objectID' or 'objectType' plus 'objectValue' must be provided.
   *
   * @param objectID    UUID of Object
   * @param objectType  Type of Object
   * @param objectValue Value of Object
   * @return Resolved ObjectEntity
   * @throws InvalidArgumentException If an existing Object cannot be resolved and creating a new Object fails
   */
  public ObjectEntity resolveObject(UUID objectID, String objectType, String objectValue) throws InvalidArgumentException {
    ObjectEntity objectEntity = objectManager.getObject(objectID);
    if (objectEntity == null) {
      // Can't fetch Object by ID, try to fetch it by type and value.
      objectEntity = objectManager.getObject(objectType, objectValue);
      if (objectEntity == null) {
        // Object doesn't exist yet, need to create it.
        objectEntity = createObject(objectType, objectValue);
      }
    }

    return objectEntity;
  }

  private ObjectEntity createObject(String type, String value) throws InvalidArgumentException {
    ObjectTypeEntity typeEntity = objectManager.getObjectType(type);
    if (typeEntity == null) {
      throw new InvalidArgumentException()
              .addValidationError("ObjectType does not exist.", "object.type.not.exist", "objectType", type);
    }

    Validator validator = validatorFactory.get(typeEntity.getValidator(), typeEntity.getValidatorParameter());
    if (!validator.validate(value)) {
      throw new InvalidArgumentException()
              .addValidationError("Object did not pass validation against ObjectType.", "object.not.valid", "objectValue", value);
    }

    ObjectEntity objectEntity = new ObjectEntity()
            .setId(UUID.randomUUID()) // Need to provide client-generated ID.
            .setTypeID(typeEntity.getId())
            .setValue(value);

    try {
      objectEntity = objectManager.saveObject(objectEntity);
    } catch (ImmutableViolationException ex) {
      // This should usually not happen as any existing Object should be fetched before attempting to create a new one.
      throw new RuntimeException(ex);
    }

    return objectEntity;
  }

}
