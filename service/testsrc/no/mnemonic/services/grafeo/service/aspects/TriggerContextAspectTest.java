package no.mnemonic.services.grafeo.service.aspects;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.grafeo.api.service.v1.RequestHeader;
import no.mnemonic.services.grafeo.service.Service;
import no.mnemonic.services.grafeo.service.TestSecurityContext;
import no.mnemonic.services.grafeo.service.contexts.SecurityContext;
import no.mnemonic.services.grafeo.service.contexts.TriggerContext;
import no.mnemonic.services.triggers.pipeline.api.TriggerEvent;
import no.mnemonic.services.triggers.pipeline.api.TriggerEventConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TriggerContextAspectTest {

  @Mock
  private TriggerEventConsumer triggerEventConsumer;
  @Mock
  private static TriggerEvent triggerEvent;

  @Test
  public void testInjectNewTriggerContext() {
    TestService service = createService();

    assertFalse(TriggerContext.isSet());
    assertEquals("Called!", service.method(RequestHeader.builder().build(), "Called!"));
    assertFalse(TriggerContext.isSet());
  }

  @Test
  public void testAlreadyInjectedTriggerContext() throws Exception {
    TestService service = createService();

    try (TriggerContext ignored = TriggerContext.set(TriggerContext.builder().setTriggerEventConsumer(triggerEventConsumer).build())) {
      assertTrue(TriggerContext.isSet());
      assertEquals("Called!", service.method(RequestHeader.builder().build(), "Called!"));
      assertTrue(TriggerContext.isSet());
    }
  }

  @Test
  public void testSubmitRegisteredTriggerEvents() throws Exception {
    assertEquals("Called!", createService().method(RequestHeader.builder().build(), "Called!"));
    verify(triggerEventConsumer).submit(triggerEvent);
  }

  private TestService createService() {
    return Guice.createInjector(new TestModule()).getInstance(TestService.class);
  }

  private class TestModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(TriggerEventConsumer.class).toInstance(triggerEventConsumer);
      install(new TriggerContextAspect());
    }
  }

  static class TestService implements Service {
    String method(RequestHeader rh, String something) {
      assertTrue(TriggerContext.isSet());
      TriggerContext.get().registerTriggerEvent(triggerEvent);
      return something;
    }

    @Override
    public SecurityContext createSecurityContext(Credentials credentials) {
      return new TestSecurityContext();
    }
  }
}
