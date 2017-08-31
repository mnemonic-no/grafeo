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
    UUID inReferenceTo = UUID.randomUUID();
    UUID organization = UUID.randomUUID();
    UUID source = UUID.randomUUID();
    UUID acl = UUID.randomUUID();
    UUID objectID = UUID.randomUUID();
    String json = String.format("{" +
            "type : 'factType'," +
            "value : 'factValue'," +
            "inReferenceTo : '%s'," +
            "organization : '%s'," +
            "source : '%s'," +
            "accessMode : 'Explicit'," +
            "comment : 'comment'," +
            "acl : ['%s']," +
            "bindings : [{ objectID : '%s', objectType : 'objectType', objectValue : 'objectValue', direction : 'BiDirectional' }]" +
            "}", inReferenceTo, organization, source, acl, objectID);

    CreateFactRequest request = getMapper().readValue(json, CreateFactRequest.class);
    assertEquals("factType", request.getType());
    assertEquals("factValue", request.getValue());
    assertEquals(inReferenceTo, request.getInReferenceTo());
    assertEquals(organization, request.getOrganization());
    assertEquals(source, request.getSource());
    assertEquals(AccessMode.Explicit, request.getAccessMode());
    assertEquals("comment", request.getComment());
    assertEquals(ListUtils.list(acl), request.getAcl());
    assertEquals(1, request.getBindings().size());
    assertEquals(objectID, request.getBindings().get(0).getObjectID());
    assertEquals("objectType", request.getBindings().get(0).getObjectType());
    assertEquals("objectValue", request.getBindings().get(0).getObjectValue());
    assertEquals(Direction.BiDirectional, request.getBindings().get(0).getDirection());
  }

  @Test
  public void testRequestValidationFailsOnNull() {
    Set<ConstraintViolation<CreateFactRequest>> violations = getValidator().validate(new CreateFactRequest());
    assertEquals(3, violations.size());
    assertPropertyInvalid(violations, "type");
    assertPropertyInvalid(violations, "value");
    assertPropertyInvalid(violations, "bindings");
  }

  @Test
  public void testRequestValidationFailsOnEmpty() {
    Set<ConstraintViolation<CreateFactRequest>> violations = getValidator().validate(new CreateFactRequest()
            .setType("")
            .setValue("")
            .setBindings(ListUtils.list())
    );
    assertEquals(3, violations.size());
    assertPropertyInvalid(violations, "type");
    assertPropertyInvalid(violations, "value");
    assertPropertyInvalid(violations, "bindings");
  }

  @Test
  public void testRequestValidationFailsOnBlank() {
    Set<ConstraintViolation<CreateFactRequest>> violations = getValidator().validate(new CreateFactRequest()
            .setType(" ")
            .setValue(" ")
            .setBindings(ListUtils.list(new CreateFactRequest.FactObjectBinding().setDirection(Direction.BiDirectional)))
    );
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "type");
    assertPropertyInvalid(violations, "value");
  }

  @Test
  public void testRequestValidationFailsOnValid() {
    Set<ConstraintViolation<CreateFactRequest>> violations = getValidator().validate(new CreateFactRequest()
            .setType("type")
            .setValue("value")
            .addBinding(new CreateFactRequest.FactObjectBinding())
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "direction");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new CreateFactRequest()
            .setType("type")
            .setValue("value")
            .addBinding(new CreateFactRequest.FactObjectBinding().setDirection(Direction.BiDirectional))
    ).isEmpty());
  }

}
