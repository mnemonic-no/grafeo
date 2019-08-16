package no.mnemonic.act.platform.test.integration;

import no.mnemonic.act.platform.api.request.v1.CreateOriginRequest;
import no.mnemonic.act.platform.api.request.v1.UpdateOriginRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.Assert.*;

public class OriginIT extends AbstractIT {

  @Test
  public void testFetchOrigin() throws Exception {
    // Create an Origin in the database ...
    OriginEntity entity = createOrigin();

    // ... and check that it can be received via the REST API.
    fetchAndAssertSingle("/v1/origin/uuid/" + entity.getId(), entity.getId());
  }

  @Test
  public void testSearchOrigins() throws Exception {
    // Create an Origin in the database ...
    OriginEntity entity = createOrigin();

    // ... and check that it can be found via the REST API.
    fetchAndAssertList("/v1/origin", entity.getId());
  }

  @Test
  public void testCreateOrigin() throws Exception {
    // Create an Origin via the REST API ...
    CreateOriginRequest request = new CreateOriginRequest()
            .setName("origin");
    Response response = request("/v1/origin").post(Entity.json(request));
    assertEquals(201, response.getStatus());

    // ... and check that it ends up in the database.
    assertNotNull(getOriginManager().getOrigin(getIdFromModel(getPayload(response))));
  }

  @Test
  public void testUpdateOrigin() {
    // Create an Origin in the database ...
    OriginEntity entity = createOrigin();

    // ... update it via the REST API ...
    UpdateOriginRequest request = new UpdateOriginRequest().setName("OriginUpdated");
    Response response = request("/v1/origin/uuid/" + entity.getId()).put(Entity.json(request));
    assertEquals(200, response.getStatus());

    // ... and check that the update ends up in the database.
    assertEquals(request.getName(), getOriginManager().getOrigin(entity.getId()).getName());
  }

  @Test
  public void testDeleteOrigin() {
    // Create an Origin in the database ...
    OriginEntity entity = createOrigin();

    // ... delete it via the REST API ...
    Response response = request("/v1/origin/uuid/" + entity.getId()).delete();
    assertEquals(200, response.getStatus());

    // ... and check that it's marked as deleted in the database.
    assertTrue(getOriginManager().getOrigin(entity.getId()).getFlags().contains(OriginEntity.Flag.Deleted));
  }

  private OriginEntity createOrigin() {
    OriginEntity entity = new OriginEntity()
            .setId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .setName("origin");
    return getOriginManager().saveOrigin(entity);
  }

}
