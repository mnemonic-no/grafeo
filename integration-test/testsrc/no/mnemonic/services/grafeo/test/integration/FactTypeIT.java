package no.mnemonic.services.grafeo.test.integration;

import com.fasterxml.jackson.databind.node.ArrayNode;
import no.mnemonic.services.grafeo.api.request.v1.CreateFactTypeRequest;
import no.mnemonic.services.grafeo.api.request.v1.FactObjectBindingDefinition;
import no.mnemonic.services.grafeo.api.request.v1.MetaFactBindingDefinition;
import no.mnemonic.services.grafeo.api.request.v1.UpdateFactTypeRequest;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
            .setValidator("TrueValidator")
            .addRelevantObjectBinding(new FactObjectBindingDefinition()
                    .setSourceObjectType(createObjectType().getId())
                    .setBidirectionalBinding(true)
            );
    Response response = request("/v1/factType").post(Entity.json(request));
    assertEquals(201, response.getStatus());

    // ... and check that it ends up in the database.
    assertNotNull(getFactManager().getFactType(getIdFromModel(getPayload(response))));
  }

  @Test
  public void testCreateMetaFactType() throws Exception {
    // Create a FactType via the REST API ...
    CreateFactTypeRequest request = new CreateFactTypeRequest()
            .setName("MetaFactType")
            .setValidator("TrueValidator")
            .addRelevantFactBinding(new MetaFactBindingDefinition()
                    .setFactType(createFactType().getId())
            );
    Response response = request("/v1/factType").post(Entity.json(request));
    assertEquals(201, response.getStatus());

    // ... and check that it ends up in the database.
    assertNotNull(getFactManager().getFactType(getIdFromModel(getPayload(response))));
  }

  @Test
  public void testUpdateFactType() {
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
