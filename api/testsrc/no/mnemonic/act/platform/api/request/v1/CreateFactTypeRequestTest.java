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
    UUID sourceObjectType = UUID.randomUUID();
    UUID destinationObjectType = UUID.randomUUID();
    String json = String.format("{" +
            "name : 'name'," +
            "validator : 'validator'," +
            "validatorParameter : 'validatorParameter'," +
            "relevantObjectBindings : [{ sourceObjectType : '%s', destinationObjectType : '%s', bidirectionalBinding : true }]" +
            "}", sourceObjectType, destinationObjectType);

    CreateFactTypeRequest request = getMapper().readValue(json, CreateFactTypeRequest.class);
    assertEquals("name", request.getName());
    assertEquals("validator", request.getValidator());
    assertEquals("validatorParameter", request.getValidatorParameter());
    assertEquals(1, request.getRelevantObjectBindings().size());
    assertEquals(sourceObjectType, request.getRelevantObjectBindings().get(0).getSourceObjectType());
    assertEquals(destinationObjectType, request.getRelevantObjectBindings().get(0).getDestinationObjectType());
    assertTrue(request.getRelevantObjectBindings().get(0).isBidirectionalBinding());
  }

  @Test
  public void testRequestValidationFailsOnNull() {
    Set<ConstraintViolation<CreateFactTypeRequest>> violations = getValidator().validate(new CreateFactTypeRequest());
    assertEquals(3, violations.size());
    assertPropertyInvalid(violations, "name");
    assertPropertyInvalid(violations, "validator");
    assertPropertyInvalid(violations, "relevantObjectBindings");
  }

  @Test
  public void testRequestValidationFailsOnEmpty() {
    Set<ConstraintViolation<CreateFactTypeRequest>> violations = getValidator().validate(new CreateFactTypeRequest()
            .setName("")
            .setValidator("")
            .setRelevantObjectBindings(new ArrayList<>()));
    assertEquals(3, violations.size());
    assertPropertyInvalid(violations, "name");
    assertPropertyInvalid(violations, "validator");
    assertPropertyInvalid(violations, "relevantObjectBindings");
  }

  @Test
  public void testRequestValidationFailsOnBlank() {
    Set<ConstraintViolation<CreateFactTypeRequest>> violations = getValidator().validate(new CreateFactTypeRequest()
            .setName(" ")
            .setValidator(" ")
            .addRelevantObjectBinding(new FactObjectBindingDefinition()
                    .setSourceObjectType(UUID.randomUUID())
                    .setDestinationObjectType(UUID.randomUUID())
            ));
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "name");
    assertPropertyInvalid(violations, "validator");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new CreateFactTypeRequest()
            .setName("name")
            .setValidator("validator")
            .addRelevantObjectBinding(new FactObjectBindingDefinition()
                    .setSourceObjectType(UUID.randomUUID())
                    .setDestinationObjectType(UUID.randomUUID())
            )
    ).isEmpty());
  }

}
