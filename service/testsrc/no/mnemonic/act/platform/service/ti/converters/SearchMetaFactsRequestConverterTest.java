package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.request.v1.SearchMetaFactsRequest;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.api.FactSearchCriteria.KeywordFieldStrategy.*;
import static org.junit.Assert.*;

public class SearchMetaFactsRequestConverterTest {

  private final SearchMetaFactsRequestConverter converter = SearchMetaFactsRequestConverter.builder()
          .setCurrentUserIdSupplier(UUID::randomUUID)
          .setAvailableOrganizationIdSupplier(() -> SetUtils.set(UUID.randomUUID()))
          .build();

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutCurrentUserIdSupplierThrowsException() {
    SearchMetaFactsRequestConverter.builder()
            .setAvailableOrganizationIdSupplier(() -> SetUtils.set(UUID.randomUUID()))
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutAvailableOrganizationIdSupplierThrowsException() {
    SearchMetaFactsRequestConverter.builder()
            .setCurrentUserIdSupplier(UUID::randomUUID)
            .build();
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }

  @Test
  public void testConvertEmptyRequest() {
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest());
    assertFalse(criteria.getRetracted());
    assertEquals(25, criteria.getLimit());
    assertNotNull(criteria.getCurrentUserID());
    assertNotNull(criteria.getAvailableOrganizationID());
  }

  @Test
  public void testConvertRequestFilterOnFact() {
    UUID fact = UUID.randomUUID();
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest()
            .setFact(fact)
    );
    assertEquals(SetUtils.set(fact), criteria.getInReferenceTo());
  }

  @Test
  public void testConvertRequestFilterByKeywords() {
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest().setKeywords("keyword"));
    assertEquals("keyword", criteria.getKeywords());
    assertEquals(SetUtils.set(factValue, organization, source), criteria.getKeywordFieldStrategy());
    assertEquals(FactSearchCriteria.MatchStrategy.any, criteria.getKeywordMatchStrategy());
  }

  @Test
  public void testConvertRequestFilterOnFactType() {
    UUID id = UUID.randomUUID();
    String name = "name";
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest()
            .addFactType(id.toString())
            .addFactType(name)
    );
    assertEquals(SetUtils.set(id), criteria.getFactTypeID());
    assertEquals(SetUtils.set(name), criteria.getFactTypeName());
  }

  @Test
  public void testConvertRequestFilterOnFactValue() {
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest()
            .addFactValue("value"));
    assertEquals(SetUtils.set("value"), criteria.getFactValue());
  }

  @Test
  public void testConvertRequestFilterOnOrganization() {
    UUID id = UUID.randomUUID();
    String name = "name";
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest()
            .addOrganization(id.toString())
            .addOrganization(name)
    );
    assertEquals(SetUtils.set(id), criteria.getOrganizationID());
    assertEquals(SetUtils.set(name), criteria.getOrganizationName());
  }

  @Test
  public void testConvertRequestFilterOnSource() {
    UUID id = UUID.randomUUID();
    String name = "name";
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest()
            .addSource(id.toString())
            .addSource(name)
    );
    assertEquals(SetUtils.set(id), criteria.getSourceID());
    assertEquals(SetUtils.set(name), criteria.getSourceName());
  }

  @Test
  public void testConvertRequestIncludeRetracted() {
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest()
            .setIncludeRetracted(true));
    assertNull(criteria.getRetracted());
  }

  @Test
  public void testConvertRequestExcludeRetracted() {
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest()
            .setIncludeRetracted(false));
    assertFalse(criteria.getRetracted());
  }

  @Test
  public void testConvertRequestFilterOnTimestamp() {
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest()
            .setAfter(123456789L)
            .setBefore(987654321L)
    );
    assertEquals(123456789L, (long) criteria.getStartTimestamp());
    assertEquals(987654321L, (long) criteria.getEndTimestamp());
    assertEquals(SetUtils.set(FactSearchCriteria.TimeFieldStrategy.timestamp), criteria.getTimeFieldStrategy());
  }

  @Test
  public void testConvertRequestWithLimit() {
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest()
            .setLimit(123));
    assertEquals(123, criteria.getLimit());
  }

}
