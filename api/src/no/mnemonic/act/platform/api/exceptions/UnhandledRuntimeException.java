package no.mnemonic.act.platform.api.exceptions;

/**
 * UnhandledRuntimeException is a catch-all replacement for RuntimeExceptions which aren't handled explicitly.
 * The service backend will catch all unhandled RuntimeExceptions and replace them with UnhandledRuntimeException.
 */
public class UnhandledRuntimeException extends RuntimeException {

  private static final long serialVersionUID = -8524595669587308660L;

  public UnhandledRuntimeException(String message) {
    super(message);
  }

}
