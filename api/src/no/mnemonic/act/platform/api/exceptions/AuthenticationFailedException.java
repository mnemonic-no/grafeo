package no.mnemonic.act.platform.api.exceptions;

/**
 * An AuthenticationFailedException is thrown when a user could not be authenticated.
 */
public class AuthenticationFailedException extends Exception {

  private static final long serialVersionUID = -4979047581600008643L;

  public AuthenticationFailedException(String message) {
    super(message);
  }

}
