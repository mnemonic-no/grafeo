package no.mnemonic.act.platform.rest.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.rest.providers.ObjectMapperResolver;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

@ApiModel(description = "Container for all responses from the API.")
public class ResultStash<T> {

  @ApiModelProperty(value = "Status code returned from API", example = "200", required = true)
  private final int responseCode;
  @ApiModelProperty(value = "Maximum number of returned results", example = "25", required = true)
  private final int limit;
  @ApiModelProperty(value = "Number of available results on server", example = "100", required = true)
  private final int count;
  @ApiModelProperty(value = "Contains messages returned from the API, usually error messages")
  private final List<ResultMessage> messages;
  @ApiModelProperty(value = "Returned results (might be an array or a single object)", required = true)
  private final T data;

  private ResultStash(int responseCode, int limit, int count, List<ResultMessage> messages, T data) {
    this.responseCode = responseCode;
    this.limit = limit;
    this.count = count;
    this.messages = messages;
    this.data = data;
  }

  public int getResponseCode() {
    return responseCode;
  }

  public int getLimit() {
    return limit;
  }

  public int getCount() {
    return count;
  }

  // This is needed to make the 'size' field visible in Swagger documentation. It's not actually used as the field
  // will be set by the custom ResultStashSerializer.
  @ApiModelProperty(value = "Actual number of returned results", example = "25", required = true)
  public int getSize() {
    return -1;
  }

  public List<ResultMessage> getMessages() {
    return messages;
  }

  public T getData() {
    return data;
  }

  public static <T> Response buildResponse(T model) {
    return ResultStash.builder()
            .setData(model)
            .buildResponse();
  }

  public static <T> Response buildResponse(ResultSet<T> result) {
    return ResultStash.builder()
            .setLimit(result.getLimit())
            .setCount(result.getCount())
            .setData(result)
            .buildResponse();
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  public static class Builder<T> {
    private Response.Status status = Response.Status.OK;
    private int limit;
    private int count;
    private List<ResultMessage> messages;
    private T data;

    private Builder() {
    }

    public Response buildResponse() {
      return Response
              .status(status)
              .type(MediaType.APPLICATION_JSON_TYPE)
              .entity(new ResultStash<>(status.getStatusCode(), limit, count, messages, data))
              .build();
    }

    public Builder<T> setStatus(Response.Status status) {
      this.status = status;
      return this;
    }

    public Builder<T> setLimit(int limit) {
      this.limit = limit;
      return this;
    }

    public Builder<T> setCount(int count) {
      this.count = count;
      return this;
    }

    public Builder<T> setData(T data) {
      this.data = data;
      return this;
    }

    public Builder<T> addActionError(String message, String messageTemplate) {
      ResultMessage error = ResultMessage.builder()
              .setType(ResultMessage.Type.ActionError)
              .setMessage(message)
              .setMessageTemplate(messageTemplate)
              .build();

      this.messages = ListUtils.addToList(this.messages, error);
      return this;
    }

    public Builder<T> addActionError(String message, String messageTemplate, String field, String value) {
      ResultMessage error = ResultMessage.builder()
              .setType(ResultMessage.Type.ActionError)
              .setMessage(message)
              .setMessageTemplate(messageTemplate)
              .setField(field)
              .setParameter(value)
              .build();

      this.messages = ListUtils.addToList(this.messages, error);
      return this;
    }

    public Builder<T> addFieldError(String message, String messageTemplate, String field, String value) {
      ResultMessage error = ResultMessage.builder()
              .setType(ResultMessage.Type.FieldError)
              .setMessage(message)
              .setMessageTemplate(messageTemplate)
              .setField(field)
              .setParameter(value)
              .build();

      this.messages = ListUtils.addToList(this.messages, error);
      return this;
    }
  }

  /**
   * Custom serializer for {@link ResultStash} which consumes all results from the service layer and writes them to
   * the output stream (if result implements Iterable or Iterator). It will also write the correct number of fetched
   * results, i.e. the 'size' field in {@link ResultStash}. If the result contains a single object that object will
   * simply be serialized as-is and 'size' will be 0.
   * <p>
   * Note: This serializer cannot be registered directly on {@link ResultStash} using {@link JsonSerialize} because
   * then Swagger won't pick up the properties and won't show them in the documentation. Instead it's manually
   * registered on {@link ObjectMapper}, see {@link ObjectMapperResolver}.
   */
  public static class ResultStashSerializer extends JsonSerializer<ResultStash> {

    @Override
    public void serialize(ResultStash value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      gen.writeStartObject();

      // Write simple fields to output. 'messages' will be handled correctly using default serializers.
      gen.writeNumberField("responseCode", value.getResponseCode());
      gen.writeNumberField("limit", value.getLimit());
      gen.writeNumberField("count", value.getCount());
      gen.writeObjectField("messages", value.getMessages());

      // Write 'data' field which will consume all results from the service layer.
      int size = writeData(value.getData(), gen);

      // Write 'size' field based on the number of fetched results.
      gen.writeNumberField("size", size);

      gen.writeEndObject();
    }

    private int writeData(Object data, JsonGenerator gen) throws IOException {
      gen.writeFieldName("data");

      if (data instanceof Iterable) {
        return writeIterator(((Iterable) data).iterator(), gen);
      } else if (data instanceof Iterator) {
        return writeIterator((Iterator) data, gen);
      } else {
        // Not a collection, write single object as-is and set 'size' to 0.
        gen.writeObject(data);
        return 0;
      }
    }

    private int writeIterator(Iterator iterator, JsonGenerator gen) throws IOException {
      // Write an array with all results. This will actually fetch all results from the service layer!
      gen.writeStartArray();

      int size = 0;
      while (iterator.hasNext()) {
        gen.writeObject(iterator.next());
        size++;
      }

      gen.writeEndArray();
      return size;
    }
  }
}
