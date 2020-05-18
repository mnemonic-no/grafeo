package no.mnemonic.act.platform.rest.api.v1;

import com.fasterxml.jackson.databind.JsonNode;
import no.mnemonic.act.platform.api.request.v1.TraverseByObjectIdRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectIdRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectTypeValueRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectsRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.rest.AbstractEndpointTest;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Set;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;
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

  @Test
  public void testTraverseObjectByTypeValue() throws Exception {
    String type = "ip";
    String value = "27.13.4.125";
    when(getTiService().traverse(any(), isA(TraverseGraphByObjectTypeValueRequest.class))).then(i -> {
      TraverseGraphByObjectTypeValueRequest request = i.getArgument(1);
      assertEquals(type, request.getType());
      assertEquals(value, request.getValue());
      return StreamingResultSet.<String>builder().setValues(ListUtils.list("something")).build();
    });

    TraverseGraphByObjectTypeValueRequest request = new TraverseGraphByObjectTypeValueRequest()
            .setQuery("g.values('value')");
    Response response = target(String.format("/v1/traverse/object/%s/%s", type, value)).request().post(Entity.json(request));
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(1, payload.size());
    assertEquals("something", payload.get(0).asText());

    verify(getTiService(), times(1)).traverse(notNull(), isA(TraverseGraphByObjectTypeValueRequest.class));
  }

  @Test
  public void testTraverseObjects() throws Exception {
    Set<String> objects = set(UUID.randomUUID().toString(), "ThreatActor/Sofacy");

    when(getTiService().traverse(any(), isA(TraverseGraphByObjectsRequest.class))).then(i -> {
      TraverseGraphByObjectsRequest request = i.getArgument(1);
      assertEquals(objects, request.getObjects());
      return StreamingResultSet.<String>builder().setValues(ListUtils.list("something")).build();
    });

    TraverseGraphByObjectsRequest request = new TraverseGraphByObjectsRequest()
            .setQuery("g.values('value')")
            .setObjects(objects);;
    Response response = target(String.format("/v1/traverse/objects")).request().post(Entity.json(request));

    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(1, payload.size());
    assertEquals("something", payload.get(0).asText());
  }
}
