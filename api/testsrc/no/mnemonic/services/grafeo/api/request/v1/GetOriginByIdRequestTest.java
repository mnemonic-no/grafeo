package no.mnemonic.services.grafeo.api.request.v1;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetOriginByIdRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{ id : '%s' }", id);

    GetOriginByIdRequest request = getMapper().readValue(json, GetOriginByIdRequest.class);
    assertEquals(id, request.getId());
  }

  @Test
  public void testRequestValidationFails() {
    Set<ConstraintViolation<GetOriginByIdRequest>> violations = getValidator().validate(new GetOriginByIdRequest());
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "id");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new GetOriginByIdRequest().setId(UUID.randomUUID())).isEmpty());
  }

}
