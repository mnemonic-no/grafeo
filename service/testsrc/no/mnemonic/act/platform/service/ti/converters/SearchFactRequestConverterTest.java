package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.request.v1.SearchFactRequest;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class SearchFactRequestConverterTest {

  private final SearchFactRequestConverter converter = SearchFactRequestConverter.builder()
          .setCurrentUserIdSupplier(UUID::randomUUID)
          .setAvailableOrganizationIdSupplier(() -> SetUtils.set(UUID.randomUUID()))
          .build();

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutCurrentUserIdSupplierThrowsException() {
    SearchFactRequestConverter.builder()
            .setAvailableOrganizationIdSupplier(() -> SetUtils.set(UUID.randomUUID()))
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutAvailableOrganizationIdSupplierThrowsException() {
    SearchFactRequestConverter.builder()
            .setCurrentUserIdSupplier(UUID::randomUUID)
            .build();
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }

  @Test
  public void testConvertEmptyRequest() {
    FactSearchCriteria criteria = converter.apply(new SearchFactRequest());
    assertFalse(criteria.getRetracted());
    assertEquals(25, criteria.getLimit());
    assertNotNull(criteria.getCurrentUserID());
    assertNotNull(criteria.getAvailableOrganizationID());
  }

  @Test
  public void testConvertRequestFilterOnObjectType() {
    UUID id = UUID.randomUUID();
    String name = "name";
    FactSearchCriteria criteria = converter.apply(new SearchFactRequest()
            .addObjectType(id.toString())
            .addObjectType(name)
    );
    assertEquals(SetUtils.set(id), criteria.getObjectTypeID());
    assertEquals(SetUtils.set(name), criteria.getObjectTypeName());
  }

  @Test
  public void testConvertRequestFilterOnFactType() {
    UUID id = UUID.randomUUID();
    String name = "name";
    FactSearchCriteria criteria = converter.apply(new SearchFactRequest()
            .addFactType(id.toString())
            .addFactType(name)
    );
    assertEquals(SetUtils.set(id), criteria.getFactTypeID());
    assertEquals(SetUtils.set(name), criteria.getFactTypeName());
  }

  @Test
  public void testConvertRequestFilterOnObjectValue() {
    FactSearchCriteria criteria = converter.apply(new SearchFactRequest().addObjectValue("value"));
    assertEquals(SetUtils.set("value"), criteria.getObjectValue());
  }

  @Test
  public void testConvertRequestFilterOnFactValue() {
    FactSearchCriteria criteria = converter.apply(new SearchFactRequest().addFactValue("value"));
    assertEquals(SetUtils.set("value"), criteria.getFactValue());
  }

  @Test
  public void testConvertRequestFilterOnOrganization() {
    UUID id = UUID.randomUUID();
    String name = "name";
    FactSearchCriteria criteria = converter.apply(new SearchFactRequest()
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
    FactSearchCriteria criteria = converter.apply(new SearchFactRequest()
            .addSource(id.toString())
            .addSource(name)
    );
    assertEquals(SetUtils.set(id), criteria.getSourceID());
    assertEquals(SetUtils.set(name), criteria.getSourceName());
  }

  @Test
  public void testConvertRequestIncludeRetracted() {
    FactSearchCriteria criteria = converter.apply(new SearchFactRequest().setIncludeRetracted(true));
    assertNull(criteria.getRetracted());
  }

  @Test
  public void testConvertRequestExcludeRetracted() {
    FactSearchCriteria criteria = converter.apply(new SearchFactRequest().setIncludeRetracted(false));
    assertFalse(criteria.getRetracted());
  }

  @Test
  public void testConvertRequestFilterOnTimestamp() {
    FactSearchCriteria criteria = converter.apply(new SearchFactRequest()
            .setAfter(123456789L)
            .setBefore(987654321L)
    );
    assertEquals(123456789L, (long) criteria.getStartTimestamp());
    assertEquals(987654321L, (long) criteria.getEndTimestamp());
    assertEquals(SetUtils.set(FactSearchCriteria.TimeFieldStrategy.timestamp), criteria.getTimeFieldStrategy());
  }

  @Test
  public void testConvertRequestWithLimit() {
    FactSearchCriteria criteria = converter.apply(new SearchFactRequest().setLimit(123));
    assertEquals(123, criteria.getLimit());
  }

}
