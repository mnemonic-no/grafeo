package no.mnemonic.services.grafeo.api.request.v1;

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
            "origin : ['origin']," +
            "minimum : 0.1," +
            "maximum : 0.2," +
            "dimension : 'trust'," +
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
    assertEquals(SetUtils.set("origin"), request.getOrigin());
    assertEquals(0.1f, request.getMinimum(), 0.0);
    assertEquals(0.2f, request.getMaximum(), 0.0);
    assertEquals(Dimension.trust, request.getDimension());
    assertTrue(request.getIncludeRetracted());
    assertEquals(1480520820000L, request.getBefore().longValue());
    assertEquals(1480520821000L, request.getAfter().longValue());
    assertEquals(25, request.getLimit().intValue());
  }

  @Test
  public void testDecodeTimeFieldSearchRequest() throws Exception {
    String json = "{" +
            "startTimestamp : '2016-11-30T15:47:00Z'," +
            "endTimestamp : '2016-11-30T15:47:01Z'," +
            "timeMatchStrategy : 'all'," +
            "timeFieldStrategy : ['all']" +
            "}";

    SearchObjectFactsRequest request = getMapper().readValue(json, SearchObjectFactsRequest.class);
    assertEquals(1480520820000L, request.getStartTimestamp().longValue());
    assertEquals(1480520821000L, request.getEndTimestamp().longValue());
    assertEquals(TimeFieldSearchRequest.TimeMatchStrategy.all, request.getTimeMatchStrategy());
    assertEquals(SetUtils.set(TimeFieldSearchRequest.TimeFieldStrategy.all), request.getTimeFieldStrategy());
  }

  @Test
  public void testRequestValidationFailsOnMin() {
    Set<ConstraintViolation<SearchObjectFactsRequest>> violations = getValidator().validate(new SearchObjectFactsRequest()
            .setMinimum(-0.1f)
            .setMaximum(-0.2f)
            .setLimit(-1)
    );

    assertEquals(3, violations.size());
    assertPropertyInvalid(violations, "minimum");
    assertPropertyInvalid(violations, "maximum");
    assertPropertyInvalid(violations, "limit");
  }

  @Test
  public void testRequestValidationFailsOnMax() {
    Set<ConstraintViolation<SearchObjectFactsRequest>> violations = getValidator().validate(new SearchObjectFactsRequest()
            .setMinimum(1.1f)
            .setMaximum(1.2f)
    );

    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "minimum");
    assertPropertyInvalid(violations, "maximum");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new SearchObjectFactsRequest()).isEmpty());
  }

}
