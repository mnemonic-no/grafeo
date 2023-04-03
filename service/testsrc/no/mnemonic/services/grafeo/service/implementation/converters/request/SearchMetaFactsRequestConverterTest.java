package no.mnemonic.services.grafeo.service.implementation.converters.request;

import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.request.v1.Dimension;
import no.mnemonic.services.grafeo.api.request.v1.SearchMetaFactsRequest;
import no.mnemonic.services.grafeo.api.request.v1.TimeFieldSearchRequest;
import no.mnemonic.services.grafeo.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.services.grafeo.service.implementation.resolvers.AccessControlCriteriaResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.IndexSelectCriteriaResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.SearchByNameRequestResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SearchMetaFactsRequestConverterTest {

  @Mock
  private SearchByNameRequestResolver byNameResolver;
  @Mock
  private AccessControlCriteriaResolver accessControlCriteriaResolver;
  @Mock
  private IndexSelectCriteriaResolver indexSelectCriteriaResolver;

  private SearchMetaFactsRequestConverter converter;

  @Before
  public void setup() throws Exception {
    initMocks(this);

    when(accessControlCriteriaResolver.get()).thenReturn(AccessControlCriteria.builder()
            .addCurrentUserIdentity(UUID.randomUUID())
            .addAvailableOrganizationID(UUID.randomUUID())
            .build());
    when(indexSelectCriteriaResolver.validateAndCreateCriteria(any(), any()))
            .thenReturn(IndexSelectCriteria.builder().build());

    converter = new SearchMetaFactsRequestConverter(byNameResolver, accessControlCriteriaResolver, indexSelectCriteriaResolver);
  }

  @Test
  public void testConvertNullReturnsNull() throws Exception {
    assertNull(converter.apply(null));
  }

  @Test
  public void testConvertEmptyRequest() throws Exception {
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest());
    assertEquals(SetUtils.set(FactSearchCriteria.NumberFieldStrategy.certainty), criteria.getNumberFieldStrategy());
    assertEquals(25, criteria.getLimit());
    assertNotNull(criteria.getAccessControlCriteria());
    assertNotNull(criteria.getIndexSelectCriteria());
  }

  @Test
  public void testConvertRequestFilterOnFact() throws Exception {
    UUID fact = UUID.randomUUID();
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest()
            .setFact(fact)
    );
    assertEquals(SetUtils.set(fact), criteria.getInReferenceTo());
  }

  @Test
  public void testConvertRequestFilterByKeywords() throws Exception {
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest().setKeywords("keyword"));
    assertEquals("keyword", criteria.getKeywords());
    assertEquals(SetUtils.set(
            FactSearchCriteria.KeywordFieldStrategy.factValueText,
            FactSearchCriteria.KeywordFieldStrategy.factValueIp,
            FactSearchCriteria.KeywordFieldStrategy.factValueDomain
    ), criteria.getKeywordFieldStrategy());
    assertEquals(FactSearchCriteria.MatchStrategy.any, criteria.getKeywordMatchStrategy());
  }

  @Test
  public void testConvertRequestFilterOnFactType() throws Exception {
    UUID id = UUID.randomUUID();
    UUID idForName = UUID.randomUUID();
    when(byNameResolver.resolveFactType(notNull())).thenReturn(SetUtils.set(id, idForName));

    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest()
            .addFactType(id.toString())
            .addFactType("name")
    );

    assertEquals(SetUtils.set(id, idForName), criteria.getFactTypeID());
    verify(byNameResolver).resolveFactType(SetUtils.set(id.toString(), "name"));
  }

  @Test
  public void testConvertRequestFilterOnFactValue() throws Exception {
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest()
            .addFactValue("value"));
    assertEquals(SetUtils.set("value"), criteria.getFactValue());
  }

  @Test
  public void testConvertRequestFilterOnOrganization() throws Exception {
    UUID id = UUID.randomUUID();
    UUID idForName = UUID.randomUUID();
    when(byNameResolver.resolveOrganization(notNull())).thenReturn(SetUtils.set(id, idForName));

    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest()
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

    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest()
            .addOrigin(id.toString())
            .addOrigin("name")
    );

    assertEquals(SetUtils.set(id, idForName), criteria.getOriginID());
    verify(byNameResolver).resolveOrigin(SetUtils.set(id.toString(), "name"));
  }

  @Test
  public void testConvertRequestFilterOnMinMax() throws Exception {
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest()
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
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest()
            .setStartTimestamp(123456789L)
            .setEndTimestamp(987654321L)
            .addTimeFieldStrategy(TimeFieldSearchRequest.TimeFieldStrategy.all)
            .setTimeMatchStrategy(TimeFieldSearchRequest.TimeMatchStrategy.all)
    );
    assertEquals(123456789L, (long) criteria.getStartTimestamp());
    assertEquals(987654321L, (long) criteria.getEndTimestamp());
    assertEquals(SetUtils.set(FactSearchCriteria.TimeFieldStrategy.all), criteria.getTimeFieldStrategy());
    assertEquals(FactSearchCriteria.MatchStrategy.all, criteria.getTimeMatchStrategy());

    verify(indexSelectCriteriaResolver).validateAndCreateCriteria(123456789L, 987654321L);
  }

  @Test
  public void testConvertRequestWithLimit() throws Exception {
    FactSearchCriteria criteria = converter.apply(new SearchMetaFactsRequest()
            .setLimit(123));
    assertEquals(123, criteria.getLimit());
  }
}
