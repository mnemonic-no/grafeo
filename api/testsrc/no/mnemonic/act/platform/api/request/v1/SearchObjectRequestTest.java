package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SearchObjectRequestTest extends AbstractRequestTest {

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
            "origin : ['origin']," +
            "minimum : 0.1," +
            "maximum : 0.2," +
            "dimension : 'trust'," +
            "before : '2016-11-30T15:47:00Z'," +
            "after : '2016-11-30T15:47:01Z'," +
            "limit : 25," +
            "includeStatistics : true" +
            "}", objectID, factID);

    SearchObjectRequest request = getMapper().readValue(json, SearchObjectRequest.class);
    assertEquals("keyword", request.getKeywords());
    assertEquals(SetUtils.set(objectID), request.getObjectID());
    assertEquals(SetUtils.set(factID), request.getFactID());
    assertEquals(SetUtils.set("objectType"), request.getObjectType());
    assertEquals(SetUtils.set("factType"), request.getFactType());
    assertEquals(SetUtils.set("objectValue"), request.getObjectValue());
    assertEquals(SetUtils.set("factValue"), request.getFactValue());
    assertEquals(SetUtils.set("organization"), request.getOrganization());
    assertEquals(SetUtils.set("origin"), request.getOrigin());
    assertEquals(0.1f, request.getMinimum(), 0.0);
    assertEquals(0.2f, request.getMaximum(), 0.0);
    assertEquals(Dimension.trust, request.getDimension());
    assertEquals(1480520820000L, request.getBefore().longValue());
    assertEquals(1480520821000L, request.getAfter().longValue());
    assertEquals(25, request.getLimit().intValue());
    assertTrue(request.isIncludeStatistics());
  }

  @Test
  public void testDecodeTimeFieldSearchRequest() throws Exception {
    String json = "{" +
            "startTimestamp : '2016-11-30T15:47:00Z'," +
            "endTimestamp : '2016-11-30T15:47:01Z'," +
            "timeMatchStrategy : 'all'," +
            "timeFieldStrategy : ['all']" +
            "}";

    SearchObjectRequest request = getMapper().readValue(json, SearchObjectRequest.class);
    assertEquals(1480520820000L, request.getStartTimestamp().longValue());
    assertEquals(1480520821000L, request.getEndTimestamp().longValue());
    assertEquals(TimeFieldSearchRequest.TimeMatchStrategy.all, request.getTimeMatchStrategy());
    assertEquals(SetUtils.set(TimeFieldSearchRequest.TimeFieldStrategy.all), request.getTimeFieldStrategy());
  }

  @Test
  public void testRequestValidationFailsOnMin() {
    Set<ConstraintViolation<SearchObjectRequest>> violations = getValidator().validate(new SearchObjectRequest()
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
    Set<ConstraintViolation<SearchObjectRequest>> violations = getValidator().validate(new SearchObjectRequest()
            .setMinimum(1.1f)
            .setMaximum(1.2f)
    );

    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "minimum");
    assertPropertyInvalid(violations, "maximum");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new SearchObjectRequest()).isEmpty());
  }

}
