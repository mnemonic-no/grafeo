package no.mnemonic.act.platform.rest.api.v1;

import com.fasterxml.jackson.databind.JsonNode;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.request.v1.*;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.rest.AbstractEndpointTest;
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

public class FactTypeEndpointTest extends AbstractEndpointTest {

  @Test
  public void testGetFactTypeById() throws Exception {
    UUID id = UUID.randomUUID();
    when(getTiService().getFactType(any(), isA(GetFactTypeByIdRequest.class))).then(i -> {
      assertEquals(id, i.<GetFactTypeByIdRequest>getArgument(1).getId());
      return FactType.builder().setId(id).build();
    });

    Response response = target(String.format("/v1/factType/uuid/%s", id)).request().get();
    assertEquals(200, response.getStatus());
    assertEquals(id.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).getFactType(any(), isA(GetFactTypeByIdRequest.class));
  }

  @Test
  public void testSearchFactTypes() throws Exception {
    when(getTiService().searchFactTypes(any(), isA(SearchFactTypeRequest.class))).then(i -> ResultSet.<FactType>builder().setValues(createFactTypes()).build());

    Response response = target("/v1/factType").request().get();
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(3, payload.size());

    verify(getTiService(), times(1)).searchFactTypes(any(), isA(SearchFactTypeRequest.class));
  }

  @Test
  public void testCreateFactType() throws Exception {
    UUID id = UUID.randomUUID();
    when(getTiService().createFactType(any(), isA(CreateFactTypeRequest.class))).then(i -> FactType.builder().setId(id).build());

    Response response = target("/v1/factType").request().post(Entity.json(createCreateFactTypeRequest()));
    assertEquals(201, response.getStatus());
    assertEquals(id.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).createFactType(any(), isA(CreateFactTypeRequest.class));
  }

  @Test
  public void testUpdateFactType() throws Exception {
    UUID id = UUID.randomUUID();
    when(getTiService().updateFactType(any(), isA(UpdateFactTypeRequest.class))).then(i -> {
      assertEquals(id, i.<UpdateFactTypeRequest>getArgument(1).getId());
      return FactType.builder().setId(id).build();
    });

    Response response = target(String.format("/v1/factType/uuid/%s", id)).request().put(Entity.json(new UpdateFactTypeRequest()));
    assertEquals(200, response.getStatus());
    assertEquals(id.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).updateFactType(any(), isA(UpdateFactTypeRequest.class));
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
            .setValidator("validator")
            .addRelevantObjectBinding(new FactObjectBindingDefinition()
                    .setSourceObjectType(UUID.randomUUID())
                    .setDestinationObjectType(UUID.randomUUID())
            );
  }

}
