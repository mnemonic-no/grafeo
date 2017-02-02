package no.mnemonic.act.platform.service.aspects;

import com.google.inject.matcher.Matchers;
import no.mnemonic.act.platform.service.Service;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import org.aopalliance.intercept.MethodInvocation;

/**
 * The AuthenticationAspect makes sure that a user is successfully authenticated and creates a SecurityContext.
 */
public class AuthenticationAspect extends AbstractAspect {

  @Override
  protected void configure() {
    bindInterceptor(Matchers.subclassesOf(Service.class), matchServiceMethod(), this);
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Service service = getService(invocation);
    // TODO: Authenticate user!

    if (SecurityContext.isSet()) {
      return invocation.proceed();
    }

    try (SecurityContext ignored = SecurityContext.set(service.createSecurityContext())) {
      return invocation.proceed();
    }
  }

}
