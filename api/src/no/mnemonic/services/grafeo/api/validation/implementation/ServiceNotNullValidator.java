package no.mnemonic.services.grafeo.api.validation.implementation;

import no.mnemonic.services.grafeo.api.validation.constraints.ServiceNotNull;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator implementation of the {@link ServiceNotNull} constraint which will validate that an object is not null.
 * This implementation is used when validating requests on the service layer (but not on the REST layer).
 */
public class ServiceNotNullValidator implements ConstraintValidator<ServiceNotNull, Object> {

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    return value != null;
  }

}
