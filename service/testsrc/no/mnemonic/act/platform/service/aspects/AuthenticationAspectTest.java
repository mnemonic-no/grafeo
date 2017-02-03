package no.mnemonic.act.platform.service.aspects;

import com.google.inject.Guice;
import no.mnemonic.act.platform.api.service.v1.RequestHeader;
import no.mnemonic.act.platform.service.Service;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import org.junit.Test;

import static org.junit.Assert.*;

public class AuthenticationAspectTest {

  @Test
  public void testInjectNewSecurityContext() {
    TestService service = createService();

    assertFalse(SecurityContext.isSet());
    assertEquals("Called!", service.method(new RequestHeader(), "Called!"));
    assertFalse(SecurityContext.isSet());
  }

  @Test
  public void testAlreadyInjectedSecurityContext() throws Exception {
    TestService service = createService();

    try (SecurityContext ignored = SecurityContext.set(new SecurityContext())) {
      assertTrue(SecurityContext.isSet());
      assertEquals("Called!", service.method(new RequestHeader(), "Called!"));
      assertTrue(SecurityContext.isSet());
    }
  }

  private TestService createService() {
    return Guice.createInjector(new AuthenticationAspect()).getInstance(TestService.class);
  }

  static class TestService implements Service {
    String method(RequestHeader rh, String something) {
      assertTrue(SecurityContext.isSet());
      return something;
    }

    @Override
    public SecurityContext createSecurityContext() {
      return new SecurityContext();
    }
  }

}
