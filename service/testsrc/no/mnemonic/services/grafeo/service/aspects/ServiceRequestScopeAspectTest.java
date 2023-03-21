package no.mnemonic.services.grafeo.service.aspects;

import com.google.inject.Guice;
import com.google.inject.Injector;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.grafeo.api.service.v1.RequestHeader;
import no.mnemonic.services.grafeo.service.Service;
import no.mnemonic.services.grafeo.service.contexts.SecurityContext;
import no.mnemonic.services.grafeo.service.contexts.TriggerContext;
import no.mnemonic.services.grafeo.service.scopes.ServiceRequestScope;
import no.mnemonic.services.grafeo.service.ti.TiSecurityContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ServiceRequestScopeAspectTest {

  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private TriggerContext triggerContext;

  private final static AtomicInteger initializationCounter = new AtomicInteger();

  @Before
  public void setUp() {
    initMocks(this);

    TiSecurityContext.set(securityContext);
    TriggerContext.set(triggerContext);
  }

  @After
  public void cleanUp() {
    TiSecurityContext.clear();
    TriggerContext.clear();

    initializationCounter.set(0);
  }

  @Test
  public void testSingleServiceMethodCall() {
    assertNotNull(createService().method(RequestHeader.builder().build()));
    assertEquals(1, initializationCounter.get());
  }

  @Test
  public void testMultipleServiceMethodCalls() {
    RequestScoped scoped1 = createService().method(RequestHeader.builder().build());
    RequestScoped scoped2 = createService().method(RequestHeader.builder().build());
    // Verify that two different RequestScoped objects are injected into consecutive method calls.
    assertNotSame(scoped1, scoped2);
    assertEquals(2, initializationCounter.get());
  }

  private TestService createService() {
    return Guice.createInjector(new ServiceRequestScopeAspect()).getInstance(TestService.class);
  }

  @ServiceRequestScope
  static class RequestScoped {
    @Inject
    RequestScoped(TiSecurityContext context1, TriggerContext context2) {
      // Verify that the injected contexts are the same as the ones provided by Context.get().
      assertSame(context1, TiSecurityContext.get());
      assertSame(context2, TriggerContext.get());

      initializationCounter.incrementAndGet();
    }

    private RequestScoped handle() {
      return this;
    }
  }

  static class Converter {
    private final RequestScoped injectedIntoConverter;

    @Inject
    Converter(RequestScoped injectedIntoConverter) {
      this.injectedIntoConverter = injectedIntoConverter;
    }

    private RequestScoped injected() {
      return injectedIntoConverter;
    }
  }

  static class Delegate {
    private final RequestScoped injectedIntoDelegate;
    private final Converter converter;

    @Inject
    Delegate(RequestScoped injectedIntoDelegate, Converter converter) {
      this.injectedIntoDelegate = injectedIntoDelegate;
      this.converter = converter;
    }

    private RequestScoped injected() {
      // Verify that the same RequestScoped object was injected into both the delegate and converter.
      assertSame(injectedIntoDelegate, converter.injected());
      return injectedIntoDelegate.handle();
    }
  }

  static class TestService implements Service {
    private final Injector injector;

    @Inject
    TestService(Injector injector) {
      this.injector = injector;
    }

    RequestScoped method(RequestHeader rh) {
      return injector.getInstance(Delegate.class).injected();
    }

    @Override
    public SecurityContext createSecurityContext(Credentials credentials) {
      return null;
    }
  }
}
