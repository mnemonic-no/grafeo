package no.mnemonic.act.platform.api.model.v1;

import java.util.UUID;

public class ObjectType {

  private final UUID id;
  private final Namespace namespace;
  private final String name;
  private final String validator;
  private final String validatorParameter;
  private final String entityHandler;
  private final String entityHandlerParameter;

  private ObjectType(UUID id, Namespace namespace, String name, String validator, String validatorParameter, String entityHandler, String entityHandlerParameter) {
    this.id = id;
    this.namespace = namespace;
    this.name = name;
    this.validator = validator;
    this.validatorParameter = validatorParameter;
    this.entityHandler = entityHandler;
    this.entityHandlerParameter = entityHandlerParameter;
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

    private Builder() {
    }

    public ObjectType build() {
      return new ObjectType(id, namespace, name, validator, validatorParameter, entityHandler, entityHandlerParameter);
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
