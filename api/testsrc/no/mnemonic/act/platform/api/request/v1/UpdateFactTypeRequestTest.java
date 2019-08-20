package no.mnemonic.act.platform.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UpdateFactTypeRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID id = UUID.randomUUID();
    UUID sourceObjectType = UUID.randomUUID();
    UUID destinationObjectType = UUID.randomUUID();
    UUID factType = UUID.randomUUID();
    String json = String.format("{" +
            "id : '%s'," +
            "name : 'name'," +
            "defaultConfidence : 0.1," +
            "addObjectBindings : [{ sourceObjectType : '%s', destinationObjectType : '%s', bidirectionalBinding : true }]," +
            "addFactBindings : [{ factType : '%s' }]" +
            "}", id, sourceObjectType, destinationObjectType, factType);

    UpdateFactTypeRequest request = getMapper().readValue(json, UpdateFactTypeRequest.class);
    assertEquals(id, request.getId());
    assertEquals("name", request.getName());
    assertEquals(0.1f, request.getDefaultConfidence(), 0.0);
    assertEquals(1, request.getAddObjectBindings().size());
    assertEquals(sourceObjectType, request.getAddObjectBindings().get(0).getSourceObjectType());
    assertEquals(destinationObjectType, request.getAddObjectBindings().get(0).getDestinationObjectType());
    assertTrue(request.getAddObjectBindings().get(0).isBidirectionalBinding());
    assertEquals(1, request.getAddFactBindings().size());
    assertEquals(factType, request.getAddFactBindings().get(0).getFactType());
  }

  @Test
  public void testRequestValidationFailsOnNotNull() {
    Set<ConstraintViolation<UpdateFactTypeRequest>> violations = getValidator().validate(new UpdateFactTypeRequest());
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "id");
  }

  @Test
  public void testRequestValidationFailsOnSize() {
    Set<ConstraintViolation<UpdateFactTypeRequest>> violations = getValidator().validate(new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .setName(""));
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "name");
  }

  @Test
  public void testRequestValidationFailsOnMin() {
    Set<ConstraintViolation<UpdateFactTypeRequest>> violations = getValidator().validate(new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .setDefaultConfidence(-0.1f));
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "defaultConfidence");
  }

  @Test
  public void testRequestValidationFailsOnMax() {
    Set<ConstraintViolation<UpdateFactTypeRequest>> violations = getValidator().validate(new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .setDefaultConfidence(1.1f));
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "defaultConfidence");
  }

  @Test
  public void testRequestValidationFailsOnValid() {
    Set<ConstraintViolation<UpdateFactTypeRequest>> violations = getValidator().validate(new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .setName("name")
            .addAddFactBinding(new MetaFactBindingDefinition()));
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "factType");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .setName("name")
            .addAddObjectBinding(new FactObjectBindingDefinition()
                    .setSourceObjectType(UUID.randomUUID())
                    .setDestinationObjectType(UUID.randomUUID())
            )
            .addAddFactBinding(new MetaFactBindingDefinition()
                    .setFactType(UUID.randomUUID())
            )
    ).isEmpty());
  }

}
