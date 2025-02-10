package no.mnemonic.services.grafeo.service.aspects;

import com.google.inject.Key;
import com.google.inject.matcher.Matchers;
import no.mnemonic.services.grafeo.service.Service;
import no.mnemonic.services.grafeo.service.contexts.SecurityContext;
import no.mnemonic.services.grafeo.service.contexts.TriggerContext;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.scopes.ServiceRequestScope;
import no.mnemonic.services.grafeo.service.scopes.ServiceRequestScopeImpl;
import org.aopalliance.intercept.MethodInvocation;

import jakarta.inject.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The ServiceRequestScopeAspect handles the {@link ServiceRequestScope} for a single service method call. It sets up
 * the scope before the service method is invoked, terminates the scope afterwards, and makes all thread-local contexts
 * available in the scope. Note that all contexts must have been initialized before this aspect is called (i.e. install
 * the ServiceRequestScopeAspect as the last aspect).
 */
@SuppressWarnings("unchecked")
public class ServiceRequestScopeAspect extends AbstractAspect {

  private static final Provider UNSEEDED_KEY_PROVIDER = () -> {
    throw new IllegalStateException("If you got here then it means that your code asked for a scoped object which " +
            "should have been explicitly seeded in the service request scope by calling ServiceRequestScopeImpl.seed().");
  };

  private final ServiceRequestScopeImpl scope = new ServiceRequestScopeImpl();
  private final Map<Class, Supplier> contexts = new HashMap<>();

  public ServiceRequestScopeAspect() {
    // Add all contexts which will be seeded in the service request scope.
    contexts.put(SecurityContext.class, SecurityContext::get);
    contexts.put(GrafeoSecurityContext.class, GrafeoSecurityContext::get);
    contexts.put(TriggerContext.class, TriggerContext::get);
  }

  @Override
  protected void configure() {
    bindInterceptor(Matchers.subclassesOf(Service.class), matchServiceMethod(), this);

    // Register service request scope in Guice.
    bindScope(ServiceRequestScope.class, scope);

    // Bind all contexts in service request scope. They must be explicitly seeded after the scope
    // has been entered, otherwise the UNSEEDED_KEY_PROVIDER will be called and throw an exception.
    for (Class ctx : contexts.keySet()) {
      bind(ctx).toProvider(UNSEEDED_KEY_PROVIDER).in(ServiceRequestScope.class);
    }
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    if (scope.isInsideScope()) {
      // If already inside scope, just proceed with service method.
      return invocation.proceed();
    }

    // Enter service request scope.
    scope.enter();
    try {
      // Explicitly seed all contexts. The contexts must already been set at this point!
      for (Map.Entry<Class, Supplier> seed : contexts.entrySet()) {
        scope.seed(Key.get(seed.getKey()), seed.getValue().get());
      }

      // Proceed with service method after all contexts have been seeded.
      return invocation.proceed();
    } finally {
      // Leave scope once the service method has finished. Do that in a finally-block
      // to not leave the scope open in case the service method throws an exception.
      scope.exit();
    }
  }

}
