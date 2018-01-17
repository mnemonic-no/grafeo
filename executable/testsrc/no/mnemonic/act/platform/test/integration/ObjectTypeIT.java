package no.mnemonic.act.platform.test.integration;

import com.fasterxml.jackson.databind.node.ArrayNode;
import no.mnemonic.act.platform.api.request.v1.CreateObjectTypeRequest;
import no.mnemonic.act.platform.api.request.v1.UpdateObjectTypeRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ObjectTypeIT extends AbstractIT {

  @Test
  public void testFetchObjectType() throws Exception {
    // Create an ObjectType in the database ...
    ObjectTypeEntity entity = createObjectType();

    // ... and check that it can be received via the REST API.
    Response response = request("/v1/objectType/uuid/" + entity.getId()).get();
    assertEquals(200, response.getStatus());
    assertEquals(entity.getId(), getIdFromModel(getPayload(response)));
  }

  @Test
  public void testSearchObjectTypes() throws Exception {
    // Create an ObjectType in the database ...
    ObjectTypeEntity entity = createObjectType();

    // ... and check that it can be found via the REST API.
    Response response = request("/v1/objectType").get();
    assertEquals(200, response.getStatus());
    ArrayNode data = (ArrayNode) getPayload(response);
    assertEquals(1, data.size());
    assertEquals(entity.getId(), getIdFromModel(data.get(0)));
  }

  @Test
  public void testCreateObjectType() throws Exception {
    // Create an ObjectType via the REST API ...
    CreateObjectTypeRequest request = new CreateObjectTypeRequest()
            .setName("ObjectType")
            .setEntityHandler("IdentityHandler")
            .setValidator("TrueValidator");
    Response response = request("/v1/objectType").post(Entity.json(request));
    assertEquals(201, response.getStatus());

    // ... and check that it ends up in the database.
    assertNotNull(getObjectManager().getObjectType(getIdFromModel(getPayload(response))));
  }

  @Test
  public void testUpdateObjectType() throws Exception {
    // Create an ObjectType in the database ...
    ObjectTypeEntity entity = createObjectType();

    // ... update it via the REST API ...
    UpdateObjectTypeRequest request = new UpdateObjectTypeRequest().setName("ObjectTypeUpdated");
    Response response = request("/v1/objectType/uuid/" + entity.getId()).put(Entity.json(request));
    assertEquals(200, response.getStatus());

    // ... and check that the update ends up in the database.
    assertEquals(request.getName(), getObjectManager().getObjectType(entity.getId()).getName());
  }

}
