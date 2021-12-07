package no.mnemonic.act.platform.service.aspects;

import com.datastax.oss.driver.api.core.DriverTimeoutException;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.UnexpectedAuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.UnhandledRuntimeException;
import no.mnemonic.act.platform.api.service.v1.RequestHeader;
import no.mnemonic.act.platform.service.Service;
import no.mnemonic.act.platform.service.TestSecurityContext;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.services.common.api.ServiceTimeOutException;
import no.mnemonic.services.common.auth.model.Credentials;
import org.junit.Test;

import static org.junit.Assert.assertThrows;

public class RuntimeExceptionHandlerAspectTest {

  @Test
  public void testReplaceUnknownRuntimeException() {
    TestService service = createService();

    assertThrows(UnhandledRuntimeException.class, () -> service.method(RequestHeader.builder().build(), new IllegalArgumentException("test")));
    assertThrows(UnhandledRuntimeException.class, () -> service.method(RequestHeader.builder().build(), new RuntimeException("test")));
  }

  @Test
  public void testReplaceCassandraClientTimeout() {
    TestService service = createService();

    assertThrows(ServiceTimeOutException.class, () -> service.method(RequestHeader.builder().build(), new DriverTimeoutException("test")));
  }

  @Test
  public void testDontReplaceKnownRuntimeException() {
    TestService service = createService();

    assertThrows(UnexpectedAuthenticationFailedException.class, () -> service.method(RequestHeader.builder().build(), new UnexpectedAuthenticationFailedException("test")));
    assertThrows(UnhandledRuntimeException.class, () -> service.method(RequestHeader.builder().build(), new UnhandledRuntimeException("test")));
    assertThrows(ServiceTimeOutException.class, () -> service.method(RequestHeader.builder().build(), new ServiceTimeOutException("test")));
  }

  @Test
  public void testDontReplaceCheckedException() {
    TestService service = createService();

    assertThrows(AccessDeniedException.class, () -> service.method(RequestHeader.builder().build(), new AccessDeniedException("test")));
    assertThrows(Exception.class, () -> service.method(RequestHeader.builder().build(), new Exception("test")));
  }

  private TestService createService() {
    return Guice.createInjector(new TestModule()).getInstance(TestService.class);
  }

  private static class TestModule extends AbstractModule {
    @Override
    protected void configure() {
      install(new RuntimeExceptionHandlerAspect());
    }
  }

  static class TestService implements Service {
    void method(RequestHeader rh, Exception exception) throws Exception {
      throw exception;
    }

    @Override
    public SecurityContext createSecurityContext(Credentials credentials) {
      return new TestSecurityContext();
    }
  }
}
