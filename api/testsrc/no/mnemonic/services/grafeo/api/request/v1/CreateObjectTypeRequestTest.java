package no.mnemonic.services.grafeo.api.request.v1;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreateObjectTypeRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    String json = "{" +
            "name : 'name'," +
            "validator : 'validator'," +
            "validatorParameter : 'validatorParameter'," +
            "indexOption : 'TimeGlobal'" +
            "}";

    CreateObjectTypeRequest request = getMapper().readValue(json, CreateObjectTypeRequest.class);
    assertEquals("name", request.getName());
    assertEquals("validator", request.getValidator());
    assertEquals("validatorParameter", request.getValidatorParameter());
    assertEquals(CreateObjectTypeRequest.IndexOption.TimeGlobal, request.getIndexOption());
  }

  @Test
  public void testRequestValidationFailsOnNull() {
    Set<ConstraintViolation<CreateObjectTypeRequest>> violations = getValidator().validate(new CreateObjectTypeRequest());
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "name");
    assertPropertyInvalid(violations, "validator");
  }

  @Test
  public void testRequestValidationFailsOnEmpty() {
    Set<ConstraintViolation<CreateObjectTypeRequest>> violations = getValidator().validate(new CreateObjectTypeRequest()
            .setName("")
            .setValidator(""));
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "name");
    assertPropertyInvalid(violations, "validator");
  }

  @Test
  public void testRequestValidationFailsOnBlank() {
    Set<ConstraintViolation<CreateObjectTypeRequest>> violations = getValidator().validate(new CreateObjectTypeRequest()
            .setName(" ")
            .setValidator(" "));
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "name");
    assertPropertyInvalid(violations, "validator");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new CreateObjectTypeRequest()
            .setName("name")
            .setValidator("validator")
    ).isEmpty());
  }

}
