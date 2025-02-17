package no.mnemonic.services.grafeo.test.integration;

import com.fasterxml.jackson.databind.JsonNode;
import no.mnemonic.services.grafeo.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.services.grafeo.api.request.v1.SearchObjectRequest;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectIT extends AbstractIT {

  @Test
  public void testFetchObject() throws Exception {
    // Create an Object and a related Fact in the database ...
    ObjectTypeEntity objectType = createObjectType();
    ObjectRecord object = createObject(objectType.getId());
    createFact(object);

    // ... and check that the Object can be received via the REST API.
    fetchAndAssertSingle("/v1/object/uuid/" + object.getId(), object.getId());
    fetchAndAssertSingle("/v1/object/" + objectType.getName() + "/" + object.getValue(), object.getId());
  }

  @Test
  public void testFetchObjectCalculatesStatistic() throws Exception {
    // Create an Object and a related Fact in the database ...
    ObjectRecord object = createObject();
    FactRecord fact = createFact(object);

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
    ObjectRecord object = createObject(objectType.getId());
    FactRecord fact = createFact(object);

    // ... and check that the related Fact can be found via the REST API.
    fetchAndAssertList("/v1/object/uuid/" + object.getId() + "/facts", new SearchObjectFactsRequest(), fact.getId());
    fetchAndAssertList("/v1/object/" + objectType.getName() + "/" + object.getValue() + "/facts", new SearchObjectFactsRequest(), fact.getId());
  }

  @Test
  public void testSearchObjectFactsWithFiltering() throws Exception {
    // Create an Object with multiple related Facts in the database ...
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());
    ObjectRecord object = createObject(objectType.getId());
    FactRecord fact = createFact(object, factType, f -> f.setValue("value"));
    createFact(object, factType, f -> f.setValue("otherValue"));

    // ... and check that only one Fact after filtering is found via the REST API.
    fetchAndAssertList("/v1/object/uuid/" + object.getId() + "/facts", new SearchObjectFactsRequest().addFactValue(fact.getValue()), fact.getId());
  }

  @Test
  public void testSearchObjectsWithFiltering() throws Exception {
    // Create multiple Objects and related Facts in the database ...
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());
    ObjectRecord object1 = createObject(objectType.getId(), o -> o.setValue("value"));
    ObjectRecord object2 = createObject(objectType.getId(), o -> o.setValue("otherValue"));
    createFact(object1, factType, f -> f.setValue("fact1"));
    createFact(object2, factType, f -> f.setValue("fact2"));

    // ... and check that only one Object after filtering is found via the REST API.
    fetchAndAssertList("/v1/object/search", new SearchObjectRequest().addObjectValue(object1.getValue()), object1.getId());
  }

}
