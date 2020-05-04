package no.mnemonic.act.platform.rest.api.v1;

import com.fasterxml.jackson.databind.JsonNode;
import no.mnemonic.act.platform.api.request.v1.TraverseByObjectIdRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectIdRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.rest.AbstractEndpointTest;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TraverseEndpointTest extends AbstractEndpointTest {

  @Test
  public void testTraverseByObjectId() throws Exception {
    UUID id = UUID.randomUUID();
    when(getTiService().traverse(any(), isA(TraverseGraphByObjectIdRequest.class))).then(i -> {
      assertEquals(id, i.<TraverseGraphByObjectIdRequest>getArgument(1).getId());
      return StreamingResultSet.<String>builder().setValues(ListUtils.list("something")).build();
    });

    TraverseByObjectIdRequest request = new TraverseByObjectIdRequest()
            .setQuery("g.values('value')");
    Response response = target(String.format("/v1/traverse/object/%s", id)).request().post(Entity.json(request));
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(1, payload.size());
    assertEquals("something", payload.get(0).asText());

    verify(getTiService(), times(1)).traverse(notNull(), isA(TraverseGraphByObjectIdRequest.class));
  }
}
