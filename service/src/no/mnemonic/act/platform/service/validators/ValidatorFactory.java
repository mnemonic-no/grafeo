package no.mnemonic.act.platform.service.validators;

/**
 * A factory which creates Validators.
 */
public interface ValidatorFactory {

  /**
   * Creates a Validator based on it's name and a parameter string.
   *
   * @param validator Name of Validator.
   * @param parameter Parameter string which is Validator specific.
   * @return A Validator instance.
   */
  Validator get(String validator, String parameter);

}
