package no.mnemonic.act.platform.test.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.act.platform.api.request.v1.SearchObjectRequest;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.act.platform.entity.cassandra.FactTypeEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.time.Instant;
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
  public void testFetchObjectCalculatesStatistic() throws Exception {
    // Create an Object and a related Fact in the database ...
    ObjectEntity object = createObject();
    FactEntity fact = createFact(object);

    // ... and check that the returned Object contains the correct statistics.
    Response response = target("/v1/object/uuid/" + object.getId()).request().get();
    assertEquals(200, response.getStatus());

    JsonNode data = getPayload(response);
    assertEquals(1, data.get("statistics").size());
    assertEquals(fact.getTypeID(), getIdFromModel(data.get("statistics").get(0).get("type")));
    assertEquals(1, data.get("statistics").get(0).get("count").asLong());
    assertEquals(Instant.ofEpochMilli(fact.getTimestamp()).toString(), data.get("statistics").get(0).get("lastAddedTimestamp").asText());
    assertEquals(Instant.ofEpochMilli(fact.getLastSeenTimestamp()).toString(), data.get("statistics").get(0).get("lastSeenTimestamp").asText());
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
  public void testSearchObjectFactsWithFiltering() throws Exception {
    // Create an Object with multiple related Facts in the database ...
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());
    ObjectEntity object = createObject(objectType.getId());
    FactEntity fact = createFact(object, factType, f -> f.setValue("value"));
    createFact(object, factType, f -> f.setValue("otherValue"));

    // ... and check that only one Fact after filtering is found via the REST API.
    fetchAndAssertList("/v1/object/uuid/" + object.getId() + "/facts", new SearchObjectFactsRequest().addFactValue(fact.getValue()), fact.getId());
  }

  @Test
  public void testSearchObjects() throws Exception {
    // Create an Object and a related Fact in the database ...
    ObjectEntity object = createObject();
    createFact(object);

    // ... and check that the Object can be found via the REST API.
    fetchAndAssertList("/v1/object/search", new SearchObjectRequest(), object.getId());
  }

  @Test
  public void testSearchObjectsWithFiltering() throws Exception {
    // Create multiple Objects and related Facts in the database ...
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());
    ObjectEntity object1 = createObject(objectType.getId(), o -> o.setValue("value"));
    ObjectEntity object2 = createObject(objectType.getId(), o -> o.setValue("otherValue"));
    createFact(object1, factType, f -> f.setValue("fact1"));
    createFact(object2, factType, f -> f.setValue("fact2"));

    // ... and check that only one Object after filtering is found via the REST API.
    fetchAndAssertList("/v1/object/search", new SearchObjectRequest().addObjectValue(object1.getValue()), object1.getId());
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
