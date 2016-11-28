package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RetractFactRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID fact = UUID.randomUUID();
    UUID organization = UUID.randomUUID();
    UUID source = UUID.randomUUID();
    UUID acl = UUID.randomUUID();
    String json = String.format("{" +
            "fact : '%s'," +
            "organization : '%s'," +
            "source : '%s'," +
            "accessMode : 'Explicit'," +
            "comment : 'comment'," +
            "acl : ['%s']" +
            "}", fact, organization, source, acl);

    RetractFactRequest request = getMapper().readValue(json, RetractFactRequest.class);
    assertEquals(fact, request.getFact());
    assertEquals(organization, request.getOrganization());
    assertEquals(source, request.getSource());
    assertEquals(AccessMode.Explicit, request.getAccessMode());
    assertEquals("comment", request.getComment());
    assertEquals(ListUtils.list(acl), request.getAcl());
  }

  @Test
  public void testRequestValidationFailsOnNotNull() {
    Set<ConstraintViolation<RetractFactRequest>> violations = getValidator().validate(new RetractFactRequest());
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "fact");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new RetractFactRequest().setFact(UUID.randomUUID())).isEmpty());
  }

}
