package no.mnemonic.act.platform.service.aspects;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import no.mnemonic.act.platform.api.service.v1.RequestHeader;
import no.mnemonic.act.platform.service.Service;
import no.mnemonic.act.platform.service.TestSecurityContext;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.contexts.TriggerContext;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.triggers.pipeline.api.TriggerEvent;
import no.mnemonic.services.triggers.pipeline.api.TriggerEventConsumer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class TriggerContextAspectTest {

  @Mock
  private TriggerEventConsumer triggerEventConsumer;
  @Mock
  private static TriggerEvent triggerEvent;

  @Before
  public void setUp() {
    initMocks(this);
  }

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
