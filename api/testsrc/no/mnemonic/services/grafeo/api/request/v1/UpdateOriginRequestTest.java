package no.mnemonic.services.grafeo.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UpdateOriginRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID id = UUID.randomUUID();
    UUID organization = UUID.randomUUID();
    String json = String.format("{" +
            "id : '%s'," +
            "organization : '%s'," +
            "name : 'name'," +
            "description : 'description'," +
            "trust : 0.1" +
            "}", id, organization);

    UpdateOriginRequest request = getMapper().readValue(json, UpdateOriginRequest.class);
    assertEquals(id, request.getId());
    assertEquals(organization, request.getOrganization());
    assertEquals("name", request.getName());
    assertEquals("description", request.getDescription());
    assertEquals(0.1f, request.getTrust(), 0.0);
  }

  @Test
  public void testRequestValidationFailsOnNotNull() {
    Set<ConstraintViolation<UpdateOriginRequest>> violations = getValidator().validate(new UpdateOriginRequest());
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "id");
  }

  @Test
  public void testRequestValidationFailsOnMin() {
    Set<ConstraintViolation<UpdateOriginRequest>> violations = getValidator().validate(new UpdateOriginRequest()
            .setId(UUID.randomUUID())
            .setTrust(-0.1f)
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "trust");
  }

  @Test
  public void testRequestValidationFailsOnMax() {
    Set<ConstraintViolation<UpdateOriginRequest>> violations = getValidator().validate(new UpdateOriginRequest()
            .setId(UUID.randomUUID())
            .setTrust(1.1f)
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "trust");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new UpdateOriginRequest()
            .setId(UUID.randomUUID())
    ).isEmpty());
  }

}
