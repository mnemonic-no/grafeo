package no.mnemonic.services.grafeo.rest.api.auth;

import no.mnemonic.services.grafeo.api.service.v1.RequestHeader;
import no.mnemonic.services.grafeo.auth.properties.model.SubjectCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.core.HttpHeaders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SubjectCredentialsResolverTest {

  @Mock
  private HttpHeaders httpHeaders;
  @InjectMocks
  private SubjectCredentialsResolver resolver;

  @Test
  public void testGrafeoUserIdIsSetInRequestHeader() {
    when(httpHeaders.getHeaderString("Grafeo-User-ID")).thenReturn("1");

    RequestHeader rh = resolver.getRequestHeader();

    assertNotNull(rh.getCredentials());
    assertInstanceOf(SubjectCredentials.class, rh.getCredentials());
    assertEquals(1, ((SubjectCredentials) rh.getCredentials()).getSubjectID());
  }

  @Test
  public void testGrafeoUserIdOmitsNegativeId() {
    when(httpHeaders.getHeaderString("Grafeo-User-ID")).thenReturn("-1");
    assertNull(resolver.getRequestHeader().getCredentials());
  }

  @Test
  public void testActUserIdIsSetInRequestHeader() {
    when(httpHeaders.getHeaderString("Grafeo-User-ID")).thenReturn(null);
    when(httpHeaders.getHeaderString("ACT-User-ID")).thenReturn("1");

    RequestHeader rh = resolver.getRequestHeader();

    assertNotNull(rh.getCredentials());
    assertInstanceOf(SubjectCredentials.class, rh.getCredentials());
    assertEquals(1, ((SubjectCredentials) rh.getCredentials()).getSubjectID());
  }

  @Test
  public void testActUserIdOmitsNegativeId() {
    when(httpHeaders.getHeaderString("Grafeo-User-ID")).thenReturn(null);
    when(httpHeaders.getHeaderString("ACT-User-ID")).thenReturn("-1");
    assertNull(resolver.getRequestHeader().getCredentials());
  }

  @Test
  public void testOmittedHeaders() {
    assertNull(resolver.getRequestHeader().getCredentials());
  }
}
