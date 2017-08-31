package no.mnemonic.act.platform.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GetObjectByTypeValueRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    String json = "{ type : 'type', value : 'value' }";

    GetObjectByTypeValueRequest request = getMapper().readValue(json, GetObjectByTypeValueRequest.class);
    assertEquals("type", request.getType());
    assertEquals("value", request.getValue());
  }

  @Test
  public void testRequestValidationFailsOnNull() {
    Set<ConstraintViolation<GetObjectByTypeValueRequest>> violations = getValidator().validate(new GetObjectByTypeValueRequest());
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "type");
    assertPropertyInvalid(violations, "value");
  }

  @Test
  public void testRequestValidationFailsOnEmpty() {
    Set<ConstraintViolation<GetObjectByTypeValueRequest>> violations = getValidator().validate(new GetObjectByTypeValueRequest()
            .setType("")
            .setValue("")
    );
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "type");
    assertPropertyInvalid(violations, "value");
  }

  @Test
  public void testRequestValidationFailsOnBlank() {
    Set<ConstraintViolation<GetObjectByTypeValueRequest>> violations = getValidator().validate(new GetObjectByTypeValueRequest()
            .setType(" ")
            .setValue(" ")
    );
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "type");
    assertPropertyInvalid(violations, "value");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new GetObjectByTypeValueRequest()
            .setType("type")
            .setValue("value")
    ).isEmpty());
  }

}
