package no.mnemonic.act.platform.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TraverseGraphByObjectSearchRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    String json = "{" +
            "search : {" +
            "  objectType : ['objectType']," +
            "  objectValue : ['objectValue']" +
            "}," +
            "traverse : {" +
            "  query : 'g.out()'" +
            "}" +
            "}";

    TraverseGraphByObjectSearchRequest request = getMapper().readValue(json, TraverseGraphByObjectSearchRequest.class);

    SearchObjectRequest search = request.getSearch();
    assertEquals(set("objectType"), search.getObjectType());
    assertEquals(set("objectValue"), search.getObjectValue());

    TraverseGraphRequest traverse = request.getTraverse();
    assertEquals("g.out()", traverse.getQuery());
  }

  @Test
  public void testRequestValidationFailsOnNullSearchAndTraverse() {
    Set<ConstraintViolation<TraverseGraphByObjectSearchRequest>> violations = getValidator()
            .validate(new TraverseGraphByObjectSearchRequest());
    assertEquals(2, violations.size());
    assertPropertyInvalid(violations, "search");
    assertPropertyInvalid(violations, "traverse");
  }

  @Test
  public void testRequestValidationFailsOnNullQuery() {
    Set<ConstraintViolation<TraverseGraphByObjectSearchRequest>> violations = getValidator()
            .validate(new TraverseGraphByObjectSearchRequest()
                    .setSearch(new SearchObjectRequest())
                    .setTraverse(new TraverseGraphRequest())
            );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnEmptyQuery() {
    Set<ConstraintViolation<TraverseGraphByObjectSearchRequest>> violations = getValidator()
            .validate(new TraverseGraphByObjectSearchRequest()
                    .setSearch(new SearchObjectRequest())
                    .setTraverse(new TraverseGraphRequest().setQuery(""))
            );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationFailsOnBlankQuery() {
    Set<ConstraintViolation<TraverseGraphByObjectSearchRequest>> violations = getValidator()
            .validate(new TraverseGraphByObjectSearchRequest()
                    .setSearch(new SearchObjectRequest())
                    .setTraverse(new TraverseGraphRequest().setQuery(" "))
            );
    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "query");
  }

  @Test
  public void testRequestValidationSucceeds() {
    Set<ConstraintViolation<TraverseGraphByObjectSearchRequest>> violations = getValidator()
            .validate(new TraverseGraphByObjectSearchRequest()
                    .setSearch(new SearchObjectRequest())
                    .setTraverse(new TraverseGraphRequest()
                            .setQuery("g.out()")
                            .setAfter(1400000000000L)
                            .setBefore(1500000000000L)
                            .setIncludeRetracted(true))
            );
    assertTrue(violations.isEmpty());
  }
}
