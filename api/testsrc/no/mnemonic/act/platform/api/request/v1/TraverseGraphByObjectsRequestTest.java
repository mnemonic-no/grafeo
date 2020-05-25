package no.mnemonic.act.platform.api.request.v1;

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
            "before : '2016-11-30T15:47:00Z'," +
            "after : '2016-11-30T15:47:01Z'," +
            "includeRetracted : true," +
            "limit: 10," +
            "query : 'g.out()'" +
            "}";
    TraverseGraphByObjectsRequest request = getMapper().readValue(json, TraverseGraphByObjectsRequest.class);

    assertEquals(set(id.toString(), "ThreatActor/Sofacy"), request.getObjects());

    assertEquals(1480520820000L, request.getBefore().longValue());
    assertEquals(1480520821000L, request.getAfter().longValue());
    assertTrue(request.getIncludeRetracted());
    assertEquals(Integer.valueOf(10), request.getLimit());
    assertEquals("g.out()", request.getQuery());
  }

  @Test
  public void testAddingObjects() {
    TraverseGraphByObjectsRequest request = new TraverseGraphByObjectsRequest()
            .setObjects(set("ThreatActor/Sofacy"))
            .addObject("ThreatActor/Panda");

    assertEquals(set("ThreatActor/Sofacy", "ThreatActor/Panda"),request.getObjects());
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
    long after = 1470000000000L;
    long before = 1480000000000L;
    TraverseGraphByObjectsRequest request = TraverseGraphByObjectsRequest.from(
            new TraverseGraphRequest()
                    .setQuery("g.out()")
                    .setLimit(10)
                    .setIncludeRetracted(true)
                    .setAfter(after)
                    .setBefore(before),
            "test");

    assertEquals(set("test"), request.getObjects());
    assertEquals(after, request.getAfter().longValue());
    assertEquals(before, request.getBefore().longValue());
    assertTrue(request.getIncludeRetracted());
    assertEquals(Integer.valueOf(10), request.getLimit());
    assertEquals("g.out()", request.getQuery());
  }
}
