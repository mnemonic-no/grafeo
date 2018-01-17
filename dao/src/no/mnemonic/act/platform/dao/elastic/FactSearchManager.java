package no.mnemonic.act.platform.dao.elastic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.handlers.EntityHandler;
import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.elasticsearch.common.bytes.BytesReference.toBytes;
import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * Class for indexing Facts into ElasticSearch as well as for retrieving and searching indexed Facts.
 */
@Singleton
public class FactSearchManager implements LifecycleAspect {

  private static final String INDEX_NAME = "act";
  private static final String TYPE_NAME = "fact";
  private static final String MAPPINGS_JSON = "mappings.json";
  private static final int MAX_RESULT_WINDOW = 10_000; // Must be the same value as specified in mappings.json.

  private static final Logger LOGGER = Logging.getLogger(FactSearchManager.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final ObjectReader FACT_DOCUMENT_READER = MAPPER.readerFor(FactDocument.class);
  private static final ObjectWriter FACT_DOCUMENT_WRITER = MAPPER.writerFor(FactDocument.class);

  @Dependency
  private final ClientFactory clientFactory;
  private final Function<UUID, EntityHandler> entityHandlerForTypeIdResolver;

  @Inject
  public FactSearchManager(ClientFactory clientFactory, Function<UUID, EntityHandler> entityHandlerForTypeIdResolver) {
    this.clientFactory = clientFactory;
    this.entityHandlerForTypeIdResolver = entityHandlerForTypeIdResolver;
  }

  @Override
  public void startComponent() {
    if (!indexExists()) {
      LOGGER.info("Index '%s' does not exist, create it.", INDEX_NAME);
      createIndex();
    }
  }

  @Override
  public void stopComponent() {
    // NOOP
  }

  /**
   * Retrieve an indexed Fact by its UUID. Returns NULL if Fact cannot be fetched from ElasticSearch.
   *
   * @param id UUID of indexed Fact
   * @return Indexed Fact or NULL if not available
   */
  public FactDocument getFact(UUID id) {
    if (id == null) return null;
    GetResponse response;

    try {
      GetRequest request = new GetRequest(INDEX_NAME, TYPE_NAME, id.toString());
      response = clientFactory.getHighLevelClient().get(request);
    } catch (IOException ex) {
      throw logAndExit(ex, String.format("Could not perform request to fetch Fact with id = %s.", id));
    }

    if (response.isExists()) {
      LOGGER.info("Successfully fetched Fact with id = %s.", id);
      return decodeFactDocument(id, response.getSourceAsBytes());
    } else {
      // Fact isn't indexed in ElasticSearch, log warning and return null.
      LOGGER.warning("Could not fetch Fact with id = %s. Fact not indexed?", id);
      return null;
    }
  }

  /**
   * Index a Fact into ElasticSearch.
   *
   * @param fact Fact to index
   * @return Indexed Fact
   */
  public FactDocument indexFact(FactDocument fact) {
    if (fact == null || fact.getId() == null) return null;
    IndexResponse response;

    try {
      IndexRequest request = new IndexRequest(INDEX_NAME, TYPE_NAME, fact.getId().toString())
              .source(FACT_DOCUMENT_WRITER.writeValueAsBytes(encodeValues(fact)), XContentType.JSON);
      response = clientFactory.getHighLevelClient().index(request);
    } catch (IOException ex) {
      throw logAndExit(ex, String.format("Could not perform request to index Fact with id = %s.", fact.getId()));
    }

    if (response.status() != RestStatus.OK && response.status() != RestStatus.CREATED) {
      LOGGER.warning("Could not index Fact with id = %s.", fact.getId());
    } else if (response.getResult() == DocWriteResponse.Result.CREATED) {
      LOGGER.info("Successfully indexed Fact with id = %s.", fact.getId());
    } else if (response.getResult() == DocWriteResponse.Result.UPDATED) {
      LOGGER.info("Successfully re-indexed existing Fact with id = %s.", fact.getId());
    }

    return fact;
  }

  /**
   * Search for Facts indexed in ElasticSearch by a given search criteria. Only Facts satisfying the search criteria
   * will be returned. Returns an empty list if no Fact satisfies the search criteria.
   * <p>
   * Both 'currentUserID' (identifying the calling user) and 'availableOrganizationID' (identifying the Organizations
   * the calling user has access to) must be set in the search criteria in order to apply access control to Facts. Only
   * Facts accessible to the calling user will be returned.
   *
   * @param criteria Search criteria to match against Facts
   * @return Facts satisfying search Criteria
   */
  public List<FactDocument> searchFacts(FactSearchCriteria criteria) {
    List<FactDocument> result = ListUtils.list();
    if (criteria == null) return result;

    SearchResponse response;
    try {
      response = clientFactory.getHighLevelClient().search(buildSearchRequest(criteria));
    } catch (IOException ex) {
      throw logAndExit(ex, "Could not perform request to search for Facts.");
    }

    if (response.status() != RestStatus.OK) {
      LOGGER.warning("Could not search for Facts (response code %s).", response.status());
      return result;
    }

    for (SearchHit hit : response.getHits()) {
      FactDocument document = decodeFactDocument(UUID.fromString(hit.getId()), toBytes(hit.getSourceRef()));
      if (document != null) {
        result.add(document);
      }
    }

    LOGGER.info("Successfully retrieved %d Facts.", result.size());
    return result;
  }

  private boolean indexExists() {
    Response response;

    try {
      // Need to use low-level client here because the Index API is not yet supported by the high-level client.
      response = clientFactory.getLowLevelClient().performRequest("HEAD", INDEX_NAME);
    } catch (IOException ex) {
      throw logAndExit(ex, "Could not perform request to verify if index exists.");
    }

    // Index exists if request returns with status code 200.
    return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
  }

  private void createIndex() {
    Response response;

    try (InputStream payload = FactSearchManager.class.getClassLoader().getResourceAsStream(MAPPINGS_JSON)) {
      // Need to use low-level client here because the Index API is not yet supported by the high-level client.
      HttpEntity body = new InputStreamEntity(payload, ContentType.APPLICATION_JSON);
      response = clientFactory.getLowLevelClient().performRequest("PUT", INDEX_NAME, Collections.emptyMap(), body);
    } catch (IOException ex) {
      throw logAndExit(ex, "Could not perform request to create index.");
    }

    if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
      String msg = String.format("Could not create index '%s'.", INDEX_NAME);
      LOGGER.error(msg);
      throw new IllegalStateException(msg);
    }

    LOGGER.info("Successfully created index '%s'.", INDEX_NAME);
  }

