package no.mnemonic.act.platform.service.ti.helpers;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.request.v1.FactObjectBindingDefinition;
import no.mnemonic.act.platform.api.request.v1.MetaFactBindingDefinition;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.commons.utilities.collections.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Class providing common methods used for handling FactTypes in FactTypeCreateDelegate and FactTypeUpdateDelegate.
 */
public class FactTypeHelper {

  private final FactManager factManager;
  private final ObjectManager objectManager;

  public FactTypeHelper(FactManager factManager, ObjectManager objectManager) {
    this.factManager = factManager;
    this.objectManager = objectManager;
  }

  /**
   * Assert that a FactType does not yet exist (by name).
   *
   * @param name Name of FactType
   * @throws InvalidArgumentException Thrown if FactType already exists
   */
  public void assertFactTypeNotExists(String name) throws InvalidArgumentException {
    if (factManager.getFactType(name) != null) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("FactType with name = %s already exists.", name), "fact.type.exist", "name", name);
    }
  }

  /**
   * Assert that ObjectTypes exist when a binding definition between a FactType and an ObjectType should be created.
   * <p>
   * The operation does nothing if no binding definitions are given (either empty or NULL).
   *
   * @param bindingDefinitions FactType/ObjectType binding definitions
   * @param propertyName       Property name
   * @throws InvalidArgumentException Thrown if an ObjectType part of a binding definition does not exist
   */
  public void assertObjectTypesToBindExist(List<FactObjectBindingDefinition> bindingDefinitions, String propertyName) throws InvalidArgumentException {
    if (CollectionUtils.isEmpty(bindingDefinitions)) return;

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

      if (sourceObjectType != null && objectManager.getObjectType(sourceObjectType) == null) {
        ex.addValidationError(String.format("ObjectType with id = %s does not exist.", sourceObjectType),
                "object.type.not.exist", String.format("%s[%d].sourceObjectType", propertyName, i), sourceObjectType.toString());
      }

      if (destinationObjectType != null && objectManager.getObjectType(destinationObjectType) == null) {
        ex.addValidationError(String.format("ObjectType with id = %s does not exist.", destinationObjectType),
                "object.type.not.exist", String.format("%s[%d].destinationObjectType", propertyName, i), destinationObjectType.toString());
      }
    }

    if (ex.hasErrors()) throw ex; // Fail if any binding definition is invalid.
  }

  /**
   * Assert that FactTypes exist when a binding definition for a meta FactType should be created.
   * <p>
   * The operation does nothing if no binding definitions are given (either empty or NULL).
   *
   * @param bindingDefinitions Meta FactType binding definitions
   * @param propertyName       Property name
   * @throws InvalidArgumentException Thrown if a FactType of a binding definition does not exist
   */
  public void assertFactTypesToBindExist(List<MetaFactBindingDefinition> bindingDefinitions, String propertyName) throws InvalidArgumentException {
    if (CollectionUtils.isEmpty(bindingDefinitions)) return;

    InvalidArgumentException ex = new InvalidArgumentException();

    for (int i = 0; i < bindingDefinitions.size(); i++) {
      UUID factTypeID = bindingDefinitions.get(i).getFactType();
      if (factManager.getFactType(factTypeID) == null) {
        ex.addValidationError(String.format("FactType with id = %s does not exist.", factTypeID),
                "fact.type.not.exist", String.format("%s[%d].factType", propertyName, i), factTypeID.toString());
      }
    }

    if (ex.hasErrors()) throw ex; // Fail if any binding definition is invalid.
  }

  /**
   * Convert FactObjectBindingDefinitions from a request to entities.
   * <p>
   * The operation returns NULL if no binding definitions are given (either empty or NULL).
   *
   * @param bindingDefinitions Definitions as part of a request
   * @return Definitions converted to entities
   */
  public Set<FactTypeEntity.FactObjectBindingDefinition> convertFactObjectBindingDefinitions(List<FactObjectBindingDefinition> bindingDefinitions) {
    if (CollectionUtils.isEmpty(bindingDefinitions)) return null;
    return bindingDefinitions.stream()
            .map(r -> new FactTypeEntity.FactObjectBindingDefinition()
                    .setSourceObjectTypeID(r.getSourceObjectType())
                    .setDestinationObjectTypeID(r.getDestinationObjectType())
                    .setBidirectionalBinding(r.isBidirectionalBinding()))
            .collect(Collectors.toSet());
  }

  /**
   * Convert MetaFactBindingDefinitions from a request to entities.
   * <p>
   * The operation returns NULL if no binding definitions are given (either empty or NULL).
   *
   * @param bindingDefinitions Definitions as part of a request
   * @return Definitions converted to entities
   */
  public Set<FactTypeEntity.MetaFactBindingDefinition> convertMetaFactBindingDefinitions(List<MetaFactBindingDefinition> bindingDefinitions) {
    if (CollectionUtils.isEmpty(bindingDefinitions)) return null;
    return bindingDefinitions.stream()
            .map(r -> new FactTypeEntity.MetaFactBindingDefinition().setFactTypeID(r.getFactType()))
            .collect(Collectors.toSet());
  }

}
