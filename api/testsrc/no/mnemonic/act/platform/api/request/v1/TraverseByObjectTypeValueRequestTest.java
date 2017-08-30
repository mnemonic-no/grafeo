package no.mnemonic.act.platform.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TraverseByObjectTypeValueRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    String json = "{ type : 'type', value : 'value', query : 'g.out()' }";

    TraverseByObjectTypeValueRequest request = getMapper().readValue(json, TraverseByObjectTypeValueRequest.class);
    assertEquals("type", request.getType());
    assertEquals("value", request.getValue());
    assertEquals("g.out()", request.getQuery());
  }

  @Test
  public void testRequestValidationFailsOnNull() {
    Set<ConstraintViolation<TraverseByObjectTypeValueRequest>> violations = getValidator().validate(new TraverseByObjectTypeValueRequest());
    assertEquals(3, violations.size());
    assertPropertyInvalid(violations, "type");
    assertPropertyInvalid(violations, "value");
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnEmpty() {
    Set<ConstraintViolation<TraverseByObjectTypeValueRequest>> violations = getValidator().validate(new TraverseByObjectTypeValueRequest()
            .setType("type")
            .setValue("value")
            .setQuery("")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnBlank() {
    Set<ConstraintViolation<TraverseByObjectTypeValueRequest>> violations = getValidator().validate(new TraverseByObjectTypeValueRequest()
            .setType("type")
            .setValue("value")
            .setQuery(" ")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new TraverseByObjectTypeValueRequest()
            .setType("type")
            .setValue("value")
            .setQuery("g.out()")
    ).isEmpty());
  }

}
