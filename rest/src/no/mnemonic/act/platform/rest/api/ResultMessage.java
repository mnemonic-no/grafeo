package no.mnemonic.act.platform.rest.api;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.utilities.json.TimestampSerializer;

@ApiModel(description = "Container for a message returned from the API server, usually an error message.")
public class ResultMessage {

  public enum Type {
    ActionError, FieldError
  }

  @ApiModelProperty(value = "Type of the message", example = "FieldError", required = true)
  private final Type type;
  @ApiModelProperty(value = "Non-translated message", example = "Name has an invalid format", required = true)
  private final String message;
  @ApiModelProperty(value = "Message template usable for translating messages", example = "validation.error", required = true)
  private final String messageTemplate;
  @ApiModelProperty(value = "Contains the field which caused an error (can usually be mapped to a field in the request)", example = "name")
  private final String field;
  @ApiModelProperty(value = "Contains the parameter which caused an error (usually the field's value in the request)", example = "Non-Valid-Name")
  private final String parameter;
  @ApiModelProperty(value = "When the message was generated", example = "2016-09-28T21:26:22Z", dataType = "string", required = true)
  @JsonSerialize(using = TimestampSerializer.class)
  private final long timestamp;

  private ResultMessage(Type type, String message, String messageTemplate, String field, String parameter) {
    this.type = type;
    this.message = message;
    this.messageTemplate = messageTemplate;
    this.field = field;
    this.parameter = parameter;
    this.timestamp = System.currentTimeMillis();
  }

  public Type getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  public String getMessageTemplate() {
    return messageTemplate;
  }

  public String getField() {
    return field;
  }

  public String getParameter() {
    return parameter;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Type type;
    private String message;
    private String messageTemplate;
    private String field;
    private String parameter;

    private Builder() {
    }

    public ResultMessage build() {
      return new ResultMessage(type, message, messageTemplate, field, parameter);
    }

    public Builder setType(Type type) {
      this.type = type;
      return this;
    }

    public Builder setMessage(String message) {
      this.message = message;
      return this;
    }

    public Builder setMessageTemplate(String messageTemplate) {
      this.messageTemplate = messageTemplate;
      return this;
    }

    public Builder setField(String field) {
      this.field = field;
      return this;
    }

    public Builder setParameter(String parameter) {
      this.parameter = parameter;
      return this;
    }
  }

}
