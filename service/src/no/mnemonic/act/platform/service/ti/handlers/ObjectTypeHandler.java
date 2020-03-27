package no.mnemonic.act.platform.service.ti.handlers;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;

import javax.inject.Inject;

public class ObjectTypeHandler {

  private final ObjectManager objectManager;

  @Inject
  public ObjectTypeHandler(ObjectManager objectManager) {
    this.objectManager = objectManager;
  }

  /**
   * Assert that an ObjectType exists (by name).
   *
   * @param name     Name of ObjectType
   * @param property Name of request property
   * @throws InvalidArgumentException Thrown if ObjectType does not exist
   */
  public void assertObjectTypeExists(String name, String property) throws InvalidArgumentException {
    if (objectManager.getObjectType(name) == null) {
      throw new InvalidArgumentException()
        .addValidationError(String.format("ObjectType with name = %s does not exist.", name), "object.type.not.exist", property, name);
    }
  }

  /**
   * Assert that an ObjectType does not yet exist (by name).
   *
   * @param name Name of ObjectType
   * @throws InvalidArgumentException Thrown if ObjectType already exists
   */
  public void assertObjectTypeNotExists(String name) throws InvalidArgumentException {
    if (objectManager.getObjectType(name) != null) {
      throw new InvalidArgumentException()
        .addValidationError(String.format("ObjectType with name = %s already exists.", name), "object.type.exist", "name", name);
    }
  }
}
