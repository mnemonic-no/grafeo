package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SearchFactRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID objectID = UUID.randomUUID();
    UUID factID = UUID.randomUUID();

    String json = String.format("{" +
            "keywords : 'keyword'," +
            "objectID : ['%s']," +
            "factID : ['%s']," +
            "objectType : ['objectType']," +
            "factType : ['factType']," +
            "objectValue : ['objectValue']," +
            "factValue : ['factValue']," +
            "organization : ['organization']," +
            "source : ['source']," +
            "includeRetracted : true," +
            "before : '2016-11-30T15:47:00Z'," +
            "after : '2016-11-30T15:47:01Z'," +
            "limit : 25" +
            "}", objectID, factID);

    SearchFactRequest request = getMapper().readValue(json, SearchFactRequest.class);
    assertEquals("keyword", request.getKeywords());
    assertEquals(SetUtils.set(objectID), request.getObjectID());
    assertEquals(SetUtils.set(factID), request.getFactID());
    assertEquals(SetUtils.set("objectType"), request.getObjectType());
    assertEquals(SetUtils.set("factType"), request.getFactType());
    assertEquals(SetUtils.set("objectValue"), request.getObjectValue());
    assertEquals(SetUtils.set("factValue"), request.getFactValue());
    assertEquals(SetUtils.set("organization"), request.getOrganization());
    assertEquals(SetUtils.set("source"), request.getSource());
    assertTrue(request.getIncludeRetracted());
    assertEquals(1480520820000L, request.getBefore().longValue());
    assertEquals(1480520821000L, request.getAfter().longValue());
    assertEquals(25, request.getLimit().intValue());
  }

  @Test
  public void testRequestValidationFailsOnMin() {
    Set<ConstraintViolation<SearchFactRequest>> violations = getValidator().validate(new SearchFactRequest()
            .setLimit(-1)
    );

    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "limit");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new SearchFactRequest()).isEmpty());
  }

}
