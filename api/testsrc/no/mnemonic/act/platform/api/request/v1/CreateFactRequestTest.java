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
    UUID organization = UUID.randomUUID();
    UUID source = UUID.randomUUID();
    UUID acl = UUID.randomUUID();
    UUID objectID = UUID.randomUUID();
    String json = String.format("{" +
            "type : 'factType'," +
            "value : 'factValue'," +
            "organization : '%s'," +
            "source : '%s'," +
            "accessMode : 'Explicit'," +
            "comment : 'comment'," +
            "acl : ['%s']," +
            "sourceObject : 'type/value'," +
            "destinationObject : '%s'," +
            "bidirectionalBinding : true" +
            "}", organization, source, acl, objectID);

    CreateFactRequest request = getMapper().readValue(json, CreateFactRequest.class);
    assertEquals("factType", request.getType());
    assertEquals("factValue", request.getValue());
    assertEquals(organization, request.getOrganization());
    assertEquals(source, request.getSource());
    assertEquals(AccessMode.Explicit, request.getAccessMode());
    assertEquals("comment", request.getComment());
    assertEquals(ListUtils.list(acl), request.getAcl());
    assertEquals("type/value", request.getSourceObject());
    assertEquals(objectID, UUID.fromString(request.getDestinationObject()));
    assertTrue(request.isBidirectionalBinding());
  }

  @Test
  public void testRequestValidationFailsOnNull() {
    Set<ConstraintViolation<CreateFactRequest>> violations = getValidator().validate(new CreateFactRequest());
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "type");
    assertPropertyInvalid(violations, "value");
  }

  @Test
  public void testRequestValidationFailsOnEmpty() {
    Set<ConstraintViolation<CreateFactRequest>> violations = getValidator().validate(new CreateFactRequest()
            .setType("")
            .setValue("")
    );
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "type");
    assertPropertyInvalid(violations, "value");
  }

  @Test
  public void testRequestValidationFailsOnBlank() {
    Set<ConstraintViolation<CreateFactRequest>> violations = getValidator().validate(new CreateFactRequest()
            .setType(" ")
            .setValue(" ")
    );
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "type");
    assertPropertyInvalid(violations, "value");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new CreateFactRequest()
            .setType("type")
            .setValue("value")
    ).isEmpty());
  }

}
