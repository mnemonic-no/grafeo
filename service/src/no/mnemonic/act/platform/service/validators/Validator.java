package no.mnemonic.act.platform.service.validators;

/**
 * A Validator verifies that the value of an Object or Fact fulfills a specific format.
 */
public interface Validator {

  /**
   * Verifies that a value fulfills a specific format.
   *
   * @param value Value to validate.
   * @return True, if the value passes the validation.
   */
  boolean validate(String value);

}
