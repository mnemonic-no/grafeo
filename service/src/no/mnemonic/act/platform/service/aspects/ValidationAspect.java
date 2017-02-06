package no.mnemonic.act.platform.service.aspects;

import com.google.inject.matcher.Matchers;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.service.Service;
import no.mnemonic.commons.utilities.ObjectUtils;
import org.aopalliance.intercept.MethodInvocation;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Parameter;
import java.util.Set;

/**
 * The ValidationAspect validates all request objects which implement @{@link ValidatingRequest} before a service
 * method is called. It will throw an @{@link InvalidArgumentException} if the request fails validation.
 */
public class ValidationAspect extends AbstractAspect {

  private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Override
  protected void configure() {
    bindInterceptor(Matchers.subclassesOf(Service.class), matchServiceMethod(), this);
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    // Validate parameters.
    validateMethodParameters(invocation);
    // No violations found, proceed with service call.
    return invocation.proceed();
  }

  private void validateMethodParameters(MethodInvocation invocation) throws InvalidArgumentException {
    boolean isInvalid = false;
    InvalidArgumentException ex = new InvalidArgumentException();

    for (int i = 0; i < invocation.getMethod().getParameterCount(); i++) {
      Parameter parameter = invocation.getMethod().getParameters()[i];
      // Only validate arguments which implement ValidatingRequest.
      if (ValidatingRequest.class.isAssignableFrom(parameter.getType())) {
        ValidatingRequest request = (ValidatingRequest) invocation.getArguments()[i];
        if (request == null) {
          // Don't allow null request objects.
          ex.addValidationError(InvalidArgumentException.ErrorMessage.NULL, parameter.getName(), "NULL");
          isInvalid = true;
        } else {
          isInvalid |= validateRequest(request, ex);
        }
      }
    }

    if (isInvalid) {
      // If violations exist abort invoked service call.
      throw ex;
    }
  }

  private boolean validateRequest(ValidatingRequest request, InvalidArgumentException ex) {
    Set<ConstraintViolation<ValidatingRequest>> violations = validator.validate(request);
    for (ConstraintViolation<ValidatingRequest> v : violations) {
      // Add all violations to the provided InvalidArgumentException.
      String property = ObjectUtils.ifNotNull(v.getPropertyPath(), Object::toString, "UNKNOWN");
      String value = ObjectUtils.ifNotNull(v.getInvalidValue(), Object::toString, "NULL");
      ex.addValidationError(v.getMessage(), v.getMessageTemplate(), property, value);
    }

    return !violations.isEmpty();
  }

}
