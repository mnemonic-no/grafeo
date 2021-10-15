package no.mnemonic.act.platform.service.ti.handlers;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.service.validators.Validator;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;

public class ValidatorHandler {

  private final ValidatorFactory validatorFactory;

  @Inject
  public ValidatorHandler(ValidatorFactory validatorFactory) {
    this.validatorFactory = validatorFactory;
  }

  /**
   * Assert that a Validator exists and that it can be used for an ObjectType or FactType.
   *
   * @param validator          Name of Validator
   * @param validatorParameter Parameter of Validator
   * @param requestedType      Type which requests to use the Validator
   * @throws InvalidArgumentException Thrown if Validator does not exist, or it cannot be used for an ObjectType or FactType
   */
  public void assertValidator(String validator, String validatorParameter, Validator.ApplicableType requestedType) throws InvalidArgumentException {
    Validator resolved;

    try {
      resolved = validatorFactory.get(validator, validatorParameter);
    } catch (IllegalArgumentException ex) {
      // An IllegalArgumentException will be thrown if a Validator cannot be found.
      throw new InvalidArgumentException()
              .addValidationError(ex.getMessage(), "validator.not.exist", "validator", validator);
    }

    if (!SetUtils.in(requestedType, resolved.appliesTo())) {
      String msg = String.format("It is not allowed to use validator '%s' for %s.", validator, requestedType);
      throw new InvalidArgumentException()
              .addValidationError(msg, "validator.not.applicable", "validator", validator);
    }
  }
}
