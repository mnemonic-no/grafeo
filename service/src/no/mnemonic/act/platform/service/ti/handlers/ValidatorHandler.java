package no.mnemonic.act.platform.service.ti.handlers;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;

import javax.inject.Inject;

public class ValidatorHandler {

  private final ValidatorFactory validatorFactory;

  @Inject
  public ValidatorHandler(ValidatorFactory validatorFactory) {
    this.validatorFactory = validatorFactory;
  }

  /**
   * Assert that a Validator exists.
   *
   * @param validator          Name of Validator
   * @param validatorParameter Parameter of Validator
   * @throws InvalidArgumentException Thrown if Validator does not exist
   */
  public void assertValidatorExists(String validator, String validatorParameter) throws InvalidArgumentException {
    try {
      validatorFactory.get(validator, validatorParameter);
    } catch (IllegalArgumentException ex) {
      // An IllegalArgumentException will be thrown if a Validator cannot be found.
      throw new InvalidArgumentException()
        .addValidationError(ex.getMessage(), "validator.not.exist", "validator", validator);
    }
  }
}