  private SearchRequest buildSearchRequest(FactSearchCriteria criteria) {
    BoolQueryBuilder rootQuery = boolQuery();
    applySimpleFilterQueries(criteria, rootQuery);
    applyKeywordSearchQuery(criteria, rootQuery);
    applyTimestampSearchQuery(criteria, rootQuery);
    applyAccessControlQuery(criteria, rootQuery);

    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
            .size(criteria.getLimit() > 0 && criteria.getLimit() < MAX_RESULT_WINDOW ? criteria.getLimit() : MAX_RESULT_WINDOW)
            .query(rootQuery);
    return new SearchRequest()
            .indices(INDEX_NAME)
            .types(TYPE_NAME)
            .source(sourceBuilder);
  }

  private void applySimpleFilterQueries(FactSearchCriteria criteria, BoolQueryBuilder rootQuery) {
    if (!CollectionUtils.isEmpty(criteria.getFactID())) {
      rootQuery.filter(termsQuery("_id", criteria.getFactID()));
    }

    if (!CollectionUtils.isEmpty(criteria.getFactTypeID())) {
      rootQuery.filter(termsQuery("typeID", criteria.getFactTypeID()));
    }

    if (!CollectionUtils.isEmpty(criteria.getFactTypeName())) {
      rootQuery.filter(termsQuery("typeName", criteria.getFactTypeName()));
    }

    if (!CollectionUtils.isEmpty(criteria.getFactValue())) {
      rootQuery.filter(termsQuery("value", criteria.getFactValue()));
    }

    if (!CollectionUtils.isEmpty(criteria.getInReferenceTo())) {
      rootQuery.filter(termsQuery("inReferenceTo", criteria.getInReferenceTo()));
    }

    if (!CollectionUtils.isEmpty(criteria.getOrganizationID())) {
      rootQuery.filter(termsQuery("organizationID", criteria.getOrganizationID()));
    }

    if (!CollectionUtils.isEmpty(criteria.getOrganizationName())) {
      rootQuery.filter(termsQuery("organizationName", criteria.getOrganizationName()));
    }

    if (!CollectionUtils.isEmpty(criteria.getSourceID())) {
      rootQuery.filter(termsQuery("sourceID", criteria.getSourceID()));
    }

    if (!CollectionUtils.isEmpty(criteria.getSourceName())) {
      rootQuery.filter(termsQuery("sourceName", criteria.getSourceName()));
    }

    if (!CollectionUtils.isEmpty(criteria.getObjectID())) {
      rootQuery.filter(nestedQuery("objects", termsQuery("objects.id", criteria.getObjectID()), ScoreMode.None));
    }

    if (!CollectionUtils.isEmpty(criteria.getObjectTypeID())) {
      rootQuery.filter(nestedQuery("objects", termsQuery("objects.typeID", criteria.getObjectTypeID()), ScoreMode.None));
    }

    if (!CollectionUtils.isEmpty(criteria.getObjectTypeName())) {
      rootQuery.filter(nestedQuery("objects", termsQuery("objects.typeName", criteria.getObjectTypeName()), ScoreMode.None));
    }

    if (!CollectionUtils.isEmpty(criteria.getObjectValue())) {
      rootQuery.filter(nestedQuery("objects", termsQuery("objects.value", criteria.getObjectValue()), ScoreMode.None));
    }

    if (criteria.getRetracted() != null) {
      rootQuery.filter(termQuery("retracted", (boolean) criteria.getRetracted()));
    }
  }

