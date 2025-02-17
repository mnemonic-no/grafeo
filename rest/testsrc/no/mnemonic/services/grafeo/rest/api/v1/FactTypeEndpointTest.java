package no.mnemonic.services.grafeo.rest.api.v1;

import com.fasterxml.jackson.databind.JsonNode;
import no.mnemonic.services.grafeo.api.model.v1.FactType;
import no.mnemonic.services.grafeo.api.request.v1.CreateFactTypeRequest;
import no.mnemonic.services.grafeo.api.request.v1.GetFactTypeByIdRequest;
import no.mnemonic.services.grafeo.api.request.v1.SearchFactTypeRequest;
import no.mnemonic.services.grafeo.api.request.v1.UpdateFactTypeRequest;
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

public class FactTypeEndpointTest extends AbstractEndpointTest {

  @Test
  public void testGetFactTypeById() throws Exception {
    UUID id = UUID.randomUUID();
    when(getService().getFactType(any(), isA(GetFactTypeByIdRequest.class))).then(i -> {
      assertEquals(id, i.<GetFactTypeByIdRequest>getArgument(1).getId());
      return FactType.builder().setId(id).build();
    });

    Response response = target(String.format("/v1/factType/uuid/%s", id)).request().get();
    assertEquals(200, response.getStatus());
    assertEquals(id.toString(), getPayload(response).get("id").textValue());

    verify(getService(), times(1)).getFactType(notNull(), isA(GetFactTypeByIdRequest.class));
  }

  @Test
  public void testSearchFactTypes() throws Exception {
    when(getService().searchFactTypes(any(), isA(SearchFactTypeRequest.class))).then(i -> StreamingResultSet.<FactType>builder().setValues(createFactTypes()).build());

    Response response = target("/v1/factType").request().get();
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(3, payload.size());

    verify(getService(), times(1)).searchFactTypes(notNull(), isA(SearchFactTypeRequest.class));
  }

  @Test
  public void testCreateFactType() throws Exception {
    UUID id = UUID.randomUUID();
    when(getService().createFactType(any(), isA(CreateFactTypeRequest.class))).then(i -> FactType.builder().setId(id).build());

    Response response = target("/v1/factType").request().post(Entity.json(createCreateFactTypeRequest()));
    assertEquals(201, response.getStatus());
    assertEquals(id.toString(), getPayload(response).get("id").textValue());

    verify(getService(), times(1)).createFactType(notNull(), isA(CreateFactTypeRequest.class));
  }

  @Test
  public void testUpdateFactType() throws Exception {
    UUID id = UUID.randomUUID();
    when(getService().updateFactType(any(), isA(UpdateFactTypeRequest.class))).then(i -> {
      assertEquals(id, i.<UpdateFactTypeRequest>getArgument(1).getId());
      return FactType.builder().setId(id).build();
    });

    Response response = target(String.format("/v1/factType/uuid/%s", id)).request().put(Entity.json(new UpdateFactTypeRequest()));
    assertEquals(200, response.getStatus());
    assertEquals(id.toString(), getPayload(response).get("id").textValue());

    verify(getService(), times(1)).updateFactType(notNull(), isA(UpdateFactTypeRequest.class));
  }

  private Collection<FactType> createFactTypes() {
    Collection<FactType> types = new HashSet<>();
    for (int i = 0; i < 3; i++) {
      types.add(FactType.builder().setId(UUID.randomUUID()).build());
    }
    return types;
  }

  private CreateFactTypeRequest createCreateFactTypeRequest() {
    return new CreateFactTypeRequest()
            .setName("name")
            .setValidator("validator");
  }

}
