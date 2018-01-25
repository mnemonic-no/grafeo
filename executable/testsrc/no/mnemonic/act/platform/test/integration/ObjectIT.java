package no.mnemonic.act.platform.test.integration;

import com.fasterxml.jackson.databind.JsonNode;
import no.mnemonic.act.platform.api.request.v1.*;
import no.mnemonic.act.platform.dao.cassandra.entity.AccessMode;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.time.Instant;

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
    Response response = request("/v1/object/uuid/" + object.getId()).get();
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

  @Test
  public void testTraverseObjects() throws Exception {
    // Create an Object and a related Fact in the database ...
    ObjectTypeEntity objectType = createObjectType();
    ObjectEntity object = createObject(objectType.getId());
    FactEntity fact = createFact(object);

    // ... and check that the Fact can be received via a simple graph traversal.
    fetchAndAssertList(String.format("/v1/object/uuid/%s/traverse", object.getId()),
            new TraverseByObjectIdRequest().setQuery("g.outE()"), fact.getId());
    fetchAndAssertList(String.format("/v1/object/%s/%s/traverse", objectType.getName(), object.getValue()),
            new TraverseByObjectTypeValueRequest().setQuery("g.outE()"), fact.getId());
    fetchAndAssertList("/v1/object/traverse", new TraverseByObjectSearchRequest().setQuery("g.outE()"), fact.getId());
  }

  @Test
  public void testTraverseObjectsWithACL() throws Exception {
    // Create one Object and multiple related Facts with different access modes in the database ...
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());
    ObjectEntity object = createObject(objectType.getId());
    FactEntity fact = createFact(object, factType, f -> f.setAccessMode(AccessMode.Public));
    createFact(object, factType, f -> f.setAccessMode(AccessMode.Explicit));

    // ... and check that only one Fact can be received via a simple graph traversal.
    fetchAndAssertList(String.format("/v1/object/uuid/%s/traverse", object.getId()),
            new TraverseByObjectIdRequest().setQuery("g.outE()"), fact.getId());
  }

  @Test
  public void testTraverseObjectsWithFiltering() throws Exception {
    // Create multiple Objects and related Facts in the database ...
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());
    ObjectEntity object1 = createObject(objectType.getId(), o -> o.setValue("value"));
    ObjectEntity object2 = createObject(objectType.getId(), o -> o.setValue("otherValue"));
    FactEntity fact = createFact(object1, factType, f -> f.setValue("fact1"));
    createFact(object2, factType, f -> f.setValue("fact2"));

    // ... and check that only one Fact can be received via a simple graph traversal after filtering.
    TraverseByObjectSearchRequest request = (TraverseByObjectSearchRequest) new TraverseByObjectSearchRequest()
            .setQuery("g.outE()")
            .addObjectValue(object1.getValue());
    fetchAndAssertList("/v1/object/traverse", request, fact.getId());
  }

}
