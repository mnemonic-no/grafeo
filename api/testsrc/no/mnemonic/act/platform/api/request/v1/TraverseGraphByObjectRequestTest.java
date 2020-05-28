package no.mnemonic.act.platform.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TraverseGraphByObjectRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    String json = "{" +
            "before : '2016-11-30T15:47:00Z'," +
            "after : '2016-11-30T15:47:01Z'," +
            "includeRetracted : true," +
            "query : 'g.out()'" +
            "}";
    TraverseGraphByObjectRequest request = getMapper().readValue(json, TraverseGraphByObjectRequest.class);

    assertEquals(1480520820000L, request.getBefore().longValue());
    assertEquals(1480520821000L, request.getAfter().longValue());
    assertTrue(request.getIncludeRetracted());
    assertEquals("g.out()", request.getQuery());
  }

  @Test
  public void testRequestValidationFailsOnNull() {
    Set<ConstraintViolation<TraverseGraphByObjectRequest>> violations = getValidator().validate(new TraverseGraphByObjectRequest());
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnEmpty() {
    Set<ConstraintViolation<TraverseGraphByObjectRequest>> violations = getValidator().validate(new TraverseGraphByObjectRequest()
            .setQuery("")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnBlank() {
    Set<ConstraintViolation<TraverseGraphByObjectRequest>> violations = getValidator().validate(new TraverseGraphByObjectRequest()
            .setQuery(" ")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new TraverseGraphByObjectRequest().setQuery("g.out()")).isEmpty());
  }
}
