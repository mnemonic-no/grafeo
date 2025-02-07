package no.mnemonic.services.grafeo.rest.api;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.services.grafeo.utilities.json.TimestampSerializer;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Container for a message returned from the API server, usually an error message.")
public class ResultMessage {

  public enum Type {
    ActionError, FieldError
  }

  @Schema(description = "Type of the message", example = "FieldError", requiredMode = REQUIRED)
  private final Type type;
  @Schema(description = "Non-translated message", example = "Name has an invalid format", requiredMode = REQUIRED)
  private final String message;
  @Schema(description = "Message template usable for translating messages", example = "validation.error", requiredMode = REQUIRED)
  private final String messageTemplate;
  @Schema(description = "Contains the field which caused an error (can usually be mapped to a field in the request)", example = "name")
  private final String field;
  @Schema(description = "Contains the parameter which caused an error (usually the field's value in the request)", example = "Non-Valid-Name")
  private final String parameter;
  @Schema(description = "When the message was generated", example = "2016-09-28T21:26:22Z", type = "string", requiredMode = REQUIRED)
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
