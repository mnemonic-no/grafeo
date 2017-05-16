package no.mnemonic.act.platform.api.exceptions;

/**
 * An AccessDeniedException is thrown when a user was denied access to a resource or
 * if the user is not allowed to perform a specific operation.
 */
public class AccessDeniedException extends Exception {

  private static final long serialVersionUID = 7800077672612415522L;

  public AccessDeniedException(String message) {
    super(message);
  }

}
