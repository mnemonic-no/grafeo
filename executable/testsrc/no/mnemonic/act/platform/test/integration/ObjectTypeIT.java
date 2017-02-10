package no.mnemonic.act.platform.test.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import no.mnemonic.act.platform.api.request.v1.CreateObjectTypeRequest;
import no.mnemonic.act.platform.api.request.v1.UpdateObjectTypeRequest;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ObjectTypeIT extends AbstractIT {

  @Test
  public void testFetchObjectType() throws Exception {
    // Create an ObjectType in the database ...
    ObjectTypeEntity entity = createObjectTypeEntity();
    getObjectManager().saveObjectType(entity);

    // ... and check that it can be received via the REST API.
    Response response = target("/v1/objectType/uuid/" + entity.getId()).request().get();
    assertEquals(200, response.getStatus());
    assertObjectType(entity, getPayload(response));
  }

  @Test
  public void testSearchObjectTypes() throws Exception {
    // Create an ObjectType in the database ...
    ObjectTypeEntity entity = createObjectTypeEntity();
    getObjectManager().saveObjectType(entity);

    // ... and check that it can be found via the REST API.
    Response response = target("/v1/objectType").request().get();
    assertEquals(200, response.getStatus());
    ArrayNode data = (ArrayNode) getPayload(response);
    assertEquals(1, data.size());
    assertObjectType(entity, data.get(0));
  }

  @Test
  public void testCreateObjectType() throws Exception {
    // Create an ObjectType via the REST API ...
    CreateObjectTypeRequest request = new CreateObjectTypeRequest()
            .setName("ObjectType")
            .setEntityHandler("IdentityHandler")
            .setValidator("TrueValidator");
    Response response = target("/v1/objectType").request().post(Entity.json(request));
    assertEquals(201, response.getStatus());
    UUID id = UUID.fromString(getPayload(response).get("id").textValue());

    // ... and check that it ends up in the database.
    ObjectTypeEntity entity = getObjectManager().getObjectType(id);
    assertNotNull(entity);
    assertEquals(id, entity.getId());
    assertEquals(request.getName(), entity.getName());
    assertEquals(request.getEntityHandler(), entity.getEntityHandler());
    assertEquals(request.getValidator(), entity.getValidator());
  }

  @Test
  public void testUpdateObjectType() throws Exception {
    // Create an ObjectType in the database ...
    ObjectTypeEntity entity = createObjectTypeEntity();
    getObjectManager().saveObjectType(entity);

    // ... update it via the REST API ...
    UpdateObjectTypeRequest request = new UpdateObjectTypeRequest().setName("ObjectTypeUpdated");
    Response response = target("/v1/objectType/uuid/" + entity.getId()).request().put(Entity.json(request));
    assertEquals(200, response.getStatus());

    // ... and check that the update ends up in the database.
    ObjectTypeEntity updatedEntity = getObjectManager().getObjectType(entity.getId());
    assertEquals(request.getName(), updatedEntity.getName());
  }

  private ObjectTypeEntity createObjectTypeEntity() {
    return new ObjectTypeEntity()
            .setId(UUID.randomUUID())
            .setName("ObjectType")
            .setEntityHandler("IdentityHandler")
            .setValidator("TrueValidator");
  }

  private void assertObjectType(ObjectTypeEntity entity, JsonNode type) {
    assertEquals(entity.getId().toString(), type.get("id").textValue());
    assertEquals(entity.getName(), type.get("name").textValue());
    assertEquals(entity.getEntityHandler(), type.get("entityHandler").textValue());
    assertEquals(entity.getValidator(), type.get("validator").textValue());
  }

}