  private void applyKeywordSearchQuery(FactSearchCriteria criteria, BoolQueryBuilder rootQuery) {
    if (StringUtils.isBlank(criteria.getKeywords())) return;

    // Set default values if not provided by criteria.
    Set<FactSearchCriteria.KeywordFieldStrategy> fieldStrategies = CollectionUtils.isEmpty(criteria.getKeywordFieldStrategy()) ?
            SetUtils.set(FactSearchCriteria.KeywordFieldStrategy.all) : criteria.getKeywordFieldStrategy();
    FactSearchCriteria.MatchStrategy matchStrategy = ObjectUtils.ifNull(criteria.getKeywordMatchStrategy(), FactSearchCriteria.MatchStrategy.any);

    applyFieldStrategy(rootQuery, field -> createFieldQuery(field, criteria.getKeywords()), fieldStrategies, matchStrategy);
  }

  private void applyTimestampSearchQuery(FactSearchCriteria criteria, BoolQueryBuilder rootQuery) {
    if (criteria.getStartTimestamp() == null && criteria.getEndTimestamp() == null) return;

    // Set default values if not provided by criteria.
    Set<FactSearchCriteria.TimeFieldStrategy> fieldStrategies = CollectionUtils.isEmpty(criteria.getTimeFieldStrategy()) ?
            SetUtils.set(FactSearchCriteria.TimeFieldStrategy.all) : criteria.getTimeFieldStrategy();
    FactSearchCriteria.MatchStrategy matchStrategy = ObjectUtils.ifNull(criteria.getTimeMatchStrategy(), FactSearchCriteria.MatchStrategy.any);

    applyFieldStrategy(rootQuery, field -> createFieldQuery(field, criteria.getStartTimestamp(), criteria.getEndTimestamp()),
            fieldStrategies, matchStrategy);
  }

