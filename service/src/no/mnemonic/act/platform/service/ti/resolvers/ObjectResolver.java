package no.mnemonic.act.platform.service.ti.resolvers;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.validators.Validator;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import no.mnemonic.commons.utilities.StringUtils;

import javax.inject.Inject;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObjectResolver {

  private static final Pattern TYPE_VALUE_PATTERN = Pattern.compile("([^/]+)/(.+)");

  private final ObjectManager objectManager;
  private final ValidatorFactory validatorFactory;

  @Inject
  public ObjectResolver(ObjectManager objectManager, ValidatorFactory validatorFactory) {
    this.objectManager = objectManager;
    this.validatorFactory = validatorFactory;
  }

  /**
   * Tries to resolve an ObjectEntity. This is performed in three steps:
   * <p>
   * 1. Try to resolve Object by ID if input represents a UUID.
   * 2. If input is not a UUID, try to resolve Object by type and value. Input should be of form 'type/value'.
   * 3. If Object doesn't exist yet, a new Object will be created with respect to type and value.
   * <p>
   * If input neither represents a UUID nor is of form 'type/value' NULL will be returned.
   * <p>
   * Creating a new Object will fail if either the requested ObjectType does not exist or the provided Object value
   * does not pass validation against the ObjectType.
   *
   * @param object Either UUID of Object or Object identified by pattern 'type/value'
   * @return Resolved ObjectEntity
   * @throws InvalidArgumentException If an existing Object cannot be resolved and creating a new Object fails
   */
  public ObjectEntity resolveObject(String object) throws InvalidArgumentException {
    if (StringUtils.isBlank(object)) return null;

    // If input is a UUID just try to fetch Object by ID.
    if (StringUtils.isUUID(object)) {
      return objectManager.getObject(UUID.fromString(object));
    }

    // Otherwise try to fetch Object by type and value.
    Matcher matcher = TYPE_VALUE_PATTERN.matcher(object);
    if (!matcher.matches()) {
      // Input doesn't conform to 'type/value' pattern. Can't fetch Object by type and value.
      return null;
    }

    // Extract type and value from input.
    String type = matcher.group(1);
    String value = matcher.group(2);

    // Fetch ObjectType first and validate that it exists (otherwise getObject(type, value) will thrown an IllegalArgumentException).
    ObjectTypeEntity typeEntity = fetchObjectType(type);

    // Try to fetch Object by type and value.
    ObjectEntity objectEntity = objectManager.getObject(type, value);
    if (objectEntity == null) {
      // Object doesn't exist yet, need to create it.
      objectEntity = createObject(typeEntity, value);
    }

    return objectEntity;
  }

  private ObjectTypeEntity fetchObjectType(String type) throws InvalidArgumentException {
    ObjectTypeEntity typeEntity = objectManager.getObjectType(type);
    if (typeEntity == null) {
      throw new InvalidArgumentException()
              .addValidationError("ObjectType does not exist.", "object.type.not.exist", "objectType", type);
    }
    return typeEntity;
  }

  private ObjectEntity createObject(ObjectTypeEntity type, String value) throws InvalidArgumentException {
    Validator validator = validatorFactory.get(type.getValidator(), type.getValidatorParameter());
    if (!validator.validate(value)) {
      throw new InvalidArgumentException()
              .addValidationError("Object did not pass validation against ObjectType.", "object.not.valid", "objectValue", value);
    }

    ObjectEntity objectEntity = new ObjectEntity()
            .setId(UUID.randomUUID()) // Need to provide client-generated ID.
            .setTypeID(type.getId())
            .setValue(value);

    return objectManager.saveObject(objectEntity);
  }

}
