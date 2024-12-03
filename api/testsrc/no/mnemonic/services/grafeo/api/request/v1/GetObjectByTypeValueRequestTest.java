package no.mnemonic.services.grafeo.api.request.v1;

import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetObjectByTypeValueRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    String json = "{ type : 'type', value : 'value', before : '2016-11-30T15:47:00Z', after : '2016-11-30T15:47:01Z' }";

    GetObjectByTypeValueRequest request = getMapper().readValue(json, GetObjectByTypeValueRequest.class);
    assertEquals("type", request.getType());
    assertEquals("value", request.getValue());
    assertEquals(1480520820000L, request.getBefore().longValue());
    assertEquals(1480520821000L, request.getAfter().longValue());
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
