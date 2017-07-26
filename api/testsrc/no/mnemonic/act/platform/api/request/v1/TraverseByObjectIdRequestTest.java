package no.mnemonic.act.platform.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TraverseByObjectIdRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{ id : '%s', query : 'g.out()' }", id);

    TraverseByObjectIdRequest request = getMapper().readValue(json, TraverseByObjectIdRequest.class);
    assertEquals(id, request.getId());
    assertEquals("g.out()", request.getQuery());
  }

  @Test
  public void testRequestValidationFailsOnNotNull() {
    Set<ConstraintViolation<TraverseByObjectIdRequest>> violations = getValidator().validate(new TraverseByObjectIdRequest());
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "id");
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnSize() {
    Set<ConstraintViolation<TraverseByObjectIdRequest>> violations = getValidator().validate(new TraverseByObjectIdRequest()
            .setId(UUID.randomUUID())
            .setQuery("")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new TraverseByObjectIdRequest().setId(UUID.randomUUID()).setQuery("g.out()")).isEmpty());
  }

}
