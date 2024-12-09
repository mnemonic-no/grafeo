package no.mnemonic.services.grafeo.service.aspects;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.service.v1.RequestHeader;
import no.mnemonic.services.grafeo.service.Service;
import no.mnemonic.services.grafeo.service.TestSecurityContext;
import no.mnemonic.services.grafeo.service.contexts.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class AuthenticationAspectTest {

  @Mock
  private AccessController accessController;
  @Mock
  private Credentials credentials;

  private RequestHeader rh;

  @BeforeEach
  public void setUp() {
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

  @Test
  public void testInvalidCredentialsThrowsAuthenticationFailed() throws Exception {
    doThrow(InvalidCredentialsException.class).when(accessController).validate(credentials);
    assertThrows(AuthenticationFailedException.class, () -> createService().method(rh, "Called!"));
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
