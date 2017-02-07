package no.mnemonic.act.platform.service.aspects;

import com.google.inject.Guice;
import no.mnemonic.act.platform.api.service.v1.RequestHeader;
import no.mnemonic.act.platform.service.Service;
import no.mnemonic.act.platform.service.contexts.RequestContext;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import org.junit.Test;

import static org.junit.Assert.*;

public class RequestContextAspectTest {

  @Test
  public void testInjectNewRequestContext() {
    TestService service = createService();

    assertFalse(RequestContext.isSet());
    assertEquals("Called!", service.method(new RequestHeader(), "Called!"));
    assertFalse(RequestContext.isSet());
  }

  @Test
  public void testAlreadyInjectedRequestContext() throws Exception {
    TestService service = createService();

    try (RequestContext ignored = RequestContext.set(new RequestContext())) {
      assertTrue(RequestContext.isSet());
      assertEquals("Called!", service.method(new RequestHeader(), "Called!"));
      assertTrue(RequestContext.isSet());
    }
  }

  private TestService createService() {
    return Guice.createInjector(new RequestContextAspect()).getInstance(TestService.class);
  }

  static class TestService implements Service {
    String method(RequestHeader rh, String something) {
      assertTrue(RequestContext.isSet());
      return something;
    }

    @Override
    public SecurityContext createSecurityContext() {
      return new SecurityContext();
    }

    @Override
    public RequestContext createRequestContext() {
      return new RequestContext();
    }
  }

}
