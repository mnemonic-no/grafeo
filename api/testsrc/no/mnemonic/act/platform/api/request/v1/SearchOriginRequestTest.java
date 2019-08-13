package no.mnemonic.act.platform.api.request.v1;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SearchOriginRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    String json = "{ includeDeleted : true, limit : 25 }";

    SearchOriginRequest request = getMapper().readValue(json, SearchOriginRequest.class);
    assertTrue(request.getIncludeDeleted());
    assertEquals(25, request.getLimit().intValue());
  }

  @Test
  public void testRequestValidationFailsOnMin() {
    Set<ConstraintViolation<SearchOriginRequest>> violations = getValidator().validate(new SearchOriginRequest()
            .setLimit(-1)
    );

    assertEquals(1, violations.size());
    assertPropertyInvalid(violations, "limit");
  }

  @Test
  public void testRequestValidationSucceeds() {
    assertTrue(getValidator().validate(new SearchOriginRequest()).isEmpty());
  }

}
