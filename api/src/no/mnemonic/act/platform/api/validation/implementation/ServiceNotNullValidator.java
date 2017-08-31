package no.mnemonic.act.platform.api.validation.implementation;

import no.mnemonic.act.platform.api.validation.constraints.ServiceNotNull;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ServiceNotNullValidator implements ConstraintValidator<ServiceNotNull, Object> {

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    return value != null;
  }

}
