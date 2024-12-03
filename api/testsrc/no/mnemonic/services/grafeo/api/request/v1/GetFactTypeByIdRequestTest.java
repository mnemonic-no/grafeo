package no.mnemonic.services.grafeo.api.request.v1;

import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetFactTypeByIdRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{ id : '%s' }", id);

    GetFactTypeByIdRequest request = getMapper().readValue(json, GetFactTypeByIdRequest.class);
    assertEquals(id, request.getId());
  }

  @Test
  public void testRequestValidationFails() {
    Set<ConstraintViolation<GetFactTypeByIdRequest>> violations = getValidator().validate(new GetFactTypeByIdRequest());
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "id");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new GetFactTypeByIdRequest().setId(UUID.randomUUID())).isEmpty());
  }

}
