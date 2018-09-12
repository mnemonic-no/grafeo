package no.mnemonic.act.platform.api.validation.implementation;

import no.mnemonic.act.platform.api.validation.constraints.ServiceNotNull;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Default validator implementation of the {@link ServiceNotNull} constraint which will always be valid.
 * This implementation is used when no other validator is explicitly specified.
 */
public class DefaultServiceNotNullValidator implements ConstraintValidator<ServiceNotNull, Object> {

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    return true;
  }

}
