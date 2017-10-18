package no.mnemonic.act.platform.rest;

import no.mnemonic.act.platform.api.request.v1.GetObjectByTypeValueRequest;
import no.mnemonic.act.platform.auth.properties.model.SubjectCredentials;
import no.mnemonic.act.platform.auth.properties.model.SubjectIdentifier;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;

/**
 * Test common endpoint functionality with one specific endpoint, however, it should work with every endpoint.
 */
public class CommonEndpointTest extends AbstractEndpointTest {

  @Test
  public void testActUserIdIsSetInRequestHeader() throws Exception {
    target("/v1/object/ip/1.1.1.1").request().header("ACT-User-ID", 1).get();

    verify(getTiService()).getObject(argThat(rh -> {
      assertNotNull(rh.getCredentials());
      assertTrue(rh.getCredentials() instanceof SubjectCredentials);
      SubjectCredentials credentials = SubjectCredentials.class.cast(rh.getCredentials());
      SubjectIdentifier identifier = SubjectIdentifier.class.cast(credentials.getUserID());
      assertEquals(1, identifier.getInternalID());
      return true;
    }), isA(GetObjectByTypeValueRequest.class));
  }

  @Test
  public void testActUserIdOmittedHeader() throws Exception {
    target("/v1/object/ip/1.1.1.1").request().get();

    verify(getTiService()).getObject(argThat(rh -> {
      assertNull(rh.getCredentials());
      return true;
    }), isA(GetObjectByTypeValueRequest.class));
  }

  @Test
  public void testActUserIdOmitsNegativeId() throws Exception {
    target("/v1/object/ip/1.1.1.1").request().header("ACT-User-ID", -1).get();

    verify(getTiService()).getObject(argThat(rh -> {
      assertNull(rh.getCredentials());
      return true;
    }), isA(GetObjectByTypeValueRequest.class));
  }

}
