package no.mnemonic.services.grafeo.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GrantFactAccessRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID fact = UUID.randomUUID();
    String json = String.format("{ fact : '%s', subject : 'subject' }", fact);

    GrantFactAccessRequest request = getMapper().readValue(json, GrantFactAccessRequest.class);
    assertEquals(fact, request.getFact());
    assertEquals("subject", request.getSubject());
  }

  @Test
  public void testRequestValidationFails() {
    Set<ConstraintViolation<GrantFactAccessRequest>> violations = getValidator().validate(new GrantFactAccessRequest());
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "fact");
    assertPropertyInvalid(violations, "subject");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new GrantFactAccessRequest()
            .setFact(UUID.randomUUID())
            .setSubject("subject")
    ).isEmpty());
  }

}
