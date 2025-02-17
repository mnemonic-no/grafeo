package no.mnemonic.services.grafeo.api.request.v1;

import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RetractFactRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID fact = UUID.randomUUID();
    String json = String.format("{" +
            "fact : '%s'," +
            "organization : 'organization'," +
            "origin : 'origin'," +
            "confidence : 0.1," +
            "accessMode : 'Explicit'," +
            "comment : 'comment'," +
            "acl : ['subject']" +
            "}", fact);

    RetractFactRequest request = getMapper().readValue(json, RetractFactRequest.class);
    assertEquals(fact, request.getFact());
    assertEquals("organization", request.getOrganization());
    assertEquals("origin", request.getOrigin());
    assertEquals(0.1f, request.getConfidence(), 0.0);
    assertEquals(AccessMode.Explicit, request.getAccessMode());
    assertEquals("comment", request.getComment());
    assertEquals(ListUtils.list("subject"), request.getAcl());
  }

  @Test
  public void testRequestValidationFailsOnNotNull() {
    Set<ConstraintViolation<RetractFactRequest>> violations = getValidator().validate(new RetractFactRequest());
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "fact");
  }

  @Test
  public void testRequestValidationFailsOnMin() {
    Set<ConstraintViolation<RetractFactRequest>> violations = getValidator().validate(new RetractFactRequest()
            .setFact(UUID.randomUUID())
            .setConfidence(-0.1f)
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "confidence");
  }

  @Test
  public void testRequestValidationFailsOnMax() {
    Set<ConstraintViolation<RetractFactRequest>> violations = getValidator().validate(new RetractFactRequest()
            .setFact(UUID.randomUUID())
            .setConfidence(1.1f)
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "confidence");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new RetractFactRequest().setFact(UUID.randomUUID())).isEmpty());
  }

}
