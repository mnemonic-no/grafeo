package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SearchObjectRequestTest extends AbstractRequestTest {

  @Test
  public void testDecodeRequest() throws Exception {
    String json = "{" +
            "type : ['type']," +
            "objectType : ['objectType']," +
            "factType : ['factType']," +
            "value : ['value']," +
            "objectValue : ['objectValue']," +
            "factValue : ['factValue']," +
            "source : ['source']," +
            "before : '2016-11-30T15:47:00Z'," +
            "after : '2016-11-30T15:47:01Z'," +
            "limit : 25" +
            "}";

    SearchObjectRequest request = getMapper().readValue(json, SearchObjectRequest.class);
    assertEquals(SetUtils.set("type"), request.getType());
    assertEquals(SetUtils.set("objectType"), request.getObjectType());
    assertEquals(SetUtils.set("factType"), request.getFactType());
    assertEquals(SetUtils.set("value"), request.getValue());
    assertEquals(SetUtils.set("objectValue"), request.getObjectValue());
    assertEquals(SetUtils.set("factValue"), request.getFactValue());
    assertEquals(SetUtils.set("source"), request.getSource());
    assertEquals(1480520820000L, request.getBefore().longValue());
    assertEquals(1480520821000L, request.getAfter().longValue());
    assertEquals(25, request.getLimit().intValue());
  }

}
