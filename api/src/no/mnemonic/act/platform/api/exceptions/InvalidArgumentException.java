package no.mnemonic.act.platform.api.exceptions;

import java.util.HashSet;
import java.util.Set;

/**
 * An InvalidArgumentException is thrown when a user provides invalid input such as invalid request objects.
 */
public class InvalidArgumentException extends Exception {

  /**
   * Used to provide consistent error messages throughout the whole application.
   * <p>
   * Feel free to add more, but try to keep them general.
   */
  public enum ErrorMessage {
    NULL("Value must not be null", "validation.not.null"),
    PARSE("Value could not be parsed", "validation.parse.error");

    private final String message;
    private final String messageTemplate;

    ErrorMessage(String message, String messageTemplate) {
      this.message = message;
      this.messageTemplate = messageTemplate;
    }

    /**
     * Returns non-translated error message.
     *
     * @return Non-translated error message
     */
    public String getMessage() {
      return message;
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

  private final Set<ValidationError> errors = new HashSet<>();

  /**
   * Returns all validation errors transported by the exception.
   *
   * @return All validation errors
   */
  public Set<ValidationError> getValidationErrors() {
    return errors;
  }

  /**
   * Add a new validation error.
   *
   * @param error    General validation error message
   * @param property Property failing validation
   * @param value    Value failing validation
   * @return this
   */
  public InvalidArgumentException addValidationError(ErrorMessage error, String property, String value) {
    errors.add(new ValidationError(error.getMessage(), error.getMessageTemplate(), property, value));
    return this;
  }

  /**
   * Add a new validation error. Use this method if no suitable general @{@link ErrorMessage} is available.
   *
   * @param message         Non-translated error message
   * @param messageTemplate Error message template used to translate error message
   * @param property        Property failing validation
   * @param value           Value failing validation
   * @return this
   */
  public InvalidArgumentException addValidationError(String message, String messageTemplate, String property, String value) {
    errors.add(new ValidationError(message, messageTemplate, property, value));
    return this;
  }

  /**
   * Wrapper for one validation error.
   */
  public class ValidationError {
    private final String message;
    private final String messageTemplate;
    private final String property;
    private final String value;

    ValidationError(String message, String messageTemplate, String property, String value) {
      this.message = message;
      this.messageTemplate = messageTemplate;
      this.property = property;
      this.value = value;
    }

    /**
     * Returns the non-translated error message.
     *
     * @return Non-translated error message
     */
    public String getMessage() {
      return message;
    }

    /**
     * Returns an error message template which can be used to translate an error message.
     *
     * @return Error message template
     */
    public String getMessageTemplate() {
      return messageTemplate;
    }

    /**
     * Returns the property which failed validation, e.g a query parameter or a parameter inside a request object.
     *
     * @return Property failing validation
     */
    public String getProperty() {
      return property;
    }

    /**
     * Returns the value which failed validation.
     *
     * @return Value failing validation
     */
    public String getValue() {
      return value;
    }
  }
}
