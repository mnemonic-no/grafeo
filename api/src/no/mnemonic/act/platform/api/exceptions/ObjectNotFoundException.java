package no.mnemonic.act.platform.api.exceptions;

/**
 * An ObjectNotFoundException is thrown when a requested object can not be found, e.g. it cannot be fetched from the database.
 */
public class ObjectNotFoundException extends Exception {

  private final String messageTemplate;
  private final String property;
  private final String value;

  public ObjectNotFoundException(String message, String messageTemplate, String property, String value) {
    super(message);
    this.messageTemplate = messageTemplate;
    this.property = property;
    this.value = value;
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
   * Returns the property which was used to identify the requested object, e.g. the 'id' property of an object.
   *
   * @return Property used for identifying requested object
   */
  public String getProperty() {
    return property;
  }

  /**
   * Returns the value which was used to identify the requested object, e.g. the actual value of the 'id' property.
   *
   * @return Value used for identifying requested object
   */
  public String getValue() {
    return value;
  }

}
