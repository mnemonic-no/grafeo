package no.mnemonic.services.grafeo.rest.api.v1;

import com.fasterxml.jackson.databind.JsonNode;
import no.mnemonic.services.grafeo.api.model.v1.Fact;
import no.mnemonic.services.grafeo.api.model.v1.Object;
import no.mnemonic.services.grafeo.api.request.v1.GetObjectByIdRequest;
import no.mnemonic.services.grafeo.api.request.v1.GetObjectByTypeValueRequest;
import no.mnemonic.services.grafeo.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.services.grafeo.api.request.v1.SearchObjectRequest;
import no.mnemonic.services.grafeo.api.service.v1.StreamingResultSet;
import no.mnemonic.services.grafeo.rest.AbstractEndpointTest;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

public class ObjectEndpointTest extends AbstractEndpointTest {

  @Test
  public void testGetObjectById() throws Exception {
    UUID id = UUID.randomUUID();
    when(getService().getObject(any(), isA(GetObjectByIdRequest.class))).then(i -> {
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

    verify(getService(), times(1)).getObject(notNull(), isA(GetObjectByIdRequest.class));
  }

  @Test
  public void testGetObjectByTypeValue() throws Exception {
    UUID id = UUID.randomUUID();
    String type = "ip";
    String value = "27.13.4.125";
    when(getService().getObject(any(), isA(GetObjectByTypeValueRequest.class))).then(i -> {
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

    verify(getService(), times(1)).getObject(notNull(), isA(GetObjectByTypeValueRequest.class));
  }

  @Test
  public void testSearchObjectFactsById() throws Exception {
    UUID id = UUID.randomUUID();
    when(getService().searchObjectFacts(any(), isA(SearchObjectFactsRequest.class))).then(i -> {
      assertEquals(id, i.<SearchObjectFactsRequest>getArgument(1).getObjectID());
      return StreamingResultSet.<Fact>builder().setValues(createFacts()).build();
    });

    Response response = target(String.format("/v1/object/uuid/%s/facts", id)).request().post(Entity.json(new SearchObjectFactsRequest()));
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(3, payload.size());

    verify(getService(), times(1)).searchObjectFacts(notNull(), isA(SearchObjectFactsRequest.class));
  }

  @Test
  public void testSearchObjectFactsByTypeValue() throws Exception {
    String type = "ip";
    String value = "27.13.4.125";
    when(getService().searchObjectFacts(any(), isA(SearchObjectFactsRequest.class))).then(i -> {
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

    verify(getService(), times(1)).searchObjectFacts(notNull(), isA(SearchObjectFactsRequest.class));
  }

  @Test
  public void testSearchObjects() throws Exception {
    when(getService().searchObjects(any(), isA(SearchObjectRequest.class))).then(i -> StreamingResultSet.<Object>builder().setValues(createObjects()).build());

    Response response = target("/v1/object/search").request().post(Entity.json(new SearchObjectRequest()));
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(3, payload.size());

    verify(getService(), times(1)).searchObjects(notNull(), isA(SearchObjectRequest.class));
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
