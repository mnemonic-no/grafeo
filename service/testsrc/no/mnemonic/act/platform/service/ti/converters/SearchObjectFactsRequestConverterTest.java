package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.request.v1.Dimension;
import no.mnemonic.act.platform.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.resolvers.SearchByNameResolver;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SearchObjectFactsRequestConverterTest {

  @Mock
  private SearchByNameResolver byNameResolver;
  @Mock
  private SecurityContext securityContext;

  private SearchObjectFactsRequestConverter converter;

  @Before
  public void setup() {
    initMocks(this);

    when(securityContext.getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(securityContext.getAvailableOrganizationID()).thenReturn(SetUtils.set(UUID.randomUUID()));

    converter = new SearchObjectFactsRequestConverter(byNameResolver, securityContext);
  }

  @Test
  public void testConvertNullReturnsNull() throws Exception {
    assertNull(converter.apply(null));
  }

  @Test
  public void testConvertEmptyRequest() throws Exception {
    FactSearchCriteria criteria = converter.apply(new SearchObjectFactsRequest());
    assertEquals(SetUtils.set(FactSearchCriteria.NumberFieldStrategy.certainty), criteria.getNumberFieldStrategy());
    assertEquals(25, criteria.getLimit());
    assertNotNull(criteria.getCurrentUserID());
    assertNotNull(criteria.getAvailableOrganizationID());
  }

  @Test
  public void testConvertRequestFilterByKeywords() throws Exception {
    FactSearchCriteria criteria = converter.apply(new SearchObjectFactsRequest().setKeywords("keyword"));
    assertEquals("keyword", criteria.getKeywords());
    assertEquals(SetUtils.set(FactSearchCriteria.KeywordFieldStrategy.factValue), criteria.getKeywordFieldStrategy());
    assertEquals(FactSearchCriteria.MatchStrategy.any, criteria.getKeywordMatchStrategy());
  }

  @Test
  public void testConvertRequestFilterOnObject() throws Exception {
    UUID id = UUID.randomUUID();
    FactSearchCriteria criteria = converter.apply(new SearchObjectFactsRequest()
            .setObjectID(id)
    );
    assertEquals(SetUtils.set(id), criteria.getObjectID());
  }

  @Test
  public void testConvertRequestFilterOnFactType() throws Exception {
    UUID id = UUID.randomUUID();
    UUID idForName = UUID.randomUUID();
    when(byNameResolver.resolveFactType(notNull())).thenReturn(SetUtils.set(id, idForName));

    FactSearchCriteria criteria = converter.apply(new SearchObjectFactsRequest()
            .addFactType(id.toString())
            .addFactType("name")
    );

    assertEquals(SetUtils.set(id, idForName), criteria.getFactTypeID());
    verify(byNameResolver).resolveFactType(SetUtils.set(id.toString(), "name"));
  }

  @Test
  public void testConvertRequestFilterOnFactValue() throws Exception {
    FactSearchCriteria criteria = converter.apply(new SearchObjectFactsRequest()
            .addFactValue("value"));
    assertEquals(SetUtils.set("value"), criteria.getFactValue());
  }

  @Test
  public void testConvertRequestFilterOnOrganization() throws Exception {
    UUID id = UUID.randomUUID();
    UUID idForName = UUID.randomUUID();
    when(byNameResolver.resolveOrganization(notNull())).thenReturn(SetUtils.set(id, idForName));

    FactSearchCriteria criteria = converter.apply(new SearchObjectFactsRequest()
            .addOrganization(id.toString())
            .addOrganization("name")
    );

    assertEquals(SetUtils.set(id, idForName), criteria.getOrganizationID());
    verify(byNameResolver).resolveOrganization(SetUtils.set(id.toString(), "name"));
  }

  @Test
  public void testConvertRequestFilterOnOrigin() throws Exception {
    UUID id = UUID.randomUUID();
    UUID idForName = UUID.randomUUID();
    when(byNameResolver.resolveOrigin(notNull())).thenReturn(SetUtils.set(id, idForName));

    FactSearchCriteria criteria = converter.apply(new SearchObjectFactsRequest()
            .addOrigin(id.toString())
            .addOrigin("name")
    );

    assertEquals(SetUtils.set(id, idForName), criteria.getOriginID());
    verify(byNameResolver).resolveOrigin(SetUtils.set(id.toString(), "name"));
  }

  @Test
  public void testConvertRequestFilterOnMinMax() throws Exception {
    FactSearchCriteria criteria = converter.apply(new SearchObjectFactsRequest()
            .setMinimum(0.1f)
            .setMaximum(0.2f)
            .setDimension(Dimension.trust)
    );
    assertEquals(0.1f, (Float) criteria.getMinNumber(), 0.0);
    assertEquals(0.2f, (Float) criteria.getMaxNumber(), 0.0);
    assertEquals(SetUtils.set(FactSearchCriteria.NumberFieldStrategy.trust), criteria.getNumberFieldStrategy());
    assertEquals(FactSearchCriteria.MatchStrategy.any, criteria.getNumberMatchStrategy());
  }

  @Test
  public void testConvertRequestFilterOnTimestamp() throws Exception {
    FactSearchCriteria criteria = converter.apply(new SearchObjectFactsRequest()
            .setAfter(123456789L)
            .setBefore(987654321L)
    );
    assertEquals(123456789L, (long) criteria.getStartTimestamp());
    assertEquals(987654321L, (long) criteria.getEndTimestamp());
    assertEquals(SetUtils.set(FactSearchCriteria.TimeFieldStrategy.timestamp), criteria.getTimeFieldStrategy());
    assertEquals(FactSearchCriteria.MatchStrategy.any, criteria.getTimeMatchStrategy());
  }

  @Test
  public void testConvertRequestWithLimit() throws Exception {
    FactSearchCriteria criteria = converter.apply(new SearchObjectFactsRequest()
            .setLimit(123));
    assertEquals(123, criteria.getLimit());
  }
}
