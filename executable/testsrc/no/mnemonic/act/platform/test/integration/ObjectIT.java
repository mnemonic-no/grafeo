package no.mnemonic.act.platform.test.integration;

import com.fasterxml.jackson.databind.node.ArrayNode;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.act.platform.api.request.v1.SearchObjectRequest;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ObjectIT extends AbstractIT {

  @Test
  public void testFetchObject() throws Exception {
    // Create an Object and a related Fact in the database ...
    ObjectTypeEntity objectType = createObjectType();
    ObjectEntity object = createObject(objectType.getId());
    createFact(object);

    // ... and check that the Object can be received via the REST API.
    fetchAndAssertSingle("/v1/object/uuid/" + object.getId(), object.getId());
    fetchAndAssertSingle("/v1/object/" + objectType.getName() + "/" + object.getValue(), object.getId());
  }

  @Test
  public void testSearchObjectFacts() throws Exception {
    // Create an Object and a related Fact in the database ...
    ObjectTypeEntity objectType = createObjectType();
    ObjectEntity object = createObject(objectType.getId());
    FactEntity fact = createFact(object);

    // ... and check that the related Fact can be found via the REST API.
    fetchAndAssertList("/v1/object/uuid/" + object.getId() + "/facts", new SearchObjectFactsRequest(), fact.getId());
    fetchAndAssertList("/v1/object/" + objectType.getName() + "/" + object.getValue() + "/facts", new SearchObjectFactsRequest(), fact.getId());
  }

  @Test
  public void testSearchObjects() throws Exception {
    // Create an Object and a related Fact in the database ...
    ObjectTypeEntity objectType = createObjectType();
    ObjectEntity object = createObject(objectType.getId());
    createFact(object);

    // ... and check that the Object can be found via the REST API.
    fetchAndAssertList("/v1/object/search", new SearchObjectRequest(), object.getId());
  }

  private void fetchAndAssertSingle(String url, UUID id) throws Exception {
    Response response = target(url).request().get();
    assertEquals(200, response.getStatus());
    assertEquals(id, getIdFromModel(getPayload(response)));
  }

  private void fetchAndAssertList(String url, ValidatingRequest request, UUID id) throws Exception {
    Response response = target(url).request().post(Entity.json(request));
    assertEquals(200, response.getStatus());
    ArrayNode data = (ArrayNode) getPayload(response);
    assertEquals(1, data.size());
    assertEquals(id, getIdFromModel(data.get(0)));
  }

}
