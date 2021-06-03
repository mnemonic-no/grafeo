package no.mnemonic.act.platform.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GetObjectByIdRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{ id : '%s', before : '2016-11-30T15:47:00Z', after : '2016-11-30T15:47:01Z' }", id);

    GetObjectByIdRequest request = getMapper().readValue(json, GetObjectByIdRequest.class);
    assertEquals(id, request.getId());
    assertEquals(1480520820000L, request.getBefore().longValue());
    assertEquals(1480520821000L, request.getAfter().longValue());
  }

  @Test
  public void testRequestValidationFails() {
    Set<ConstraintViolation<GetObjectByIdRequest>> violations = getValidator().validate(new GetObjectByIdRequest());
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "id");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new GetObjectByIdRequest().setId(UUID.randomUUID())).isEmpty());
  }

}
