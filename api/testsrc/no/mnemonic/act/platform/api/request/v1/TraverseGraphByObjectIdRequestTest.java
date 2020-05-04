package no.mnemonic.act.platform.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TraverseGraphByObjectIdRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    String json = "{" +
            "before : '2016-11-30T15:47:00Z'," +
            "after : '2016-11-30T15:47:01Z'," +
            "includeRetracted : true," +
            "query : 'g.out()'" +
            "}";
    TraverseGraphByObjectIdRequest request = getMapper().readValue(json, TraverseGraphByObjectIdRequest.class);

    assertEquals(1480520820000L, request.getBefore().longValue());
    assertEquals(1480520821000L, request.getAfter().longValue());
    assertEquals(true, request.getIncludeRetracted());
    assertEquals("g.out()", request.getQuery());
  }

  @Test
  public void testRequestValidationFailsOnNull() {
    Set<ConstraintViolation<TraverseGraphByObjectIdRequest>> violations = getValidator().validate(new TraverseGraphByObjectIdRequest());
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "id");
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnEmpty() {
    Set<ConstraintViolation<TraverseGraphByObjectIdRequest>> violations = getValidator().validate(new TraverseGraphByObjectIdRequest()
            .setId(UUID.randomUUID())
            .setQuery("")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnBlank() {
    Set<ConstraintViolation<TraverseGraphByObjectIdRequest>> violations = getValidator().validate(new TraverseGraphByObjectIdRequest()
            .setId(UUID.randomUUID())
            .setQuery(" ")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new TraverseGraphByObjectIdRequest().setId(UUID.randomUUID()).setQuery("g.out()")).isEmpty());
  }

}
