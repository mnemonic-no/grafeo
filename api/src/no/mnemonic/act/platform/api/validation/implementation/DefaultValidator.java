package no.mnemonic.act.platform.api.validation.implementation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.annotation.Annotation;

public class DefaultValidator implements ConstraintValidator<Annotation, Object> {

  @Override
  public void initialize(Annotation constraintAnnotation) {
    // NOOP
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    return true;
  }

}
