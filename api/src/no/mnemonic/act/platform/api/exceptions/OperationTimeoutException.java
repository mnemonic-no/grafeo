package no.mnemonic.act.platform.api.exceptions;

/**
 * An OperationTimeoutException is thrown when an operation took to long time to process.
 */
public class OperationTimeoutException extends Exception {

  private static final long serialVersionUID = -3738602078226319190L;

  private final String messageTemplate;

  public OperationTimeoutException(String message, String messageTemplate) {
    super(message);
    this.messageTemplate = messageTemplate;
  }

  /**
   * Returns an error message template which can be used to translate an error message.
   *
   * @return Error message template
   */
  public String getMessageTemplate() {
    return messageTemplate;
  }

}
