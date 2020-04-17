package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CreateFactRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID objectID = UUID.randomUUID();
    String json = String.format("{" +
            "type : 'factType'," +
            "value : 'factValue'," +
            "organization : 'organization'," +
            "origin : 'origin'," +
            "confidence : 0.1," +
            "accessMode : 'Explicit'," +
            "comment : 'comment'," +
            "acl : ['subject']," +
            "sourceObject : 'type/value'," +
            "destinationObject : '%s'," +
            "bidirectionalBinding : true" +
            "}", objectID);

    CreateFactRequest request = getMapper().readValue(json, CreateFactRequest.class);
    assertEquals("factType", request.getType());
    assertEquals("factValue", request.getValue());
    assertEquals("organization", request.getOrganization());
    assertEquals("origin", request.getOrigin());
    assertEquals(0.1f, request.getConfidence(), 0.0);
    assertEquals(AccessMode.Explicit, request.getAccessMode());
    assertEquals("comment", request.getComment());
    assertEquals(ListUtils.list("subject"), request.getAcl());
    assertEquals("type/value", request.getSourceObject());
    assertEquals(objectID, UUID.fromString(request.getDestinationObject()));
    assertTrue(request.isBidirectionalBinding());
  }

  @Test
  public void testRequestValidationFailsOnNull() {
    Set<ConstraintViolation<CreateFactRequest>> violations = getValidator().validate(new CreateFactRequest());
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "type");
  }

  @Test
  public void testRequestValidationFailsOnEmpty() {
    Set<ConstraintViolation<CreateFactRequest>> violations = getValidator().validate(new CreateFactRequest()
            .setType("")
            .setValue("")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "type");
  }

  @Test
  public void testRequestValidationFailsOnBlank() {
    Set<ConstraintViolation<CreateFactRequest>> violations = getValidator().validate(new CreateFactRequest()
            .setType(" ")
            .setValue(" ")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "type");
  }

  @Test
  public void testRequestValidationFailsOnMin() {
    Set<ConstraintViolation<CreateFactRequest>> violations = getValidator().validate(new CreateFactRequest()
            .setType("type")
            .setValue("value")
            .setConfidence(-0.1f)
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "confidence");
  }

  @Test
  public void testRequestValidationFailsOnMax() {
    Set<ConstraintViolation<CreateFactRequest>> violations = getValidator().validate(new CreateFactRequest()
            .setType("type")
            .setValue("value")
            .setConfidence(1.1f)
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "confidence");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new CreateFactRequest()
            .setType("type")
            .setValue("value")
    ).isEmpty());
  }

}
