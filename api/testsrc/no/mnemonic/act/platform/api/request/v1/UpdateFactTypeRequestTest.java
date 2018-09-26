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
    String json = String.format("{" +
            "id : '%s'," +
            "name : 'name'," +
            "addObjectBindings : [{ sourceObjectType : '%s', destinationObjectType : '%s', bidirectionalBinding : true }]" +
            "}", id, sourceObjectType, destinationObjectType);

    UpdateFactTypeRequest request = getMapper().readValue(json, UpdateFactTypeRequest.class);
    assertEquals(id, request.getId());
    assertEquals("name", request.getName());
    assertEquals(1, request.getAddObjectBindings().size());
    assertEquals(sourceObjectType, request.getAddObjectBindings().get(0).getSourceObjectType());
    assertEquals(destinationObjectType, request.getAddObjectBindings().get(0).getDestinationObjectType());
    assertTrue(request.getAddObjectBindings().get(0).isBidirectionalBinding());
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
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .setName("name")
            .addAddObjectBinding(new FactObjectBindingDefinition()
                    .setSourceObjectType(UUID.randomUUID())
                    .setDestinationObjectType(UUID.randomUUID())
            )
    ).isEmpty());
  }

}
