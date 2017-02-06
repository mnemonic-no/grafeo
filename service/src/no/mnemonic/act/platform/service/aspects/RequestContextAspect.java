package no.mnemonic.act.platform.service.aspects;

import com.google.inject.matcher.Matchers;
import no.mnemonic.act.platform.service.Service;
import no.mnemonic.act.platform.service.contexts.RequestContext;
import org.aopalliance.intercept.MethodInvocation;

/**
 * The RequestContextAspect creates a RequestContext accessible during service calls.
 */
public class RequestContextAspect extends AbstractAspect {

  @Override
  protected void configure() {
    bindInterceptor(Matchers.subclassesOf(Service.class), matchServiceMethod(), this);
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Service service = getService(invocation);

    if (RequestContext.isSet()) {
      return invocation.proceed();
    }

    try (RequestContext ignored = RequestContext.set(service.createRequestContext())) {
      return invocation.proceed();
    }
  }

}
