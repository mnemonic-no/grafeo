package no.mnemonic.act.platform.rest.api.v1;

import com.fasterxml.jackson.databind.JsonNode;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.request.v1.*;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.rest.AbstractEndpointTest;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

public class ObjectEndpointTest extends AbstractEndpointTest {

  @Test
  public void testGetObjectById() throws Exception {
    UUID id = UUID.randomUUID();
    when(getTiService().getObject(any(), isA(GetObjectByIdRequest.class))).then(i -> {
      GetObjectByIdRequest request = i.getArgument(1);
      assertEquals(id, request.getId());
      assertEquals(1480520820000L, (long) request.getBefore());
      assertEquals(1480520821000L, (long) request.getAfter());
      return Object.builder().setId(id).build();
    });

    Response response = target(String.format("/v1/object/uuid/%s", id))
            .queryParam("before", "2016-11-30T15:47:00Z")
            .queryParam("after", "2016-11-30T15:47:01Z")
            .request()
            .get();
    assertEquals(200, response.getStatus());
    assertEquals(id.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).getObject(notNull(), isA(GetObjectByIdRequest.class));
  }

  @Test
  public void testGetObjectByTypeValue() throws Exception {
    UUID id = UUID.randomUUID();
    String type = "ip";
    String value = "27.13.4.125";
    when(getTiService().getObject(any(), isA(GetObjectByTypeValueRequest.class))).then(i -> {
      GetObjectByTypeValueRequest request = i.getArgument(1);
      assertEquals(type, request.getType());
      assertEquals(value, request.getValue());
      assertEquals(1480520820000L, (long) request.getBefore());
      assertEquals(1480520821000L, (long) request.getAfter());
      return Object.builder().setId(id).build();
    });

    Response response = target(String.format("/v1/object/%s/%s", type, value))
            .queryParam("before", "2016-11-30T15:47:00Z")
            .queryParam("after", "2016-11-30T15:47:01Z")
            .request()
            .get();
    assertEquals(200, response.getStatus());
    assertEquals(id.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).getObject(notNull(), isA(GetObjectByTypeValueRequest.class));
  }

  @Test
  public void testSearchObjectFactsById() throws Exception {
    UUID id = UUID.randomUUID();
    when(getTiService().searchObjectFacts(any(), isA(SearchObjectFactsRequest.class))).then(i -> {
      assertEquals(id, i.<SearchObjectFactsRequest>getArgument(1).getObjectID());
      return StreamingResultSet.<Fact>builder().setValues(createFacts()).build();
    });

    Response response = target(String.format("/v1/object/uuid/%s/facts", id)).request().post(Entity.json(new SearchObjectFactsRequest()));
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(3, payload.size());

    verify(getTiService(), times(1)).searchObjectFacts(notNull(), isA(SearchObjectFactsRequest.class));
  }

  @Test
  public void testSearchObjectFactsByTypeValue() throws Exception {
    String type = "ip";
    String value = "27.13.4.125";
    when(getTiService().searchObjectFacts(any(), isA(SearchObjectFactsRequest.class))).then(i -> {
      SearchObjectFactsRequest request = i.getArgument(1);
      assertEquals(type, request.getObjectType());
      assertEquals(value, request.getObjectValue());
      return StreamingResultSet.<Fact>builder().setValues(createFacts()).build();
    });

    Response response = target(String.format("/v1/object/%s/%s/facts", type, value)).request().post(Entity.json(new SearchObjectFactsRequest()));
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(3, payload.size());

    verify(getTiService(), times(1)).searchObjectFacts(notNull(), isA(SearchObjectFactsRequest.class));
  }

  @Test
  public void testTraverseObjectById() throws Exception {
    UUID id = UUID.randomUUID();
    when(getTiService().traverseGraph(any(), isA(TraverseByObjectIdRequest.class))).then(i -> {
      assertEquals(id, i.<TraverseByObjectIdRequest>getArgument(1).getId());
      return StreamingResultSet.<String>builder().setValues(ListUtils.list("something")).build();
    });

    TraverseByObjectIdRequest request = new TraverseByObjectIdRequest()
            .setQuery("g.values('value')");
    Response response = target(String.format("/v1/object/uuid/%s/traverse", id)).request().post(Entity.json(request));
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(1, payload.size());
    assertEquals("something", payload.get(0).asText());

    verify(getTiService(), times(1)).traverseGraph(notNull(), isA(TraverseByObjectIdRequest.class));
  }

  @Test
  public void testTraverseObjectByTypeValue() throws Exception {
    String type = "ip";
    String value = "27.13.4.125";
    when(getTiService().traverseGraph(any(), isA(TraverseByObjectTypeValueRequest.class))).then(i -> {
      TraverseByObjectTypeValueRequest request = i.getArgument(1);
      assertEquals(type, request.getType());
      assertEquals(value, request.getValue());
      return StreamingResultSet.<String>builder().setValues(ListUtils.list("something")).build();
    });

    TraverseByObjectTypeValueRequest request = new TraverseByObjectTypeValueRequest()
            .setQuery("g.values('value')");
    Response response = target(String.format("/v1/object/%s/%s/traverse", type, value)).request().post(Entity.json(request));
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(1, payload.size());
    assertEquals("something", payload.get(0).asText());

    verify(getTiService(), times(1)).traverseGraph(notNull(), isA(TraverseByObjectTypeValueRequest.class));
  }

  @Test
  public void testSearchObjects() throws Exception {
    when(getTiService().searchObjects(any(), isA(SearchObjectRequest.class))).then(i -> StreamingResultSet.<Object>builder().setValues(createObjects()).build());

    Response response = target("/v1/object/search").request().post(Entity.json(new SearchObjectRequest()));
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(3, payload.size());

    verify(getTiService(), times(1)).searchObjects(notNull(), isA(SearchObjectRequest.class));
  }

  @Test
  public void testTraverseByObjectSearch() throws Exception {
    when(getTiService().traverseGraph(any(), isA(TraverseByObjectSearchRequest.class)))
            .then(i -> StreamingResultSet.<String>builder().setValues(ListUtils.list("something")).build());

    TraverseByObjectSearchRequest request = new TraverseByObjectSearchRequest()
            .setQuery("g.values('value')");
    Response response = target("/v1/object/traverse").request().post(Entity.json(request));
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(1, payload.size());
    assertEquals("something", payload.get(0).asText());

    verify(getTiService(), times(1)).traverseGraph(notNull(), isA(TraverseByObjectSearchRequest.class));
  }

  private Collection<Fact> createFacts() {
    Collection<Fact> facts = new HashSet<>();
    for (int i = 0; i < 3; i++) {
      facts.add(Fact.builder().setId(UUID.randomUUID()).build());
    }
    return facts;
  }

  private Collection<Object> createObjects() {
    Collection<Object> objects = new HashSet<>();
    for (int i = 0; i < 3; i++) {
      objects.add(Object.builder().setId(UUID.randomUUID()).build());
    }
    return objects;
  }

}
