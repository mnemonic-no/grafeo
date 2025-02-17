package no.mnemonic.services.grafeo.rest.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.rest.providers.ObjectMapperResolver;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Container for all responses from the API.")
public class ResultStash<T> {

  @Schema(description = "Status code returned from API", example = "200", requiredMode = REQUIRED)
  private final int responseCode;
  @Schema(description = "Maximum number of returned results", example = "25", requiredMode = REQUIRED)
  private final int limit;
  @Schema(description = "Number of available results on server", example = "100", requiredMode = REQUIRED)
  private final int count;
  @Schema(description = "Contains messages returned from the API, usually error messages")
  private final List<ResultMessage> messages;
  @Schema(description = "Returned results (might be an array or a single object)", requiredMode = REQUIRED)
  private final T data;

  /**
   * Only ever needed for subclasses used to make Swagger recognize the correct response model.
   * <p>
   * DON'T USE FOR OTHER PURPOSES!
   */
  protected ResultStash() {
    this(0, 0, 0, null, null);
  }

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
  @Schema(description = "Actual number of returned results", example = "25", requiredMode = REQUIRED)
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
              .entity((StreamingOutput) output -> {
                // Manually write ResultStash to output in order to stream results to clients.
                // This will use ResultStashSerializer as configured in ObjectMapperResolver.
                try (JsonGenerator gen = ObjectMapperResolver.getInstance().getFactory().createGenerator(output)) {
                  gen.writeObject(new ResultStash<>(status.getStatusCode(), limit, count, messages, data));
                  gen.flush();
                } finally {
                  // Ensure that resources are released!
                  if (data instanceof Closeable closeable) {
                    closeable.close();
                  }
                }
              })
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
