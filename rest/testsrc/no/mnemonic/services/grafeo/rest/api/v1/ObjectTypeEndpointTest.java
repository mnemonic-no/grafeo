package no.mnemonic.services.grafeo.rest.api.v1;

import com.fasterxml.jackson.databind.JsonNode;
import no.mnemonic.services.grafeo.api.model.v1.ObjectType;
import no.mnemonic.services.grafeo.api.request.v1.CreateObjectTypeRequest;
import no.mnemonic.services.grafeo.api.request.v1.GetObjectTypeByIdRequest;
import no.mnemonic.services.grafeo.api.request.v1.SearchObjectTypeRequest;
import no.mnemonic.services.grafeo.api.request.v1.UpdateObjectTypeRequest;
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

public class ObjectTypeEndpointTest extends AbstractEndpointTest {

  @Test
  public void testGetObjectTypeById() throws Exception {
    UUID id = UUID.randomUUID();
    when(getTiService().getObjectType(any(), isA(GetObjectTypeByIdRequest.class))).then(i -> {
      assertEquals(id, i.<GetObjectTypeByIdRequest>getArgument(1).getId());
      return ObjectType.builder().setId(id).build();
    });

    Response response = target(String.format("/v1/objectType/uuid/%s", id)).request().get();
    assertEquals(200, response.getStatus());
    assertEquals(id.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).getObjectType(notNull(), isA(GetObjectTypeByIdRequest.class));
  }

  @Test
  public void testSearchObjectTypes() throws Exception {
    when(getTiService().searchObjectTypes(any(), isA(SearchObjectTypeRequest.class))).then(i -> StreamingResultSet.<ObjectType>builder().setValues(createObjectTypes()).build());

    Response response = target("/v1/objectType").request().get();
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(3, payload.size());

    verify(getTiService(), times(1)).searchObjectTypes(notNull(), isA(SearchObjectTypeRequest.class));
  }

  @Test
  public void testCreateObjectType() throws Exception {
    UUID id = UUID.randomUUID();
    when(getTiService().createObjectType(any(), isA(CreateObjectTypeRequest.class))).then(i -> ObjectType.builder().setId(id).build());

    Response response = target("/v1/objectType").request().post(Entity.json(createCreateObjectTypeRequest()));
    assertEquals(201, response.getStatus());
    assertEquals(id.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).createObjectType(notNull(), isA(CreateObjectTypeRequest.class));
  }

  @Test
  public void testUpdateObjectType() throws Exception {
    UUID id = UUID.randomUUID();
    when(getTiService().updateObjectType(any(), isA(UpdateObjectTypeRequest.class))).then(i -> {
      assertEquals(id, i.<UpdateObjectTypeRequest>getArgument(1).getId());
      return ObjectType.builder().setId(id).build();
    });

    Response response = target(String.format("/v1/objectType/uuid/%s", id)).request().put(Entity.json(new UpdateObjectTypeRequest()));
    assertEquals(200, response.getStatus());
    assertEquals(id.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).updateObjectType(notNull(), isA(UpdateObjectTypeRequest.class));
  }

  private Collection<ObjectType> createObjectTypes() {
    Collection<ObjectType> types = new HashSet<>();
    for (int i = 0; i < 3; i++) {
      types.add(ObjectType.builder().setId(UUID.randomUUID()).build());
    }
    return types;
  }

  private CreateObjectTypeRequest createCreateObjectTypeRequest() {
    return new CreateObjectTypeRequest()
            .setName("name")
            .setValidator("validator");
  }

}
