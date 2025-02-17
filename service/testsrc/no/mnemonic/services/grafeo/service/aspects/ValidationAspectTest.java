package no.mnemonic.services.grafeo.service.aspects;

import com.google.inject.Guice;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.request.ValidatingRequest;
import no.mnemonic.services.grafeo.api.service.v1.RequestHeader;
import no.mnemonic.services.grafeo.api.validation.constraints.ServiceNotNull;
import no.mnemonic.services.grafeo.service.Service;
import no.mnemonic.services.grafeo.service.TestSecurityContext;
import no.mnemonic.services.grafeo.service.contexts.SecurityContext;
import org.junit.jupiter.api.Test;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidationAspectTest {

  @Test
  public void testValidRequestExecutesMethod() throws InvalidArgumentException {
    TestService service = createService();
    assertEquals("Called!", service.method(RequestHeader.builder().build(), new TestRequest().setValue(3)));
  }

  @Test
  public void testValidateSimpleRequest() {
    TestService service = createService();

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class,
            () -> service.method(RequestHeader.builder().build(), new TestRequest()));
    assertMinException(ex, "value");
  }

  @Test
  public void testValidateRequestIsNull() {
    TestService service = createService();

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class,
            () -> service.method(RequestHeader.builder().build(), (TestRequest) null));
    assertNotNullRequest(ex);
  }

  @Test
  public void testValidateServiceRequest() {
    TestService service = createService();

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class,
            () -> service.method(RequestHeader.builder().build(), new ServiceTestRequest()));
    assertServiceNotNullException(ex, "value");
  }

  @Test
  public void testValidateNestedRequest() {
    TestService service = createService();

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class,
            () -> service.method(RequestHeader.builder().build(), new NestedTestRequest().setInner(new TestRequest())));
    assertMinException(ex, "inner.value");
  }

  @Test
  public void testValidateNestedRequestIsNull() {
    TestService service = createService();

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class,
            () -> service.method(RequestHeader.builder().build(), new NestedTestRequest()));
    assertNotNullException(ex, "inner");
  }

  private TestService createService() {
    return Guice.createInjector(new ValidationAspect()).getInstance(TestService.class);
  }

  private void assertMinException(InvalidArgumentException ex, String property) {
    assertEquals(1, ex.getValidationErrors().size());
    InvalidArgumentException.ValidationError error = ex.getValidationErrors().iterator().next();
    assertEquals("must be greater than or equal to 1", error.getMessage());
    assertEquals("{jakarta.validation.constraints.Min.message}", error.getMessageTemplate());
    assertEquals(property, error.getProperty());
    assertEquals("0", error.getValue());
  }

  private void assertNotNullException(InvalidArgumentException ex, String property) {
    assertEquals(1, ex.getValidationErrors().size());
    InvalidArgumentException.ValidationError error = ex.getValidationErrors().iterator().next();
    assertEquals("must not be null", error.getMessage());
    assertEquals("{jakarta.validation.constraints.NotNull.message}", error.getMessageTemplate());
    assertEquals(property, error.getProperty());
    assertEquals("NULL", error.getValue());
  }

  private void assertServiceNotNullException(InvalidArgumentException ex, String property) {
    assertEquals(1, ex.getValidationErrors().size());
    InvalidArgumentException.ValidationError error = ex.getValidationErrors().iterator().next();
    assertEquals("must not be null in service layer", error.getMessage());
    assertEquals("{no.mnemonic.services.grafeo.api.validation.constraints.ServiceNotNull.message}", error.getMessageTemplate());
    assertEquals(property, error.getProperty());
    assertEquals("NULL", error.getValue());
  }

  private void assertNotNullRequest(InvalidArgumentException ex) {
    assertEquals(1, ex.getValidationErrors().size());
    InvalidArgumentException.ValidationError error = ex.getValidationErrors().iterator().next();
    assertEquals(InvalidArgumentException.ErrorMessage.NULL.getMessage(), error.getMessage());
    assertEquals(InvalidArgumentException.ErrorMessage.NULL.getMessageTemplate(), error.getMessageTemplate());
    assertEquals("request", error.getProperty());
    assertEquals("NULL", error.getValue());
  }

  private class TestRequest implements ValidatingRequest {
    @Min(1)
    private int value;

    TestRequest setValue(int value) {
      this.value = value;
      return this;
    }
  }

  private class ServiceTestRequest implements ValidatingRequest {
    @ServiceNotNull
    private Object value;
  }

  private class NestedTestRequest implements ValidatingRequest {
    @NotNull
    @Valid
    private TestRequest inner;

    NestedTestRequest setInner(TestRequest inner) {
      this.inner = inner;
      return this;
    }
  }

  static class TestService implements Service {
    String method(RequestHeader rh, TestRequest request) throws InvalidArgumentException {
      return "Called!";
    }

    String method(RequestHeader rh, ServiceTestRequest request) throws InvalidArgumentException {
      return "Called!";
    }

    String method(RequestHeader rh, NestedTestRequest request) throws InvalidArgumentException {
      return "Called!";
    }

    @Override
    public SecurityContext createSecurityContext(Credentials credentials) {
      return new TestSecurityContext();
    }
  }

}
