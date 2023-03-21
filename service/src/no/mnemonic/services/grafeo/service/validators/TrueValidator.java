package no.mnemonic.services.grafeo.service.validators;

/**
 * The TrueValidator accepts any input value without any validation. Use carefully!
 */
class TrueValidator implements Validator {

  @Override
  public boolean validate(String value) {
    return true;
  }

}
