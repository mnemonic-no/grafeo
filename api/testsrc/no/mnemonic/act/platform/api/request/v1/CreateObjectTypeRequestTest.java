package no.mnemonic.act.platform.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CreateObjectTypeRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    String json = "{" +
            "name : 'name'," +
            "validator : 'validator'," +
            "validatorParameter : 'validatorParameter'," +
            "entityHandler : 'entityHandler'," +
            "entityHandlerParameter : 'entityHandlerParameter'" +
            "}";

    CreateObjectTypeRequest request = getMapper().readValue(json, CreateObjectTypeRequest.class);
    assertEquals("name", request.getName());
    assertEquals("validator", request.getValidator());
    assertEquals("validatorParameter", request.getValidatorParameter());
    assertEquals("entityHandler", request.getEntityHandler());
    assertEquals("entityHandlerParameter", request.getEntityHandlerParameter());
  }

  @Test
  public void testRequestValidationFailsOnNotNull() {
    Set<ConstraintViolation<CreateObjectTypeRequest>> violations = getValidator().validate(new CreateObjectTypeRequest());
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "name");
  }

  @Test
  public void testRequestValidationFailsOnSize() {
    Set<ConstraintViolation<CreateObjectTypeRequest>> violations = getValidator().validate(new CreateObjectTypeRequest()
            .setName(""));
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "name");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new CreateObjectTypeRequest()
            .setName("name")
    ).isEmpty());
  }

}
