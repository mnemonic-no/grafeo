package no.mnemonic.act.platform.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UpdateObjectTypeRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{ id : '%s', name : 'name' }", id);

    UpdateObjectTypeRequest request = getMapper().readValue(json, UpdateObjectTypeRequest.class);
    assertEquals(id, request.getId());
    assertEquals("name", request.getName());
  }

  @Test
  public void testRequestValidationFailsOnNotNull() {
    Set<ConstraintViolation<UpdateObjectTypeRequest>> violations = getValidator().validate(new UpdateObjectTypeRequest());
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "id");
  }

  @Test
  public void testRequestValidationFailsOnSize() {
    Set<ConstraintViolation<UpdateObjectTypeRequest>> violations = getValidator().validate(new UpdateObjectTypeRequest()
            .setId(UUID.randomUUID())
            .setName(""));
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "name");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new UpdateObjectTypeRequest()
            .setId(UUID.randomUUID())
            .setName("name")
    ).isEmpty());
  }

}
