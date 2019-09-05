package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SearchMetaFactsRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    UUID fact = UUID.randomUUID();
    String json = String.format("{" +
            "fact : '%s'," +
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
            "}", fact);

    SearchMetaFactsRequest request = getMapper().readValue(json, SearchMetaFactsRequest.class);
    assertEquals(fact, request.getFact());
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
  public void testRequestValidationFailsOnNull() {
    Set<ConstraintViolation<SearchMetaFactsRequest>> violations = getValidator().validate(new SearchMetaFactsRequest());

    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "fact");
  }

  @Test
  public void testRequestValidationFailsOnMin() {
    Set<ConstraintViolation<SearchMetaFactsRequest>> violations = getValidator().validate(new SearchMetaFactsRequest()
            .setFact(UUID.randomUUID())
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
    Set<ConstraintViolation<SearchMetaFactsRequest>> violations = getValidator().validate(new SearchMetaFactsRequest()
            .setFact(UUID.randomUUID())
            .setMinimum(1.1f)
            .setMaximum(1.2f)
    );

    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "minimum");
    assertPropertyInvalid(violations, "maximum");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new SearchMetaFactsRequest().setFact(UUID.randomUUID())).isEmpty());
  }

}
