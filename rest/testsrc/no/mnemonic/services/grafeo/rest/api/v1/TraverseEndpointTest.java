package no.mnemonic.services.grafeo.rest.api.v1;

import com.fasterxml.jackson.databind.JsonNode;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.grafeo.api.request.v1.SearchObjectRequest;
import no.mnemonic.services.grafeo.api.request.v1.TraverseGraphByObjectSearchRequest;
import no.mnemonic.services.grafeo.api.request.v1.TraverseGraphByObjectsRequest;
import no.mnemonic.services.grafeo.api.request.v1.TraverseGraphRequest;
import no.mnemonic.services.grafeo.api.service.v1.StreamingResultSet;
import no.mnemonic.services.grafeo.rest.AbstractEndpointTest;
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
    Long start = 1L;
    Long end = 2L;
    when(getService().traverse(any(), isA(TraverseGraphByObjectsRequest.class))).then(i -> {
      TraverseGraphByObjectsRequest request = i.getArgument(1);
      assertEquals(set(id.toString()), request.getObjects());
      assertEquals(start, request.getStartTimestamp());
      assertEquals(end, request.getEndTimestamp());
      assertTrue(request.getIncludeRetracted());
      return StreamingResultSet.<String>builder().setValues(ListUtils.list("something")).build();
    });

    TraverseGraphRequest request = new TraverseGraphRequest()
            .setStartTimestamp(start)
            .setEndTimestamp(end)
            .setIncludeRetracted(true)
            .setQuery("g.values('value')");
    Response response = target(String.format("/v1/traverse/object/%s", id)).request().post(Entity.json(request));
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(1, payload.size());
    assertEquals("something", payload.get(0).asText());

    verify(getService(), times(1)).traverse(notNull(), isA(TraverseGraphByObjectsRequest.class));
  }

  @Test
  public void testTraverseObjectByTypeValue() throws Exception {
    String type = "ip";
    String value = "27.13.4.125";
    Long start = 1L;
    Long end = 2L;

    when(getService().traverse(any(), isA(TraverseGraphByObjectsRequest.class))).then(i -> {
      TraverseGraphByObjectsRequest request = i.getArgument(1);
      assertEquals(set("ip/27.13.4.125"), request.getObjects());
      assertEquals(start, request.getStartTimestamp());
      assertEquals(end, request.getEndTimestamp());
      assertTrue(request.getIncludeRetracted());
      return StreamingResultSet.<String>builder().setValues(ListUtils.list("something")).build();
    });

    TraverseGraphRequest request = new TraverseGraphRequest()
            .setQuery("g.values('value')")
            .setStartTimestamp(start)
            .setEndTimestamp(end)
            .setIncludeRetracted(true);
    Response response = target(String.format("/v1/traverse/object/%s/%s", type, value)).request().post(Entity.json(request));
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(1, payload.size());
    assertEquals("something", payload.get(0).asText());

    verify(getService(), times(1)).traverse(notNull(), isA(TraverseGraphByObjectsRequest.class));
  }

  @Test
  public void testTraverseObjects() throws Exception {
    Set<String> objects = set(UUID.randomUUID().toString(), "ThreatActor/Sofacy");

    when(getService().traverse(any(), isA(TraverseGraphByObjectsRequest.class))).then(i -> {
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

    verify(getService(), times(1)).traverse(notNull(), isA(TraverseGraphByObjectsRequest.class));
  }

  @Test
  public void testTraverseObjectSearch() throws Exception {
    when(getService().traverse(any(), isA(TraverseGraphByObjectSearchRequest.class)))
            .then(i -> StreamingResultSet.<String>builder().setValues(ListUtils.list("something")).build());

    TraverseGraphByObjectSearchRequest request = new TraverseGraphByObjectSearchRequest()
            .setSearch(new SearchObjectRequest())
            .setTraverse(new TraverseGraphRequest().setQuery("g.values('value')"));

    Response response = target("/v1/traverse/objects/search").request().post(Entity.json(request));
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(1, payload.size());
    assertEquals("something", payload.get(0).asText());

    verify(getService(), times(1)).traverse(notNull(), isA(TraverseGraphByObjectSearchRequest.class));
  }
}
