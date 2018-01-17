package no.mnemonic.act.platform.test.integration;

import com.fasterxml.jackson.databind.node.ArrayNode;
import no.mnemonic.act.platform.api.request.v1.CreateFactTypeRequest;
import no.mnemonic.act.platform.api.request.v1.Direction;
import no.mnemonic.act.platform.api.request.v1.FactObjectBindingDefinition;
import no.mnemonic.act.platform.api.request.v1.UpdateFactTypeRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FactTypeIT extends AbstractIT {

  @Test
  public void testFetchFactType() throws Exception {
    // Create a FactType in the database ...
    FactTypeEntity entity = createFactType();

    // ... and check that it can be received via the REST API.
    Response response = request("/v1/factType/uuid/" + entity.getId()).get();
    assertEquals(200, response.getStatus());
    assertEquals(entity.getId(), getIdFromModel(getPayload(response)));
  }

  @Test
  public void testSearchFactTypes() throws Exception {
    // Create a FactType in the database ...
    FactTypeEntity entity = createFactType();

    // ... and check that it can be found via the REST API.
    Response response = request("/v1/factType").get();
    assertEquals(200, response.getStatus());
    ArrayNode data = (ArrayNode) getPayload(response);
    assertEquals(1, data.size());
    assertEquals(entity.getId(), getIdFromModel(data.get(0)));
  }

  @Test
  public void testCreateFactType() throws Exception {
    // Create a FactType via the REST API ...
    CreateFactTypeRequest request = new CreateFactTypeRequest()
            .setName("FactType")
            .setEntityHandler("IdentityHandler")
            .setValidator("TrueValidator")
            .addRelevantObjectBinding(new FactObjectBindingDefinition()
                    .setObjectType(createObjectType().getId())
                    .setDirection(Direction.BiDirectional)
            );
    Response response = request("/v1/factType").post(Entity.json(request));
    assertEquals(201, response.getStatus());

    // ... and check that it ends up in the database.
    assertNotNull(getFactManager().getFactType(getIdFromModel(getPayload(response))));
  }

  @Test
  public void testUpdateFactType() throws Exception {
    // Create a FactType in the database ...
    FactTypeEntity entity = createFactType();

    // ... update it via the REST API ...
    UpdateFactTypeRequest request = new UpdateFactTypeRequest().setName("FactTypeUpdated");
    Response response = request("/v1/factType/uuid/" + entity.getId()).put(Entity.json(request));
    assertEquals(200, response.getStatus());

    // ... and check that the update ends up in the database.
    assertEquals(request.getName(), getFactManager().getFactType(entity.getId()).getName());
  }

}
