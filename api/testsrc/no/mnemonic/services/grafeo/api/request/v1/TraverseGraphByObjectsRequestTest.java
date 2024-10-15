package no.mnemonic.services.grafeo.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TraverseGraphByObjectsRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    String json = "{" +
            "objects : []," +
            "query : 'g.out()'" +
            "}";
    TraverseGraphByObjectsRequest request = getMapper().readValue(json, TraverseGraphByObjectsRequest.class);
    assertEquals("g.out()", request.getQuery());
  }

  @Test
  public void testDecodeRequestWithObjects() throws Exception {
    UUID id = UUID.randomUUID();
    String json = "{" +
            "objects : ['" + id + "', 'ThreatActor/Sofacy']," +
            "includeRetracted : true," +
            "limit: 10," +
            "query : 'g.out()'" +
            "}";
    TraverseGraphByObjectsRequest request = getMapper().readValue(json, TraverseGraphByObjectsRequest.class);

    assertEquals(set(id.toString(), "ThreatActor/Sofacy"), request.getObjects());
    assertTrue(request.getIncludeRetracted());
    assertEquals(Integer.valueOf(10), request.getLimit());
    assertEquals("g.out()", request.getQuery());
  }

  @Test
  public void testDecodeTimeFieldSearchRequest() throws Exception {
    String json = "{" +
            "startTimestamp : '2016-11-30T15:47:00Z'," +
            "endTimestamp : '2016-11-30T15:47:01Z'," +
            "timeMatchStrategy : 'all'," +
            "timeFieldStrategy : ['all']" +
            "}";

    TraverseGraphByObjectsRequest request = getMapper().readValue(json, TraverseGraphByObjectsRequest.class);
    assertEquals(1480520820000L, request.getStartTimestamp().longValue());
    assertEquals(1480520821000L, request.getEndTimestamp().longValue());
    assertEquals(TimeFieldSearchRequest.TimeMatchStrategy.all, request.getTimeMatchStrategy());
    assertEquals(set(TimeFieldSearchRequest.TimeFieldStrategy.all), request.getTimeFieldStrategy());
  }

  @Test
  public void testAddingObjects() {
    TraverseGraphByObjectsRequest request = new TraverseGraphByObjectsRequest()
            .setObjects(set("ThreatActor/Sofacy"))
            .addObject("ThreatActor/Panda");

    assertEquals(set("ThreatActor/Sofacy", "ThreatActor/Panda"), request.getObjects());
  }

  @Test
  public void testRequestValidationFailsOnNull() {
    Set<ConstraintViolation<TraverseGraphByObjectsRequest>> violations = getValidator()
            .validate(new TraverseGraphByObjectsRequest());

    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "objects");
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnEmpty() {
    Set<ConstraintViolation<TraverseGraphByObjectsRequest>> violations = getValidator()
            .validate(new TraverseGraphByObjectsRequest().setQuery("").setObjects(set()));

    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "objects");
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnBlankQuery() {
    Set<ConstraintViolation<TraverseGraphByObjectsRequest>> violations = getValidator()
            .validate(new TraverseGraphByObjectsRequest()
            .setObjects(set(UUID.randomUUID().toString(), "ThreatActor/Sofacy"))
            .setQuery(" ")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnBadLimit() {
    Set<ConstraintViolation<TraverseGraphByObjectsRequest>> violations = getValidator().validate(new TraverseGraphByObjectsRequest()
            .setObjects(set(UUID.randomUUID().toString()))
            .setQuery("g.out()")
            .setLimit(-1)
    );

    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "limit");
  }


  @Test
  public void testRequestValidationWithBothIdAndTypeValue() {
    Set<ConstraintViolation<TraverseGraphByObjectsRequest>> violations = getValidator().validate(new TraverseGraphByObjectsRequest()
            .setObjects(set(UUID.randomUUID().toString(), "ThreatActor/Sofacy"))
            .setQuery("g.out()")
    );
    assertEquals(0, violations.size());
  }

  @Test
  public void testFromObjectRequest() {
    TraverseGraphByObjectsRequest request = TraverseGraphByObjectsRequest.from(
            new TraverseGraphRequest()
                    .setQuery("g.out()")
                    .setLimit(10)
                    .setIncludeRetracted(true),
            "test");

    assertEquals(set("test"), request.getObjects());
    assertTrue(request.getIncludeRetracted());
    assertEquals(Integer.valueOf(10), request.getLimit());
    assertEquals("g.out()", request.getQuery());
  }

  @Test
  public void testFromObjectTimeFieldSearchRequest() {
    long startTimestamp = 1470000000000L;
    long endTimestamp = 1480000000000L;
    TraverseGraphByObjectsRequest request = TraverseGraphByObjectsRequest.from(
            new TraverseGraphRequest()
                    .setStartTimestamp(startTimestamp)
                    .setEndTimestamp(endTimestamp)
                    .setTimeMatchStrategy(TimeFieldSearchRequest.TimeMatchStrategy.all)
                    .setTimeFieldStrategy(set(TimeFieldSearchRequest.TimeFieldStrategy.all)),
            "test");

    assertEquals(set("test"), request.getObjects());
    assertEquals(startTimestamp, request.getStartTimestamp().longValue());
    assertEquals(endTimestamp, request.getEndTimestamp().longValue());
    assertEquals(TimeFieldSearchRequest.TimeMatchStrategy.all, request.getTimeMatchStrategy());
    assertEquals(set(TimeFieldSearchRequest.TimeFieldStrategy.all), request.getTimeFieldStrategy());
  }
}
