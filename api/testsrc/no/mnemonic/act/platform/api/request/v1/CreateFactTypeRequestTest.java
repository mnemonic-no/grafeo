package no.mnemonic.act.platform.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CreateFactTypeRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID objectType = UUID.randomUUID();
    String json = String.format("{" +
            "name : 'name'," +
            "validator : 'validator'," +
            "validatorParameter : 'validatorParameter'," +
            "entityHandler : 'entityHandler'," +
            "entityHandlerParameter : 'entityHandlerParameter'," +
            "relevantObjectBindings : [{ objectType : '%s', direction : 'BiDirectional' }]" +
            "}", objectType);

    CreateFactTypeRequest request = getMapper().readValue(json, CreateFactTypeRequest.class);
    assertEquals("name", request.getName());
    assertEquals("validator", request.getValidator());
    assertEquals("validatorParameter", request.getValidatorParameter());
    assertEquals("entityHandler", request.getEntityHandler());
    assertEquals("entityHandlerParameter", request.getEntityHandlerParameter());
    assertEquals(1, request.getRelevantObjectBindings().size());
    assertEquals(objectType, request.getRelevantObjectBindings().get(0).getObjectType());
    assertEquals(Direction.BiDirectional, request.getRelevantObjectBindings().get(0).getDirection());
  }

  @Test
  public void testRequestValidationFailsOnNotNull() {
    Set<ConstraintViolation<CreateFactTypeRequest>> violations = getValidator().validate(new CreateFactTypeRequest());
    assertEquals(4, violations.size());
    assertPropertyInvalid(violations, "name");
    assertPropertyInvalid(violations, "validator");
    assertPropertyInvalid(violations, "entityHandler");
    assertPropertyInvalid(violations, "relevantObjectBindings");
  }

  @Test
  public void testRequestValidationFailsOnSize() {
    Set<ConstraintViolation<CreateFactTypeRequest>> violations = getValidator().validate(new CreateFactTypeRequest()
            .setName("")
            .setValidator("")
            .setEntityHandler("")
            .setRelevantObjectBindings(new ArrayList<>()));
    assertEquals(4, violations.size());
    assertPropertyInvalid(violations, "name");
    assertPropertyInvalid(violations, "validator");
    assertPropertyInvalid(violations, "entityHandler");
    assertPropertyInvalid(violations, "relevantObjectBindings");
  }

  @Test
  public void testRequestValidationFailsOnValid() {
    Set<ConstraintViolation<CreateFactTypeRequest>> violations = getValidator().validate(new CreateFactTypeRequest()
            .setName("name")
            .setValidator("validator")
            .setEntityHandler("entityHandler")
            .addRelevantObjectBinding(new FactObjectBindingDefinition()));
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "objectType");
    assertPropertyInvalid(violations, "direction");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new CreateFactTypeRequest()
            .setName("name")
            .setValidator("validator")
            .setEntityHandler("entityHandler")
            .addRelevantObjectBinding(new FactObjectBindingDefinition()
                    .setObjectType(UUID.randomUUID())
                    .setDirection(Direction.BiDirectional)
            )
    ).isEmpty());
  }

}
