package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SearchObjectFactsRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID objectID = UUID.randomUUID();
    String json = String.format("{" +
            "objectID : '%s'," +
            "objectType : 'objectType'," +
            "objectValue : 'objectValue'," +
            "keywords : 'keyword'," +
            "factType : ['factType']," +
            "factValue : ['factValue']," +
            "organization : ['organization']," +
            "source : ['source']," +
            "includeRetracted : true," +
            "before : '2016-11-30T15:47:00Z'," +
            "after : '2016-11-30T15:47:01Z'," +
            "limit : 25" +
            "}", objectID);

    SearchObjectFactsRequest request = getMapper().readValue(json, SearchObjectFactsRequest.class);
    assertEquals(objectID, request.getObjectID());
    assertEquals("objectType", request.getObjectType());
    assertEquals("objectValue", request.getObjectValue());
    assertEquals("keyword", request.getKeywords());
    assertEquals(SetUtils.set("factType"), request.getFactType());
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
    Set<ConstraintViolation<SearchObjectFactsRequest>> violations = getValidator().validate(new SearchObjectFactsRequest()
            .setLimit(-1)
    );

    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "limit");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new SearchObjectFactsRequest()).isEmpty());
  }

}
