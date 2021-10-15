package no.mnemonic.act.platform.service.validators;

/**
 * A Validator verifies that the value of an Object or Fact fulfills a specific format.
 */
public interface Validator {

  enum ApplicableType {
    FactType, ObjectType
  }

  /**
   * Verifies that a value fulfills a specific format.
   *
   * @param value Value to validate.
   * @return True, if the value passes the validation.
   */
  boolean validate(String value);

  /**
   * Specifies whether the validator can be used for ObjectTypes, FactTypes or both.
   * <p>
   * The default implementation allows both. Implement this method if the usage should be restricted to only one type.
   *
   * @return Types which can use the validator
   */
  default ApplicableType[] appliesTo() {
    return ApplicableType.values();
  }

}
