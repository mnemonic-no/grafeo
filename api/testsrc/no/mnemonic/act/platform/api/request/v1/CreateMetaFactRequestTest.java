package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CreateMetaFactRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID fact = UUID.randomUUID();
    UUID organization = UUID.randomUUID();
    UUID source = UUID.randomUUID();
    UUID acl = UUID.randomUUID();
    String json = String.format("{" +
            "fact : '%s'," +
            "type : 'factType'," +
            "value : 'factValue'," +
            "organization : '%s'," +
            "source : '%s'," +
            "accessMode : 'Explicit'," +
            "comment : 'comment'," +
            "acl : ['%s']" +
            "}", fact, organization, source, acl);

    CreateMetaFactRequest request = getMapper().readValue(json, CreateMetaFactRequest.class);
    assertEquals(fact, request.getFact());
    assertEquals("factType", request.getType());
    assertEquals("factValue", request.getValue());
    assertEquals(organization, request.getOrganization());
    assertEquals(source, request.getSource());
    assertEquals(AccessMode.Explicit, request.getAccessMode());
    assertEquals("comment", request.getComment());
    assertEquals(ListUtils.list(acl), request.getAcl());
  }

  @Test
  public void testRequestValidationFailsOnNull() {
    Set<ConstraintViolation<CreateMetaFactRequest>> violations = getValidator().validate(new CreateMetaFactRequest());
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "fact");
    assertPropertyInvalid(violations, "type");
  }

  @Test
  public void testRequestValidationFailsOnEmpty() {
    Set<ConstraintViolation<CreateMetaFactRequest>> violations = getValidator().validate(new CreateMetaFactRequest()
            .setFact(UUID.randomUUID())
            .setType("")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "type");
  }

  @Test
  public void testRequestValidationFailsOnBlank() {
    Set<ConstraintViolation<CreateMetaFactRequest>> violations = getValidator().validate(new CreateMetaFactRequest()
            .setFact(UUID.randomUUID())
            .setType(" ")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "type");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new CreateMetaFactRequest()
            .setFact(UUID.randomUUID())
            .setType("type")
    ).isEmpty());
  }

}
