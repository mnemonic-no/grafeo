package no.mnemonic.act.platform.rest.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@ApiModel(description = "Container for all responses from the API.")
public class ResultStash<T> {

  @ApiModelProperty(value = "Status code returned from API", example = "200", required = true)
  private final int responseCode;
  @ApiModelProperty(value = "Maximum number of returned results", example = "25", required = true)
  private final int limit;
  @ApiModelProperty(value = "Number of available results on server", example = "100", required = true)
  private final int count;
  @ApiModelProperty(value = "Actual number of returned results", example = "25", required = true)
  private final int size;
  @ApiModelProperty(value = "Returned results (might be an array or a single object)", required = true)
  private final T data;

  private ResultStash(int responseCode, int limit, int count, int size, T data) {
    this.responseCode = responseCode;
    this.limit = limit;
    this.count = count;
    this.size = size;
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

  public int getSize() {
    return size;
  }

  public T getData() {
    return data;
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  public static class Builder<T> {
    private Response.Status status = Response.Status.OK;
    private int limit;
    private int count;
    private int size;
    private T data;

    private Builder() {
    }

    public Response buildResponse() {
      return Response
              .status(status)
              .type(MediaType.APPLICATION_JSON_TYPE)
              .entity(new ResultStash<>(status.getStatusCode(), limit, count, size, data))
              .build();
    }

    public Builder setStatus(Response.Status status) {
      this.status = status;
      return this;
    }

    public Builder setLimit(int limit) {
      this.limit = limit;
      return this;
    }

    public Builder setCount(int count) {
      this.count = count;
      return this;
    }

    public Builder setSize(int size) {
      this.size = size;
      return this;
    }

    public Builder setData(T data) {
      this.data = data;
      return this;
    }
  }

}