  private void applyFieldStrategy(BoolQueryBuilder rootQuery, Function<String, QueryBuilder> fieldQueryResolver,
                                  Set<? extends FactSearchCriteria.FieldStrategy> fieldStrategies,
                                  FactSearchCriteria.MatchStrategy matchStrategy) {
    // Determine all fields to query.
    Set<String> fieldsToQuery = fieldStrategies.stream()
            .flatMap(strategy -> strategy.getFields().stream())
            .collect(Collectors.toSet());

    BoolQueryBuilder strategyQuery = boolQuery();
    for (String field : fieldsToQuery) {
      if (matchStrategy == FactSearchCriteria.MatchStrategy.all) {
        // Field query must match all fields.
        strategyQuery.filter(fieldQueryResolver.apply(field));
      } else {
        // Field query should match at least one field.
        strategyQuery.should(fieldQueryResolver.apply(field));
      }
    }

    rootQuery.filter(strategyQuery);
  }

  private QueryBuilder createFieldQuery(String field, String keywords) {
    SimpleQueryStringBuilder query = simpleQueryStringQuery(keywords)
            .field(field)
            // Values are indexed differently. Avoid errors when executing an IP search against a text field, for example.
            .lenient(true);
    // If field starts with the prefix 'objects.' it's part of the nested objects, thus, it must be wrapped inside a nested query.
    return field.startsWith("objects.") ? nestedQuery("objects", query, ScoreMode.Avg) : query;
  }

  private QueryBuilder createFieldQuery(String field, Long startTimestamp, Long endTimestamp) {
    // Negative timestamps are omitted by providing NULL to from() and to().
    return rangeQuery(field)
            .from(startTimestamp != null && startTimestamp > 0 ? startTimestamp : null)
            .to(endTimestamp != null && endTimestamp > 0 ? endTimestamp : null);
  }

  private void applyAccessControlQuery(FactSearchCriteria criteria, BoolQueryBuilder rootQuery) {
    // Query to verify that user has access to Fact ...
    BoolQueryBuilder accessQuery = boolQuery()
            // ... if Fact is public.
            .should(termQuery("accessMode", FactDocument.AccessMode.Public))
            // ... if AccessMode == Explicit user must be in ACL.
            .should(boolQuery()
                    .filter(termQuery("accessMode", FactDocument.AccessMode.Explicit))
                    .filter(termQuery("acl", criteria.getCurrentUserID()))
            )
            // ... if AccessMode == RoleBased user must be in ACL or have access to the owning Organization.
            .should(boolQuery()
                    .filter(termQuery("accessMode", FactDocument.AccessMode.RoleBased))
                    .filter(boolQuery()
                            .should(termQuery("acl", criteria.getCurrentUserID()))
                            .should(termsQuery("organizationID", criteria.getAvailableOrganizationID()))
                    )
            );

    // Always apply access control query.
    rootQuery.filter(accessQuery);
  }

  private FactDocument decodeFactDocument(UUID factID, byte[] source) {
    try {
      FactDocument fact = decodeValues(FACT_DOCUMENT_READER.readValue(source));
      // Need to set ID manually because it's not indexed as an own field.
      return fact.setId(factID);
    } catch (IOException ex) {
      LOGGER.warning(ex, "Could not deserialize Fact with id = %s. Source document not stored?", factID);
      return null;
    }
  }

  private FactDocument encodeValues(FactDocument fact) {
    // Clone document first in order to not change supplied instance.
    FactDocument clone = fact.clone();
    // Encode Fact value using EntityHandler to store value in encoded format.
    clone.setValue(entityHandlerForTypeIdResolver.apply(fact.getTypeID()).encode(fact.getValue()));
    // Also encode all Object values.
    SetUtils.set(clone.getObjects()).forEach(o -> o.setValue(entityHandlerForTypeIdResolver.apply(o.getTypeID()).encode(o.getValue())));

    return clone;
  }

  private FactDocument decodeValues(FactDocument fact) {
    // Decode Fact value using EntityHandler because it's stored encoded.
    fact.setValue(entityHandlerForTypeIdResolver.apply(fact.getTypeID()).decode(fact.getValue()));
    // Also decode all Object values.
    SetUtils.set(fact.getObjects()).forEach(o -> o.setValue(entityHandlerForTypeIdResolver.apply(o.getTypeID()).decode(o.getValue())));

    return fact;
  }

  private RuntimeException logAndExit(Exception ex, String msg) {
    LOGGER.error(ex, msg);
    return new RuntimeException(msg, ex);
  }

}
