package no.mnemonic.services.grafeo.service.aspects;

import com.google.inject.matcher.Matchers;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.service.v1.RequestHeader;
import no.mnemonic.services.grafeo.service.Service;
import no.mnemonic.services.grafeo.service.contexts.SecurityContext;
import org.aopalliance.intercept.MethodInvocation;

import jakarta.inject.Inject;

/**
 * The AuthenticationAspect makes sure that a user is successfully authenticated and creates a SecurityContext.
 */
public class AuthenticationAspect extends AbstractAspect {

  @Inject
  private AccessController accessController;

  @Override
  protected void configure() {
    requestInjection(this);
    bindInterceptor(Matchers.subclassesOf(Service.class), matchServiceMethod(), this);
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Service service = getService(invocation);
    RequestHeader requestHeader = getRequestHeader(invocation);

    try {
      // For each service method invocation verify that user is authenticated!
      //noinspection unchecked
      accessController.validate(requestHeader.getCredentials());
    } catch (InvalidCredentialsException ex) {
      throw new AuthenticationFailedException("Could not authenticate user: " + ex.getMessage());
    }

    if (SecurityContext.isSet()) {
      return invocation.proceed();
    }

    try (SecurityContext ignored = SecurityContext.set(service.createSecurityContext(requestHeader.getCredentials()))) {
      return invocation.proceed();
    }
  }

}
