package no.mnemonic.act.platform.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CreateOriginRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID organization = UUID.randomUUID();
    String json = String.format("{" +
            "organization : '%s'," +
            "name : 'name'," +
            "description : 'description'," +
            "trust : 0.1" +
            "}", organization);

    CreateOriginRequest request = getMapper().readValue(json, CreateOriginRequest.class);
    assertEquals(organization, request.getOrganization());
    assertEquals("name", request.getName());
    assertEquals("description", request.getDescription());
    assertEquals(0.1f, request.getTrust(), 0.0);
  }

  @Test
  public void testRequestValidationFailsOnNull() {
    Set<ConstraintViolation<CreateOriginRequest>> violations = getValidator().validate(new CreateOriginRequest());
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "name");
  }

  @Test
  public void testRequestValidationFailsOnEmpty() {
    Set<ConstraintViolation<CreateOriginRequest>> violations = getValidator().validate(new CreateOriginRequest()
            .setName("")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "name");
  }

  @Test
  public void testRequestValidationFailsOnBlank() {
    Set<ConstraintViolation<CreateOriginRequest>> violations = getValidator().validate(new CreateOriginRequest()
            .setName(" ")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "name");
  }

  @Test
  public void testRequestValidationFailsOnMin() {
    Set<ConstraintViolation<CreateOriginRequest>> violations = getValidator().validate(new CreateOriginRequest()
            .setName("name")
            .setTrust(-0.1f)
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "trust");
  }

  @Test
  public void testRequestValidationFailsOnMax() {
    Set<ConstraintViolation<CreateOriginRequest>> violations = getValidator().validate(new CreateOriginRequest()
            .setName("name")
            .setTrust(1.1f)
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "trust");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new CreateOriginRequest()
            .setName("name")
    ).isEmpty());
  }

}
