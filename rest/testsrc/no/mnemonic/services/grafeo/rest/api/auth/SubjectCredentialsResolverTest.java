package no.mnemonic.services.grafeo.rest.api.auth;

import no.mnemonic.services.grafeo.api.service.v1.RequestHeader;
import no.mnemonic.services.grafeo.auth.properties.model.SubjectCredentials;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.HttpHeaders;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SubjectCredentialsResolverTest {

  @Mock
  private HttpHeaders httpHeaders;

  private CredentialsResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);
    resolver = new SubjectCredentialsResolver(httpHeaders);
  }

  @Test
  public void testActUserIdIsSetInRequestHeader() {
    when(httpHeaders.getHeaderString("ACT-User-ID")).thenReturn("1");

    RequestHeader rh = resolver.getRequestHeader();

    assertNotNull(rh.getCredentials());
    assertTrue(rh.getCredentials() instanceof SubjectCredentials);
    assertEquals(1, ((SubjectCredentials) rh.getCredentials()).getSubjectID());
  }

  @Test
  public void testActUserIdOmittedHeader() {
    assertNull(resolver.getRequestHeader().getCredentials());
  }

  @Test
  public void testActUserIdOmitsNegativeId() {
    when(httpHeaders.getHeaderString("ACT-User-ID")).thenReturn("-1");
    assertNull(resolver.getRequestHeader().getCredentials());
  }
}
