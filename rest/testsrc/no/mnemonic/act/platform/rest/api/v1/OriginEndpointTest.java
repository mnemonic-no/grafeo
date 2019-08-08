package no.mnemonic.act.platform.rest.api.v1;

import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.api.request.v1.GetOriginByIdRequest;
import no.mnemonic.act.platform.rest.AbstractEndpointTest;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

public class OriginEndpointTest extends AbstractEndpointTest {

  @Test
  public void testGetOriginById() throws Exception {
    UUID id = UUID.randomUUID();
    when(getTiService().getOrigin(any(), isA(GetOriginByIdRequest.class))).then(i -> {
      assertEquals(id, i.<GetOriginByIdRequest>getArgument(1).getId());
      return Origin.builder().setId(id).build();
    });

    Response response = target(String.format("/v1/origin/uuid/%s", id)).request().get();
    assertEquals(200, response.getStatus());
    assertEquals(id.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).getOrigin(any(), isA(GetOriginByIdRequest.class));
  }
}
