package no.mnemonic.services.grafeo.service.validators;

/**
 * The NullValidator verifies that a value is unset (null).
 */
class NullValidator implements Validator {

  @Override
  public boolean validate(String value) {
    return value == null;
  }

  @Override
  public ApplicableType[] appliesTo() {
    return new ApplicableType[]{ApplicableType.FactType};
  }

}
