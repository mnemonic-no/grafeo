package no.mnemonic.act.platform.service.aspects;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.service.v1.RequestHeader;
import no.mnemonic.act.platform.service.Service;
import no.mnemonic.act.platform.service.TestSecurityContext;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@SuppressWarnings("unchecked")
public class AuthenticationAspectTest {

  @Mock
  private AccessController accessController;
  @Mock
  private Credentials credentials;

  private RequestHeader rh;

  @Before
  public void setUp() {
    initMocks(this);

    rh = RequestHeader.builder()
            .setCredentials(credentials)
            .build();
  }

  @Test
  public void testInjectNewSecurityContext() {
    TestService service = createService();

    assertFalse(SecurityContext.isSet());
    assertEquals("Called!", service.method(rh, "Called!"));
    assertFalse(SecurityContext.isSet());
  }

  @Test
  public void testAlreadyInjectedSecurityContext() throws Exception {
    TestService service = createService();

    try (SecurityContext ignored = SecurityContext.set(new TestSecurityContext())) {
      assertTrue(SecurityContext.isSet());
      assertEquals("Called!", service.method(rh, "Called!"));
      assertTrue(SecurityContext.isSet());
    }
  }

  @Test
  public void testValidateCredentials() throws Exception {
    createService().method(rh, "Called!");
    verify(accessController).validate(credentials);
  }

  @Test(expected = AuthenticationFailedException.class)
  public void testInvalidCredentialsThrowsAuthenticationFailed() throws Exception {
    doThrow(InvalidCredentialsException.class).when(accessController).validate(credentials);
    createService().method(rh, "Called!");
  }

  private TestService createService() {
    return Guice.createInjector(new TestModule()).getInstance(TestService.class);
  }

  private class TestModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(AccessController.class).toInstance(accessController);
      install(new AuthenticationAspect());
    }
  }

  static class TestService implements Service {
    String method(RequestHeader rh, String something) {
      assertTrue(SecurityContext.isSet());
      return something;
    }

    @Override
    public SecurityContext createSecurityContext(Credentials credentials) {
      assertNotNull(credentials);
      return new TestSecurityContext();
    }
  }
}
