package no.mnemonic.services.grafeo.rest.api.v1;

import com.fasterxml.jackson.databind.JsonNode;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.model.v1.Origin;
import no.mnemonic.services.grafeo.api.request.v1.*;
import no.mnemonic.services.grafeo.api.service.v1.StreamingResultSet;
import no.mnemonic.services.grafeo.rest.AbstractEndpointTest;
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

    verify(getTiService(), times(1)).getOrigin(notNull(), isA(GetOriginByIdRequest.class));
  }

  @Test
  public void testSearchOrigins() throws Exception {
    when(getTiService().searchOrigins(any(), isA(SearchOriginRequest.class))).then(i -> {
      SearchOriginRequest request = i.getArgument(1);
      assertEquals(SetUtils.set(SearchOriginRequest.Type.Group), request.getType());
      assertTrue(request.getIncludeDeleted());
      assertEquals(25, (int) request.getLimit());
      return StreamingResultSet.<Origin>builder().setValues(createOrigins()).build();
    });

    Response response = target("/v1/origin")
            .queryParam("type", "Group")
            .queryParam("includeDeleted", true)
            .queryParam("limit", 25)
            .request()
            .get();
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(3, payload.size());

    verify(getTiService(), times(1)).searchOrigins(notNull(), isA(SearchOriginRequest.class));
  }

  @Test
  public void testCreateOrigin() throws Exception {
    UUID id = UUID.randomUUID();
    when(getTiService().createOrigin(any(), isA(CreateOriginRequest.class))).then(i -> Origin.builder().setId(id).build());

    Response response = target("/v1/origin").request().post(Entity.json(new CreateOriginRequest().setName("name")));
    assertEquals(201, response.getStatus());
    assertEquals(id.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).createOrigin(notNull(), isA(CreateOriginRequest.class));
  }

  @Test
  public void testUpdateOrigin() throws Exception {
    UUID id = UUID.randomUUID();
    when(getTiService().updateOrigin(any(), isA(UpdateOriginRequest.class))).then(i -> {
      assertEquals(id, i.<UpdateOriginRequest>getArgument(1).getId());
      return Origin.builder().setId(id).build();
    });

    Response response = target(String.format("/v1/origin/uuid/%s", id)).request().put(Entity.json(new UpdateOriginRequest()));
    assertEquals(200, response.getStatus());
    assertEquals(id.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).updateOrigin(notNull(), isA(UpdateOriginRequest.class));
  }

  @Test
  public void testDeleteOrigin() throws Exception {
    UUID id = UUID.randomUUID();
    when(getTiService().deleteOrigin(any(), isA(DeleteOriginRequest.class))).then(i -> {
      assertEquals(id, i.<DeleteOriginRequest>getArgument(1).getId());
      return Origin.builder().setId(id).build();
    });

    Response response = target(String.format("/v1/origin/uuid/%s", id)).request().delete();
    assertEquals(200, response.getStatus());
    assertEquals(id.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).deleteOrigin(notNull(), isA(DeleteOriginRequest.class));
  }

  private Collection<Origin> createOrigins() {
    Collection<Origin> origins = new HashSet<>();
    for (int i = 0; i < 3; i++) {
      origins.add(Origin.builder().setId(UUID.randomUUID()).build());
    }
    return origins;
  }
}
