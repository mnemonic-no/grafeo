package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TraverseByObjectSearchRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    String json = "{" +
            "objectType : ['objectType']," +
            "factType : ['factType']," +
            "objectValue : ['objectValue']," +
            "factValue : ['factValue']," +
            "source : ['source']," +
            "before : '2016-11-30T15:47:00Z'," +
            "after : '2016-11-30T15:47:01Z'," +
            "limit : 25," +
            "query : 'g.out()'" +
            "}";

    TraverseByObjectSearchRequest request = getMapper().readValue(json, TraverseByObjectSearchRequest.class);
    assertEquals(SetUtils.set("objectType"), request.getObjectType());
    assertEquals(SetUtils.set("factType"), request.getFactType());
    assertEquals(SetUtils.set("objectValue"), request.getObjectValue());
    assertEquals(SetUtils.set("factValue"), request.getFactValue());
    assertEquals(SetUtils.set("source"), request.getSource());
    assertEquals(1480520820000L, request.getBefore().longValue());
    assertEquals(1480520821000L, request.getAfter().longValue());
    assertEquals(25, request.getLimit().intValue());
    assertEquals("g.out()", request.getQuery());
  }

  @Test
  public void testRequestValidationFailsOnNull() {
    Set<ConstraintViolation<TraverseByObjectSearchRequest>> violations = getValidator().validate(new TraverseByObjectSearchRequest());
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnEmpty() {
    Set<ConstraintViolation<TraverseByObjectSearchRequest>> violations = getValidator().validate(new TraverseByObjectSearchRequest()
            .setQuery("")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnBlank() {
    Set<ConstraintViolation<TraverseByObjectSearchRequest>> violations = getValidator().validate(new TraverseByObjectSearchRequest()
            .setQuery(" ")
    );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new TraverseByObjectSearchRequest().setQuery("g.out()")).isEmpty());
  }

}
