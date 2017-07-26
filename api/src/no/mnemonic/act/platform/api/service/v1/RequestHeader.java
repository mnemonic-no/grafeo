package no.mnemonic.act.platform.api.service.v1;

import no.mnemonic.services.common.auth.model.Credentials;

/**
 * The RequestHeader is used to transport information from the client to the service layer, e.g. credentials identifying
 * the user calling a method. In general, the RequestHeader must be set by a REST endpoint when calling a service method.
 */
public class RequestHeader {

  private final Credentials credentials;

  private RequestHeader(Credentials credentials) {
    this.credentials = credentials;
  }

  public Credentials getCredentials() {
    return credentials;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Credentials credentials;

    private Builder() {
    }

    public RequestHeader build() {
      return new RequestHeader(credentials);
    }

    public Builder setCredentials(Credentials credentials) {
      this.credentials = credentials;
      return this;
    }
  }
}
