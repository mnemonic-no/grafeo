package no.mnemonic.services.grafeo.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CreateFactTypeRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID sourceObjectType = UUID.randomUUID();
    UUID destinationObjectType = UUID.randomUUID();
    UUID factType = UUID.randomUUID();
    String json = String.format("{" +
            "name : 'name'," +
            "defaultConfidence : 0.1," +
            "validator : 'validator'," +
            "validatorParameter : 'validatorParameter'," +
            "relevantObjectBindings : [{ sourceObjectType : '%s', destinationObjectType : '%s', bidirectionalBinding : true }]," +
            "relevantFactBindings : [{ factType : '%s' }]" +
            "}", sourceObjectType, destinationObjectType, factType);

    CreateFactTypeRequest request = getMapper().readValue(json, CreateFactTypeRequest.class);
    assertEquals("name", request.getName());
    assertEquals(0.1f, request.getDefaultConfidence(), 0.0);
    assertEquals("validator", request.getValidator());
    assertEquals("validatorParameter", request.getValidatorParameter());
    assertEquals(1, request.getRelevantObjectBindings().size());
    assertEquals(sourceObjectType, request.getRelevantObjectBindings().get(0).getSourceObjectType());
    assertEquals(destinationObjectType, request.getRelevantObjectBindings().get(0).getDestinationObjectType());
    assertTrue(request.getRelevantObjectBindings().get(0).isBidirectionalBinding());
    assertEquals(1, request.getRelevantFactBindings().size());
    assertEquals(factType, request.getRelevantFactBindings().get(0).getFactType());
  }

  @Test
  public void testRequestValidationFailsOnNull() {
    Set<ConstraintViolation<CreateFactTypeRequest>> violations = getValidator().validate(new CreateFactTypeRequest());
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "name");
    assertPropertyInvalid(violations, "validator");
  }

  @Test
  public void testRequestValidationFailsOnEmpty() {
    Set<ConstraintViolation<CreateFactTypeRequest>> violations = getValidator().validate(new CreateFactTypeRequest()
            .setName("")
            .setValidator(""));
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "name");
    assertPropertyInvalid(violations, "validator");
  }

  @Test
  public void testRequestValidationFailsOnBlank() {
    Set<ConstraintViolation<CreateFactTypeRequest>> violations = getValidator().validate(new CreateFactTypeRequest()
            .setName(" ")
            .setValidator(" "));
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "name");
    assertPropertyInvalid(violations, "validator");
  }

  @Test
  public void testRequestValidationFailsOnMin() {
    Set<ConstraintViolation<CreateFactTypeRequest>> violations = getValidator().validate(new CreateFactTypeRequest()
            .setName("name")
            .setValidator("validator")
            .setDefaultConfidence(-0.1f));
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "defaultConfidence");
  }

  @Test
  public void testRequestValidationFailsOnMax() {
    Set<ConstraintViolation<CreateFactTypeRequest>> violations = getValidator().validate(new CreateFactTypeRequest()
            .setName("name")
            .setValidator("validator")
            .setDefaultConfidence(1.1f));
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "defaultConfidence");
  }

  @Test
  public void testRequestValidationFailsOnValid() {
    Set<ConstraintViolation<CreateFactTypeRequest>> violations = getValidator().validate(new CreateFactTypeRequest()
            .setName("name")
            .setValidator("validator")
            .addRelevantFactBinding(new MetaFactBindingDefinition()));
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "factType");
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
            .addRelevantFactBinding(new MetaFactBindingDefinition()
                    .setFactType(UUID.randomUUID())
            )
    ).isEmpty());
  }

}
