package no.mnemonic.services.grafeo.service.validators;

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
   * @throws ValidatorConfigurationException Thrown if Validator cannot be created.
   */
  Validator get(String validator, String parameter);

}
