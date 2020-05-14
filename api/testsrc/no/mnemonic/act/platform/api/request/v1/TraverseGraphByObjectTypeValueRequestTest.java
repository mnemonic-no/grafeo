package no.mnemonic.act.platform.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TraverseGraphByObjectTypeValueRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {

    String json = "{" +
            "type : 'type'," +
            "value : 'value'," +
            "query : 'g.out()'," +
            "before : '2016-11-30T15:47:00Z'," +
            "after : '2016-11-30T15:47:01Z'," +
            "includeRetracted : true" +
            "}";

    TraverseGraphByObjectTypeValueRequest request = getMapper().readValue(json, TraverseGraphByObjectTypeValueRequest.class);
    assertEquals("type", request.getType());
    assertEquals("value", request.getValue());
    assertEquals("g.out()", request.getQuery());
    assertEquals(1480520820000L, request.getBefore().longValue());
    assertEquals(1480520821000L, request.getAfter().longValue());
    assertEquals(true, request.getIncludeRetracted());
  }

  @Test
  public void testRequestValidationFailsOnNull() {
    Set<ConstraintViolation<TraverseGraphByObjectTypeValueRequest>> violations = getValidator()
            .validate(new TraverseGraphByObjectTypeValueRequest());
    assertEquals(3, violations.size());
    assertPropertyInvalid(violations, "type");
    assertPropertyInvalid(violations, "value");
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnEmpty() {
    Set<ConstraintViolation<TraverseGraphByObjectTypeValueRequest>> violations = getValidator().validate(new TraverseGraphByObjectTypeValueRequest()
            .setType("type")
            .setValue("value")
            .setQuery("")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnBlank() {
    Set<ConstraintViolation<TraverseGraphByObjectTypeValueRequest>> violations = getValidator().validate(new TraverseGraphByObjectTypeValueRequest()
            .setType("type")
            .setValue("value")
            .setQuery(" ")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new TraverseGraphByObjectTypeValueRequest()
            .setType("type")
            .setValue("value")
            .setQuery("g.out()")
    ).isEmpty());
  }
}
