package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TraverseGraphRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    String json = "{" +
            "before : '2016-11-30T15:47:00Z'," +
            "after : '2016-11-30T15:47:01Z'," +
            "includeRetracted : true," +
            "limit: 10," +
            "query : 'g.out()'" +
            "}";
    TraverseGraphRequest request = getMapper().readValue(json, TraverseGraphRequest.class);

    assertEquals(1480520820000L, request.getBefore().longValue());
    assertEquals(1480520821000L, request.getAfter().longValue());
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

    TraverseGraphRequest request = getMapper().readValue(json, TraverseGraphRequest.class);
    assertEquals(1480520820000L, request.getStartTimestamp().longValue());
    assertEquals(1480520821000L, request.getEndTimestamp().longValue());
    assertEquals(TimeFieldSearchRequest.TimeMatchStrategy.all, request.getTimeMatchStrategy());
    assertEquals(SetUtils.set(TimeFieldSearchRequest.TimeFieldStrategy.all), request.getTimeFieldStrategy());
  }

  @Test
  public void testRequestValidationFailsOnNull() {
    Set<ConstraintViolation<TraverseGraphRequest>> violations = getValidator().validate(new TraverseGraphRequest());
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnEmpty() {
    Set<ConstraintViolation<TraverseGraphRequest>> violations = getValidator().validate(new TraverseGraphRequest()
            .setQuery("")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnBlank() {
    Set<ConstraintViolation<TraverseGraphRequest>> violations = getValidator().validate(new TraverseGraphRequest()
            .setQuery(" ")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnBadLimit() {
    Set<ConstraintViolation<TraverseGraphRequest>> violations = getValidator().validate(new TraverseGraphRequest()
            .setQuery("g.out()")
            .setLimit(-1)
    );

    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "limit");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new TraverseGraphRequest().setQuery("g.out()")).isEmpty());
  }
}
