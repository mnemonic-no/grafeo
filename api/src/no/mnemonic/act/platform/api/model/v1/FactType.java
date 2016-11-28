package no.mnemonic.act.platform.api.model.v1;

import no.mnemonic.commons.utilities.collections.ListUtils;

import java.util.List;
import java.util.UUID;

public class FactType {

  private final UUID id;
  private final Namespace namespace;
  private final String name;
  private final String validator;
  private final String validatorParameter;
  private final String entityHandler;
  private final String entityHandlerParameter;
  private final List<FactObjectBindingDefinition> relevantObjectBindings;

  private FactType(UUID id, Namespace namespace, String name, String validator, String validatorParameter,
                   String entityHandler, String entityHandlerParameter, List<FactObjectBindingDefinition> relevantObjectBindings) {
    this.id = id;
    this.namespace = namespace;
    this.name = name;
    this.validator = validator;
    this.validatorParameter = validatorParameter;
    this.entityHandler = entityHandler;
    this.entityHandlerParameter = entityHandlerParameter;
    this.relevantObjectBindings = relevantObjectBindings;
  }

  public UUID getId() {
    return id;
  }

  public Namespace getNamespace() {
    return namespace;
  }

  public String getName() {
    return name;
  }

  public String getValidator() {
    return validator;
  }

  public String getValidatorParameter() {
    return validatorParameter;
  }

  public String getEntityHandler() {
    return entityHandler;
  }

  public String getEntityHandlerParameter() {
    return entityHandlerParameter;
  }

  public List<FactObjectBindingDefinition> getRelevantObjectBindings() {
    return relevantObjectBindings;
  }

  public Info toInfo() {
    return new Info(id, name);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id;
    private Namespace namespace;
    private String name;
    private String validator;
    private String validatorParameter;
    private String entityHandler;
    private String entityHandlerParameter;
    private List<FactObjectBindingDefinition> relevantObjectBindings;

    private Builder() {
    }

    public FactType build() {
      return new FactType(id, namespace, name, validator, validatorParameter, entityHandler, entityHandlerParameter, relevantObjectBindings);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setNamespace(Namespace namespace) {
      this.namespace = namespace;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setValidator(String validator) {
      this.validator = validator;
      return this;
    }

    public Builder setValidatorParameter(String validatorParameter) {
      this.validatorParameter = validatorParameter;
      return this;
    }

    public Builder setEntityHandler(String entityHandler) {
      this.entityHandler = entityHandler;
      return this;
    }

    public Builder setEntityHandlerParameter(String entityHandlerParameter) {
      this.entityHandlerParameter = entityHandlerParameter;
      return this;
    }

    public Builder setRelevantObjectBindings(List<FactObjectBindingDefinition> relevantObjectBindings) {
      this.relevantObjectBindings = relevantObjectBindings;
      return this;
    }

    public Builder addRelevantObjectBinding(FactObjectBindingDefinition relevantObjectBinding) {
      this.relevantObjectBindings = ListUtils.addToList(this.relevantObjectBindings, relevantObjectBinding);
      return this;
    }
  }

  public static class FactObjectBindingDefinition {
    private final ObjectType.Info objectType;
    private final Direction direction;

    public FactObjectBindingDefinition(ObjectType.Info objectType, Direction direction) {
      this.objectType = objectType;
      this.direction = direction;
    }

    public ObjectType.Info getObjectType() {
      return objectType;
    }

    public Direction getDirection() {
      return direction;
    }
  }

  public class Info {
    private final UUID id;
    private final String name;

    private Info(UUID id, String name) {
      this.id = id;
      this.name = name;
    }

    public UUID getId() {
      return id;
    }

    public String getName() {
      return name;
    }
  }

}
