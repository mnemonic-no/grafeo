package no.mnemonic.act.platform.api.validation.constraints;

import no.mnemonic.act.platform.api.validation.implementation.DefaultServiceNotNullValidator;
import no.mnemonic.act.platform.api.validation.implementation.ServiceNotNullValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Constraint that specifies that an object must not be null on the service layer.
 * <p>
 * Use this constraint when a property can be null on the REST layer but must not be null on the service layer,
 * e.g. for properties which are set by the REST layer before sending to the service layer.
 * <p>
 * By default the {@link DefaultServiceNotNullValidator} will be used, but on the service layer the
 * {@link ServiceNotNullValidator} should be configured.
 */
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {DefaultServiceNotNullValidator.class})
@Documented
public @interface ServiceNotNull {

  String message() default "{no.mnemonic.act.platform.api.validation.constraints.ServiceNotNull.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

}
