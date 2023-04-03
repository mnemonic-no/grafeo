package no.mnemonic.services.grafeo.service.implementation.handlers;

import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.service.validators.Validator;
import no.mnemonic.services.grafeo.service.validators.ValidatorConfigurationException;
import no.mnemonic.services.grafeo.service.validators.ValidatorFactory;

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
    } catch (ValidatorConfigurationException ex) {
      // A ValidatorConfigurationException will be thrown if a Validator cannot be found or initialized.
      // Inspect the exception and re-throw as InvalidArgumentException.
      switch (ex.getReason()) {
        case NotFound:
          throw new InvalidArgumentException()
                  .addValidationError(ex.getMessage(), "validator.not.exist", "validator", validator);
        case Misconfigured:
          throw new InvalidArgumentException()
                  .addValidationError(ex.getMessage(), "validator.misconfigured", "validatorParameter", validatorParameter);
        default:
          throw new InvalidArgumentException()
                  .addValidationError(ex.getMessage(), "validator.initialization.error", "validator", validator);
      }
    }

    if (!SetUtils.in(requestedType, resolved.appliesTo())) {
      String msg = String.format("It is not allowed to use validator '%s' for %s.", validator, requestedType);
      throw new InvalidArgumentException()
              .addValidationError(msg, "validator.not.applicable", "validator", validator);
    }
  }
}
