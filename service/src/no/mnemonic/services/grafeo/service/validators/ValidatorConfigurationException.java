package no.mnemonic.services.grafeo.service.validators;

public class ValidatorConfigurationException extends RuntimeException {

  private static final long serialVersionUID = -2671711938032434577L;

  public enum Reason {
    NotFound, Misconfigured
  }

  private final Reason reason;

  public ValidatorConfigurationException(String message, Reason reason) {
    super(message);
    this.reason = reason;
  }

  public Reason getReason() {
    return reason;
  }

}
