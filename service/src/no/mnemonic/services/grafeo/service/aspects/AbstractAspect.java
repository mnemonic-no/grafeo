package no.mnemonic.services.grafeo.service.aspects;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import no.mnemonic.services.grafeo.api.service.v1.RequestHeader;
import no.mnemonic.services.grafeo.service.Service;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;

/**
 * The AbstractAspect provides common methods used by multiple aspects.
 */
abstract class AbstractAspect extends AbstractModule implements MethodInterceptor {

  /**
   * Retrieve the service instance from which a method was invoked.
   *
   * @param invocation Invoked method
   * @return Service of invoked method
   */
  Service getService(MethodInvocation invocation) {
    if (!(invocation.getThis() instanceof Service)) {
      throw new IllegalArgumentException("Invocation target is not a service implementation: " + invocation.getThis());
    }
    return (Service) invocation.getThis();
  }

  /**
   * Retrieve the RequestHeader set in the invoked method.
   *
   * @param invocation Invoked method
   * @return RequestHeader
   */
  RequestHeader getRequestHeader(MethodInvocation invocation) {
    Method method = invocation.getMethod();
    if (!isServiceMethod(method)) {
      throw new IllegalArgumentException("Invoked method is not a service method: " + method.getName());
    }

    if (invocation.getArguments()[0] == null) {
      throw new IllegalStateException("RequestHeader is not set in method: " + method.getName());
    }

    return (RequestHeader) invocation.getArguments()[0];
  }

  /**
   * Create a Matcher which matches against a service method.
   *
   * @return Matcher matching a service method
   */
  Matcher<Method> matchServiceMethod() {
    return new AbstractMatcher<Method>() {
      @Override
      public boolean matches(Method method) {
        return isServiceMethod(method);
      }
    };
  }

  private boolean isServiceMethod(Method method) {
    // Assume that for every service method the first parameter is a RequestHeader.
    return method.getParameterTypes().length >= 1 && method.getParameterTypes()[0].equals(RequestHeader.class);
  }

}
