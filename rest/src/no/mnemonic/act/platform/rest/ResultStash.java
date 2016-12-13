package no.mnemonic.act.platform.rest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ResultStash<T> {

  private final int responseCode;
  private final int limit;
  private final int count;
  private final int size;
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
