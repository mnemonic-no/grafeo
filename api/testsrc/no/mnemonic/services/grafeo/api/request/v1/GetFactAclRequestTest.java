package no.mnemonic.services.grafeo.api.request.v1;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetFactAclRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{ fact : '%s' }", id);

    GetFactAclRequest request = getMapper().readValue(json, GetFactAclRequest.class);
    assertEquals(id, request.getFact());
  }

  @Test
  public void testRequestValidationFails() {
    Set<ConstraintViolation<GetFactAclRequest>> violations = getValidator().validate(new GetFactAclRequest());
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "fact");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new GetFactAclRequest().setFact(UUID.randomUUID())).isEmpty());
  }

}
