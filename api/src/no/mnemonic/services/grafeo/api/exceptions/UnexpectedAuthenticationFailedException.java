package no.mnemonic.services.grafeo.api.exceptions;

/**
 * An UnexpectedAuthenticationFailedException is thrown when a user could not be authenticated in situations where an
 * authenticated user is expected. This exception should be used with care and only in situations where a user should
 * have already been authenticated.
 */
public class UnexpectedAuthenticationFailedException extends RuntimeException {

  private static final long serialVersionUID = 4502030415217078450L;

  public UnexpectedAuthenticationFailedException(String message) {
    super(message);
  }

}
