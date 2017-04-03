package no.mnemonic.act.platform.api.validation.constraints;

import no.mnemonic.act.platform.api.validation.implementation.DefaultValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {DefaultValidator.class})
@Documented
public @interface ServiceNotNull {

  String message() default "{no.mnemonic.act.platform.api.validation.constraints.ServiceNotNull.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

}
