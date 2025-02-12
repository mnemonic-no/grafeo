package no.mnemonic.services.grafeo.service.implementation.resolvers.request;

import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.service.providers.LockProvider;
import no.mnemonic.services.grafeo.service.validators.Validator;
import no.mnemonic.services.grafeo.service.validators.ValidatorFactory;

import jakarta.inject.Inject;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObjectRequestResolver {

  private static final String LOCK_REGION = ObjectRequestResolver.class.getSimpleName();
  private static final Pattern TYPE_VALUE_PATTERN = Pattern.compile("([^/]+)/(.+)");

  private final ObjectManager objectManager;
  private final ObjectFactDao objectFactDao;
  private final ValidatorFactory validatorFactory;
  private final LockProvider lockProvider;

  @Inject
  public ObjectRequestResolver(ObjectManager objectManager,
                               ObjectFactDao objectFactDao,
                               ValidatorFactory validatorFactory,
                               LockProvider lockProvider) {
    this.objectManager = objectManager;
    this.objectFactDao = objectFactDao;
    this.validatorFactory = validatorFactory;
    this.lockProvider = lockProvider;
  }

  /**
   * Tries to resolve an Object. This is performed in three steps:
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
   * @param object   Either UUID of Object or Object identified by pattern 'type/value'
   * @param property Name of the property in the request containing the Object; used in error messages
   * @return Resolved Object
   * @throws InvalidArgumentException If an existing Object cannot be resolved and creating a new Object fails
   */
  public ObjectRecord resolveObject(String object, String property) throws InvalidArgumentException {
    if (StringUtils.isBlank(object)) return null;

    // If input is a UUID just try to fetch Object by ID.
    if (StringUtils.isUUID(object)) {
      return objectFactDao.getObject(UUID.fromString(object));
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
    ObjectTypeEntity typeEntity = fetchObjectType(type, property);

    // Need to synchronize this block because multiple requests may resolve the same Object. If the Object doesn't exist
    // in the database yet they end up with a race condition trying to create the same Object. The DAO layer doesn't
    // allow that which would fail the request, or worse a duplicated Object could be created.
    try (LockProvider.Lock ignored = lockProvider.acquireLock(LOCK_REGION, object)) {
      // Try to fetch Object by type and value.
      ObjectRecord objectRecord = objectFactDao.getObject(type, value);
      if (objectRecord == null) {
        // Object doesn't exist yet, need to create it.
        objectRecord = createObject(typeEntity, value, property);
      }

      return objectRecord;
    }
  }

  private ObjectTypeEntity fetchObjectType(String type, String property) throws InvalidArgumentException {
    ObjectTypeEntity typeEntity = objectManager.getObjectType(type);
    if (typeEntity == null) {
      throw new InvalidArgumentException()
              .addValidationError("ObjectType does not exist.", "object.type.not.exist", property + ".type", type);
    }
    return typeEntity;
  }

  private ObjectRecord createObject(ObjectTypeEntity type, String value, String property) throws InvalidArgumentException {
    Validator validator = validatorFactory.get(type.getValidator(), type.getValidatorParameter());
    if (!validator.validate(value)) {
      throw new InvalidArgumentException()
              .addValidationError("Object did not pass validation against ObjectType.", "object.not.valid", property + ".value", value);
    }

    ObjectRecord objectRecord = new ObjectRecord()
            .setId(UUID.randomUUID())
            .setTypeID(type.getId())
            .setValue(value);

    return objectFactDao.storeObject(objectRecord);
  }

}
