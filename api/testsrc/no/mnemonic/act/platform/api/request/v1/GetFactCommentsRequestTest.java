package no.mnemonic.act.platform.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GetFactCommentsRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{ fact : '%s', before : '2016-11-30T15:47:00Z', after : '2016-11-30T15:47:01Z' }", id);

    GetFactCommentsRequest request = getMapper().readValue(json, GetFactCommentsRequest.class);
    assertEquals(id, request.getFact());
    assertEquals(1480520820000L, request.getBefore().longValue());
    assertEquals(1480520821000L, request.getAfter().longValue());
  }

  @Test
  public void testRequestValidationFails() {
    Set<ConstraintViolation<GetFactCommentsRequest>> violations = getValidator().validate(new GetFactCommentsRequest());
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "fact");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new GetFactCommentsRequest().setFact(UUID.randomUUID())).isEmpty());
  }

}
