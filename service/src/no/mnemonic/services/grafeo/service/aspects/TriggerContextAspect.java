package no.mnemonic.services.grafeo.service.aspects;

import com.google.inject.matcher.Matchers;
import no.mnemonic.services.grafeo.service.Service;
import no.mnemonic.services.grafeo.service.contexts.TriggerContext;
import no.mnemonic.services.triggers.pipeline.api.TriggerEventConsumer;
import org.aopalliance.intercept.MethodInvocation;

import javax.inject.Inject;

/**
 * The TriggerContextAspect creates a TriggerContext accessible during service calls and closes it
 * when the service call is finished which will submit all queued TriggerEvents to evaluation.
 */
public class TriggerContextAspect extends AbstractAspect {

  @Inject
  private TriggerEventConsumer triggerEventConsumer;

  @Override
  protected void configure() {
    requestInjection(this);
    bindInterceptor(Matchers.subclassesOf(Service.class), matchServiceMethod(), this);
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    if (TriggerContext.isSet()) {
      return invocation.proceed();
    }

    try (TriggerContext ignored = TriggerContext.set(TriggerContext.builder().setTriggerEventConsumer(triggerEventConsumer).build())) {
      return invocation.proceed();
    }
  }

}
