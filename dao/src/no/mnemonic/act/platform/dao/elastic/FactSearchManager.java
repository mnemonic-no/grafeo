package no.mnemonic.act.platform.dao.elastic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.io.CharStreams;
import no.mnemonic.act.platform.dao.api.FactExistenceSearchCriteria;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.ObjectStatisticsCriteria;
import no.mnemonic.act.platform.dao.api.ObjectStatisticsResult;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.dao.elastic.document.ScrollingSearchResult;
import no.mnemonic.act.platform.dao.elastic.document.SearchResult;
import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.HasAggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.elasticsearch.common.bytes.BytesReference.toBytes;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.aggregations.AggregationBuilders.*;

/**
 * Class for indexing Facts into ElasticSearch as well as for retrieving and searching indexed Facts.
 */
@Singleton
public class FactSearchManager implements LifecycleAspect {

  private static final String INDEX_NAME = "act";
  private static final String TYPE_NAME = "_doc";
  private static final String MAPPINGS_JSON = "mappings.json";
  private static final int MAX_RESULT_WINDOW = 10_000; // Must be the same value as specified in mappings.json.

  private static final String FILTER_FACTS_AGGREGATION_NAME = "FilterFactsAggregation";
  private static final String NESTED_OBJECTS_AGGREGATION_NAME = "NestedObjectsAggregation";
  private static final String FILTER_OBJECTS_AGGREGATION_NAME = "FilterObjectsAggregation";
  private static final String OBJECTS_COUNT_AGGREGATION_NAME = "ObjectsCountAggregation";
  private static final String UNIQUE_OBJECTS_AGGREGATION_NAME = "UniqueObjectsAggregation";
  private static final String UNIQUE_OBJECTS_SOURCE_AGGREGATION_NAME = "UniqueObjectsSourceAggregation";
  private static final String REVERSED_FACTS_AGGREGATION_NAME = "ReversedFactsAggregation";
  private static final String UNIQUE_FACT_TYPES_AGGREGATION_NAME = "UniqueFactTypesAggregation";
  private static final String MAX_LAST_ADDED_TIMESTAMP_AGGREGATION_NAME = "MaxLastAddedTimestampAggregation";
  private static final String MAX_LAST_SEEN_TIMESTAMP_AGGREGATION_NAME = "MaxLastSeenTimestampAggregation";

  private static final float CONFIDENCE_EQUALITY_INTERVAL = 0.01f;

  private static final Logger LOGGER = Logging.getLogger(FactSearchManager.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final ObjectReader FACT_DOCUMENT_READER = MAPPER.readerFor(FactDocument.class);
  private static final ObjectReader OBJECT_DOCUMENT_READER = MAPPER.readerFor(ObjectDocument.class);
  private static final ObjectWriter FACT_DOCUMENT_WRITER = MAPPER.writerFor(FactDocument.class);

  @Dependency
  private final ClientFactory clientFactory;

  private String searchScrollExpiration = "1m";
  private int searchScrollSize = 1000;
  private boolean isTestEnvironment = false;

  @Inject
  public FactSearchManager(ClientFactory clientFactory) {
    this.clientFactory = clientFactory;
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
      response = clientFactory.getClient().get(request, RequestOptions.DEFAULT);
    } catch (ElasticsearchException | IOException ex) {
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
              .setRefreshPolicy(isTestEnvironment ? WriteRequest.RefreshPolicy.IMMEDIATE : WriteRequest.RefreshPolicy.NONE)
              .source(FACT_DOCUMENT_WRITER.writeValueAsBytes(fact), XContentType.JSON);
      response = clientFactory.getClient().index(request, RequestOptions.DEFAULT);
    } catch (ElasticsearchException | IOException ex) {
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
   * Retrieve all Facts which are considered logically the same when matched against a given search criteria, i.e. the
   * following condition holds: an indexed Fact matches the search criteria and will be included in the returned result
   * if and only if the value, FactType, organization, origin, confidence, access mode and all bound Objects (including
   * direction) match exactly. In the case of a meta Fact the inReferenceTo must match instead of bound Objects (Objects
   * should be omitted). If no Fact satisfies this condition an empty result container is returned.
   * <p>
   * This method can be used to determine if a Fact already logically exists in the system, e.g. before a new Fact is
   * created. No access control will be performed. This must be done by the caller.
   *
   * @param criteria Criteria to retrieve existing Facts
   * @return All Facts satisfying search criteria wrapped inside a result container
   */
  public SearchResult<FactDocument> retrieveExistingFacts(FactExistenceSearchCriteria criteria) {
    if (criteria == null) return SearchResult.<FactDocument>builder().build();

    SearchResponse response;
    try {
      response = clientFactory.getClient().search(buildFactExistenceSearchRequest(criteria), RequestOptions.DEFAULT);
    } catch (ElasticsearchException | IOException ex) {
      throw logAndExit(ex, "Could not perform request to search for existing Facts.");
    }

    if (response.status() != RestStatus.OK) {
      LOGGER.warning("Could not search for existing Facts (response code %s).", response.status());
      return SearchResult.<FactDocument>builder().build();
    }

    List<FactDocument> result = retrieveFactDocuments(response);

    LOGGER.info("Successfully retrieved %d existing Facts.", result.size());
    return SearchResult.<FactDocument>builder()
            .setCount((int) response.getHits().getTotalHits())
            .setValues(result)
            .build();
  }

  /**
   * Search for Facts indexed in ElasticSearch by a given search criteria. Only Facts satisfying the search criteria
   * will be returned. Returns a result container which will stream out the results from ElasticSearch. It will not
   * limit the number of returned results. This must be done by the caller. Returns an empty result container if no
   * Fact satisfies the search criteria.
   * <p>
   * Both 'currentUserID' (identifying the calling user) and 'availableOrganizationID' (identifying the Organizations
   * the calling user has access to) must be set in the search criteria in order to apply access control to Facts. Only
   * Facts accessible to the calling user will be returned.
   *
   * @param criteria Search criteria to match against Facts
   * @return Facts satisfying search criteria wrapped inside a result container
   */
  public ScrollingSearchResult<FactDocument> searchFacts(FactSearchCriteria criteria) {
    if (criteria == null) return ScrollingSearchResult.<FactDocument>builder().build();

    SearchResponse response;
    try {
      response = clientFactory.getClient().search(buildFactsSearchRequest(criteria), RequestOptions.DEFAULT);
    } catch (ElasticsearchException | IOException ex) {
      throw logAndExit(ex, "Could not perform request to search for Facts.");
    }

    if (response.status() != RestStatus.OK) {
      LOGGER.warning("Could not search for Facts (response code %s).", response.status());
      return ScrollingSearchResult.<FactDocument>builder().build();
    }

    LOGGER.info("Successfully initiated streaming of search results. Start fetching data.");
    return ScrollingSearchResult.<FactDocument>builder()
            .setInitialBatch(createFactsBatch(response))
            .setFetchNextBatch(this::fetchNextFactsBatch)
            .setCount((int) response.getHits().getTotalHits())
            .build();
  }

  /**
   * Search for Objects indexed in ElasticSearch by a given search criteria. Only Objects satisfying the search criteria
   * will be returned. Returns an empty result container if no Object satisfies the search criteria.
   * <p>
   * First, the result will be reduced to only the Facts satisfying the search criteria. Then, for all matching Facts
   * the bound Objects will be reduced to the unique Objects satisfying the search criteria.
   * <p>
   * Both 'currentUserID' (identifying the calling user) and 'availableOrganizationID' (identifying the Organizations
   * the calling user has access to) must be set in the search criteria in order to apply access control to Facts. Only
   * Objects bound to Facts accessible to the calling user will be returned.
   *
   * @param criteria Search criteria to match against Facts and their bound Objects
   * @return Objects satisfying search criteria wrapped inside a result container
   */
  public SearchResult<ObjectDocument> searchObjects(FactSearchCriteria criteria) {
    if (criteria == null) return SearchResult.<ObjectDocument>builder().build();

    SearchResponse response;
    try {
      response = clientFactory.getClient().search(buildObjectsSearchRequest(criteria), RequestOptions.DEFAULT);
    } catch (ElasticsearchException | IOException ex) {
      throw logAndExit(ex, "Could not perform request to search for Objects.");
    }

    if (response.status() != RestStatus.OK) {
      LOGGER.warning("Could not search for Objects (response code %s).", response.status());
      return SearchResult.<ObjectDocument>builder().setLimit(criteria.getLimit()).build();
    }

    int count = retrieveSearchObjectsResultCount(response);
    List<ObjectDocument> result = retrieveSearchObjectsResultValues(response);

    LOGGER.info("Successfully retrieved %d Objects from a total of %d matching Objects.", result.size(), count);
    return SearchResult.<ObjectDocument>builder()
            .setLimit(criteria.getLimit())
            .setCount(count)
            .setValues(result)
            .build();
  }

  /**
   * Calculate statistics about the Facts bound to Objects. For each Object specified in the statistics criteria it is
   * calculated how many Facts of each FactType are bound to the Object and when a Fact of that FactType was last added
   * and last seen.
   * <p>
   * Both 'currentUserID' (identifying the calling user) and 'availableOrganizationID' (identifying the Organizations
   * the calling user has access to) must be set in the statistics criteria in order to apply access control to Facts.
   * Only statistics for Objects bound to Facts accessible to the calling user will be returned, and only accessible
   * Facts will be included in the returned statistics.
   *
   * @param criteria Criteria to specify for which Objects statistics should be calculated
   * @return Result container with the calculated statistics for each Object
   */
  public ObjectStatisticsResult calculateObjectStatistics(ObjectStatisticsCriteria criteria) {
    if (criteria == null) return ObjectStatisticsResult.builder().build();

    SearchResponse response;
    try {
      response = clientFactory.getClient().search(buildObjectStatisticsSearchRequest(criteria), RequestOptions.DEFAULT);
    } catch (ElasticsearchException | IOException ex) {
      throw logAndExit(ex, "Could not perform request to calculate Object statistics.");
    }

    if (response.status() != RestStatus.OK) {
      LOGGER.warning("Could not calculate Object statistics (response code %s).", response.status());
      return ObjectStatisticsResult.builder().build();
    }

    ObjectStatisticsResult result = retrieveObjectStatisticsResult(response);

    LOGGER.info("Successfully retrieved statistics for %d Objects.", result.getStatisticsCount());
    return result;
  }

  /**
   * Specify if this class is executed during unit tests (defaults to false). This setting will make indexed documents
   * available for search immediately.
   *
   * @param testEnvironment Whether this class is executed during unit tests
   * @return Class instance, i.e. 'this'
   */
  public FactSearchManager setTestEnvironment(boolean testEnvironment) {
    this.isTestEnvironment = testEnvironment;
    return this;
  }

  /**
   * Specify how long the search context of a scrolling search will be kept open in ElasticSearch. Defaults to 1 minute.
   * <p>
   * Accepts an ElasticSearch time unit: https://www.elastic.co/guide/en/elasticsearch/reference/current/common-options.html#time-units
   *
   * @param searchScrollExpiration Expiration time of search context
   * @return Class instance, i.e. 'this'
   */
  public FactSearchManager setSearchScrollExpiration(String searchScrollExpiration) {
    this.searchScrollExpiration = searchScrollExpiration;
    return this;
  }

  /**
   * Specify the batch size when fetching data from ElasticSearch using a scrolling search. Defaults to 1000.
   *
   * @param searchScrollSize Batch size
   * @return Class instance, i.e. 'this'
   */
  public FactSearchManager setSearchScrollSize(int searchScrollSize) {
    this.searchScrollSize = searchScrollSize;
    return this;
  }

  private boolean indexExists() {
    try {
      GetIndexRequest request = new GetIndexRequest(INDEX_NAME);
      return clientFactory.getClient().indices().exists(request, RequestOptions.DEFAULT);
    } catch (ElasticsearchException | IOException ex) {
      throw logAndExit(ex, "Could not perform request to verify if index exists.");
    }
  }

  private void createIndex() {
    CreateIndexResponse response;

    try (InputStream payload = FactSearchManager.class.getClassLoader().getResourceAsStream(MAPPINGS_JSON);
         InputStreamReader reader = new InputStreamReader(payload)) {
      CreateIndexRequest request = new CreateIndexRequest(INDEX_NAME)
              .source(CharStreams.toString(reader), XContentType.JSON);
      response = clientFactory.getClient().indices().create(request, RequestOptions.DEFAULT);
    } catch (ElasticsearchException | IOException ex) {
      throw logAndExit(ex, "Could not perform request to create index.");
    }

    if (!response.isAcknowledged()) {
      String msg = String.format("Could not create index '%s'.", INDEX_NAME);
      LOGGER.error(msg);
      throw new IllegalStateException(msg);
    }

    LOGGER.info("Successfully created index '%s'.", INDEX_NAME);
  }

  private ScrollingSearchResult.ScrollingBatch<FactDocument> fetchNextFactsBatch(String scrollId) {
    SearchResponse response;
    try {
      SearchScrollRequest request = new SearchScrollRequest()
              .scrollId(scrollId)
              .scroll(searchScrollExpiration);
      response = clientFactory.getClient().scroll(request, RequestOptions.DEFAULT);
    } catch (ElasticsearchException | IOException ex) {
      LOGGER.warning(ex, "Could not perform request to retrieve next batch of search results. Stop scrolling.");
      return ScrollingSearchResult.emptyBatch();
    }

    if (response.status() != RestStatus.OK) {
      LOGGER.warning("Could not retrieve next batch of search results (response code %s). Stop scrolling.", response.status());
      return ScrollingSearchResult.emptyBatch();
    }

    return createFactsBatch(response);
  }

  private ScrollingSearchResult.ScrollingBatch<FactDocument> createFactsBatch(SearchResponse response) {
    List<FactDocument> values = retrieveFactDocuments(response);
    LOGGER.debug("Successfully retrieved next batch of search results (batch: %d, total: %d).", values.size(), response.getHits().getTotalHits());

    boolean finished = values.size() < searchScrollSize;
    if (finished) {
      LOGGER.info("Successfully retrieved all search results. No more data available.");
      // Close search context when all results have been fetched. If the client doesn't consume all results the context
      // will be kept open until ElasticSearch cleans it up automatically after the expiration time elapsed.
      closeSearchContext(response.getScrollId());
    }

    return new ScrollingSearchResult.ScrollingBatch<>(response.getScrollId(), values.iterator(), finished);
  }

  private void closeSearchContext(String scrollId) {
    ClearScrollRequest request = new ClearScrollRequest();
    request.addScrollId(scrollId);

    // Perform this clean-up asynchronously because the client doesn't require the result.
    clientFactory.getClient().clearScrollAsync(request, RequestOptions.DEFAULT, new ActionListener<ClearScrollResponse>() {
      @Override
      public void onResponse(ClearScrollResponse response) {
        if (!response.isSucceeded()) {
          LOGGER.warning("Could not close search context (response code %s).", response.status());
        } else {
          LOGGER.debug("Successfully closed search context.");
        }
      }

      @Override
      public void onFailure(Exception ex) {
        LOGGER.warning(ex, "Could not close search context.");
      }
    });
  }

  private SearchRequest buildFactExistenceSearchRequest(FactExistenceSearchCriteria criteria) {
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
            .size(MAX_RESULT_WINDOW) // Always return all matching documents, but usually this should be zero or one.
            .query(buildFactExistenceQuery(criteria));
    return new SearchRequest()
            .indices(INDEX_NAME)
            .types(TYPE_NAME)
            .source(sourceBuilder);
  }

  private SearchRequest buildFactsSearchRequest(FactSearchCriteria criteria) {
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
            .size(searchScrollSize)
            .query(buildFactsQuery(criteria));
    return new SearchRequest()
            .indices(INDEX_NAME)
            .types(TYPE_NAME)
            .scroll(searchScrollExpiration)
            .source(sourceBuilder);
  }

  private SearchRequest buildObjectsSearchRequest(FactSearchCriteria criteria) {
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
            .size(0) // Not interested in the search hits as the search result is part of the returned aggregations.
            .aggregation(buildObjectsAggregation(criteria));
    return new SearchRequest()
            .indices(INDEX_NAME)
            .types(TYPE_NAME)
            .source(sourceBuilder);
  }

  private SearchRequest buildObjectStatisticsSearchRequest(ObjectStatisticsCriteria criteria) {
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
            .size(0) // Not interested in the search hits as the search result is part of the returned aggregations.
            .aggregation(buildObjectStatisticsAggregation(criteria));
    return new SearchRequest()
            .indices(INDEX_NAME)
            .types(TYPE_NAME)
            .source(sourceBuilder);
  }

  private QueryBuilder buildFactExistenceQuery(FactExistenceSearchCriteria criteria) {
    // Define all filters on direct Fact fields. Every field from the criteria must match.
    BoolQueryBuilder rootQuery = boolQuery()
            .filter(termQuery("typeID", toString(criteria.getFactTypeID())))
            .filter(termQuery("sourceID", toString(criteria.getOriginID())))
            .filter(termQuery("organizationID", toString(criteria.getOrganizationID())))
            .filter(termQuery("accessMode", toString(criteria.getAccessMode())))
            // Consider 'confidence' to be equal inside a given interval.
            .filter(rangeQuery("confidence")
                    .gt(criteria.getConfidence() - CONFIDENCE_EQUALITY_INTERVAL / 2)
                    .lt(criteria.getConfidence() + CONFIDENCE_EQUALITY_INTERVAL / 2));

    if (criteria.getFactValue() != null) {
      // Fact values must match exactly if given.
      rootQuery.filter(termQuery("value", criteria.getFactValue()));
    } else {
      // If 'value' isn't given make sure that it's also not set on any existing Fact.
      rootQuery.mustNot(existsQuery("value"));
    }

    // This clause is required when searching for existing meta Facts.
    if (criteria.getInReferenceTo() != null) {
      rootQuery.filter(termQuery("inReferenceTo", toString(criteria.getInReferenceTo())));
    }

    // These clauses are required when searching for regular Facts.
    if (!CollectionUtils.isEmpty(criteria.getObjects())) {
      // The number of bound Objects must match (stored as a de-normalized field).
      rootQuery.filter(termQuery("objectCount", criteria.getObjects().size()));

      // Define filters on nested Objects. Also all Objects must match.
      for (FactExistenceSearchCriteria.ObjectExistence object : criteria.getObjects()) {
        BoolQueryBuilder objectsQuery = boolQuery()
                .filter(termQuery("objects.id", toString(object.getObjectID())))
                .filter(termQuery("objects.direction", toString(object.getDirection())));
        rootQuery.filter(nestedQuery("objects", objectsQuery, ScoreMode.None));
      }
    }

    return rootQuery;
  }

  private QueryBuilder buildFactsQuery(FactSearchCriteria criteria) {
    BoolQueryBuilder rootQuery = boolQuery();
    applySimpleFilterQueries(criteria, rootQuery);
    applyKeywordSearchQuery(criteria, rootQuery);
    applyTimestampSearchQuery(criteria, rootQuery);
    applyNumberSearchQuery(criteria, rootQuery);

    // Always apply access control query.
    return rootQuery.filter(createAccessControlQuery(criteria.getCurrentUserID(), criteria.getAvailableOrganizationID()));
  }

  private void applySimpleFilterQueries(FactSearchCriteria criteria, BoolQueryBuilder rootQuery) {
    if (!CollectionUtils.isEmpty(criteria.getFactID())) {
      rootQuery.filter(termsQuery("_id", toString(criteria.getFactID())));
    }

    if (!CollectionUtils.isEmpty(criteria.getFactTypeID())) {
      rootQuery.filter(termsQuery("typeID", toString(criteria.getFactTypeID())));
    }

    if (!CollectionUtils.isEmpty(criteria.getFactValue())) {
      rootQuery.filter(termsQuery("value", criteria.getFactValue()));
    }

    if (!CollectionUtils.isEmpty(criteria.getInReferenceTo())) {
      rootQuery.filter(termsQuery("inReferenceTo", toString(criteria.getInReferenceTo())));
    }

    if (!CollectionUtils.isEmpty(criteria.getOrganizationID())) {
      rootQuery.filter(termsQuery("organizationID", toString(criteria.getOrganizationID())));
    }

    if (!CollectionUtils.isEmpty(criteria.getOriginID())) {
      rootQuery.filter(termsQuery("sourceID", toString(criteria.getOriginID())));
    }

    if (!CollectionUtils.isEmpty(criteria.getObjectID())) {
      rootQuery.filter(nestedQuery("objects", termsQuery("objects.id", toString(criteria.getObjectID())), ScoreMode.None));
    }

    if (!CollectionUtils.isEmpty(criteria.getObjectTypeID())) {
      rootQuery.filter(nestedQuery("objects", termsQuery("objects.typeID", toString(criteria.getObjectTypeID())), ScoreMode.None));
    }

    if (!CollectionUtils.isEmpty(criteria.getObjectValue())) {
      rootQuery.filter(nestedQuery("objects", termsQuery("objects.value", criteria.getObjectValue()), ScoreMode.None));
    }
  }

  private void applyKeywordSearchQuery(FactSearchCriteria criteria, BoolQueryBuilder rootQuery) {
    if (StringUtils.isBlank(criteria.getKeywords())) return;
    applyFieldStrategy(rootQuery, field -> createFieldQuery(field, criteria.getKeywords()),
            criteria.getKeywordFieldStrategy(), criteria.getKeywordMatchStrategy());
  }

  private void applyTimestampSearchQuery(FactSearchCriteria criteria, BoolQueryBuilder rootQuery) {
    if (criteria.getStartTimestamp() == null && criteria.getEndTimestamp() == null) return;
    applyFieldStrategy(rootQuery, field -> createFieldQuery(field, criteria.getStartTimestamp(), criteria.getEndTimestamp()),
            criteria.getTimeFieldStrategy(), criteria.getTimeMatchStrategy());
  }

  private void applyNumberSearchQuery(FactSearchCriteria criteria, BoolQueryBuilder rootQuery) {
    if (criteria.getMinNumber() == null && criteria.getMaxNumber() == null) return;
    applyFieldStrategy(rootQuery, field -> rangeQuery(field).gte(criteria.getMinNumber()).lte(criteria.getMaxNumber()),
            criteria.getNumberFieldStrategy(), criteria.getNumberMatchStrategy());
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

  private QueryBuilder createAccessControlQuery(UUID currentUserID, Set<UUID> availableOrganizationID) {
    // Query to verify that user has access to Fact ...
    return boolQuery()
            // ... if Fact is public.
            .should(termQuery("accessMode", toString(FactDocument.AccessMode.Public)))
            // ... if AccessMode == Explicit user must be in ACL.
            .should(boolQuery()
                    .filter(termQuery("accessMode", toString(FactDocument.AccessMode.Explicit)))
                    .filter(termQuery("acl", toString(currentUserID)))
            )
            // ... if AccessMode == RoleBased user must be in ACL or have access to the owning Organization.
            .should(boolQuery()
                    .filter(termQuery("accessMode", toString(FactDocument.AccessMode.RoleBased)))
                    .filter(boolQuery()
                            .should(termQuery("acl", toString(currentUserID)))
                            .should(termsQuery("organizationID", toString(availableOrganizationID)))
                    )
            );
  }

  private AggregationBuilder buildObjectsAggregation(FactSearchCriteria criteria) {
    // 1. Reduce to Facts matching the search criteria.
    return filter(FILTER_FACTS_AGGREGATION_NAME, buildFactsQuery(criteria))
            // 2. Map to nested Object documents.
            .subAggregation(nested(NESTED_OBJECTS_AGGREGATION_NAME, "objects")
                    // 3. Reduce to Objects matching the search criteria.
                    .subAggregation(filter(FILTER_OBJECTS_AGGREGATION_NAME, buildObjectsQuery(criteria))
                            // 4. Calculate the number of unique Objects by id. This will give the 'count' value.
                            // If 'count' is smaller than MAX_RESULT_WINDOW a correct value is expected, thus,
                            // the precision threshold is set to MAX_RESULT_WINDOW.
                            .subAggregation(cardinality(OBJECTS_COUNT_AGGREGATION_NAME)
                                    .field("objects.id")
                                    .precisionThreshold(MAX_RESULT_WINDOW)
                            )
                            // 5. Reduce to buckets of unique Objects by id, restricted to the search criteria's limit.
                            // This will give the actual search results.
                            .subAggregation(terms(UNIQUE_OBJECTS_AGGREGATION_NAME)
                                    .field("objects.id")
                                    .size(calculateMaximumSize(criteria))
                                    // 6. Map to the unique Object's source. Set size to 1, because all Objects in one
                                    // bucket are the same (ignoring 'direction' which isn't relevant for Object search).
                                    .subAggregation(topHits(UNIQUE_OBJECTS_SOURCE_AGGREGATION_NAME)
                                            .size(1)
                                    )
                            )
                    )
            );
  }

  private QueryBuilder buildObjectsQuery(FactSearchCriteria criteria) {
    BoolQueryBuilder rootQuery = boolQuery();

    // Apply all simple filter queries on Objects. It's not necessary to wrap them inside a nested query because the
    // query is executed inside a nested aggregation which has direct access to the nested documents.
    if (!CollectionUtils.isEmpty(criteria.getObjectID())) {
      rootQuery.filter(termsQuery("objects.id", toString(criteria.getObjectID())));
    }

    if (!CollectionUtils.isEmpty(criteria.getObjectTypeID())) {
      rootQuery.filter(termsQuery("objects.typeID", toString(criteria.getObjectTypeID())));
    }

    if (!CollectionUtils.isEmpty(criteria.getObjectValue())) {
      rootQuery.filter(termsQuery("objects.value", criteria.getObjectValue()));
    }

    // Apply keyword search on Object values if necessary.
    if (!StringUtils.isBlank(criteria.getKeywords()) &&
            (criteria.getKeywordFieldStrategy().contains(FactSearchCriteria.KeywordFieldStrategy.objectValue) ||
                    criteria.getKeywordFieldStrategy().contains(FactSearchCriteria.KeywordFieldStrategy.all))) {
      // Values are indexed differently. Avoid errors by setting 'lenient' to true.
      applyFieldStrategy(rootQuery, field -> simpleQueryStringQuery(criteria.getKeywords()).field(field).lenient(true),
              SetUtils.set(FactSearchCriteria.KeywordFieldStrategy.objectValue), criteria.getKeywordMatchStrategy());
    }

    return rootQuery;
  }

  private int calculateMaximumSize(FactSearchCriteria criteria) {
    return criteria.getLimit() > 0 && criteria.getLimit() < MAX_RESULT_WINDOW ? criteria.getLimit() : MAX_RESULT_WINDOW;
  }

  private AggregationBuilder buildObjectStatisticsAggregation(ObjectStatisticsCriteria criteria) {
    QueryBuilder accessControlQuery = createAccessControlQuery(criteria.getCurrentUserID(), criteria.getAvailableOrganizationID());
    QueryBuilder objectsQuery = termsQuery("objects.id", toString(criteria.getObjectID()));

    // 1. Reduce to only the Facts the user has access to. Non-accessible Facts won't be available in sub aggregations!
    return filter(FILTER_FACTS_AGGREGATION_NAME, accessControlQuery)
            // 2. Map to nested Object documents.
            .subAggregation(nested(NESTED_OBJECTS_AGGREGATION_NAME, "objects")
                    // 3. Reduce to only the Objects for which statistics should be calculated.
                    .subAggregation(filter(FILTER_OBJECTS_AGGREGATION_NAME, objectsQuery)
                            // 4. Reduce to buckets of unique Objects by id. There shouldn't be more buckets than the
                            // number of Objects for which statistics will be calculated ('size' parameter).
                            .subAggregation(terms(UNIQUE_OBJECTS_AGGREGATION_NAME)
                                    .field("objects.id")
                                    .size(criteria.getObjectID().size())
                                    // 5. Reverse nested aggregation to have access to parent Facts.
                                    .subAggregation(reverseNested(REVERSED_FACTS_AGGREGATION_NAME)
                                            // 6. Create one bucket for each FactType. Set 'size' to MAX_RESULT_WINDOW
                                            // in order to get the statistics for all FactTypes. The returned 'doc_count'
                                            // will give the number of Facts per FactType.
                                            .subAggregation(terms(UNIQUE_FACT_TYPES_AGGREGATION_NAME)
                                                    .field("typeID")
                                                    .size(MAX_RESULT_WINDOW)
                                                    // 7. Calculate the maximum lastAddedTimestamp per FactType.
                                                    .subAggregation(max(MAX_LAST_ADDED_TIMESTAMP_AGGREGATION_NAME)
                                                            .field("timestamp")
                                                    )
                                                    // 8. Calculate the maximum lastSeenTimestamp per FactType.
                                                    .subAggregation(max(MAX_LAST_SEEN_TIMESTAMP_AGGREGATION_NAME)
                                                            .field("lastSeenTimestamp")
                                                    )
                                            )
                                    )
                            )
                    )
            );
  }

  private List<FactDocument> retrieveFactDocuments(SearchResponse response) {
    List<FactDocument> result = ListUtils.list();
    for (SearchHit hit : response.getHits()) {
      FactDocument document = decodeFactDocument(UUID.fromString(hit.getId()), toBytes(hit.getSourceRef()));
      if (document != null) {
        result.add(document);
      }
    }
    return result;
  }

  private int retrieveSearchObjectsResultCount(SearchResponse response) {
    Aggregation objectsCountAggregation = resolveChildAggregation(response.getAggregations(), OBJECTS_COUNT_AGGREGATION_NAME);
    if (!(objectsCountAggregation instanceof Cardinality)) {
      LOGGER.warning("Could not retrieve result count when searching for Objects.");
      return -1;
    }

    // Retrieve Object count from the cardinality aggregation.
    return (int) Cardinality.class.cast(objectsCountAggregation).getValue();
  }

  private List<ObjectDocument> retrieveSearchObjectsResultValues(SearchResponse response) {
    List<ObjectDocument> result = ListUtils.list();

    Aggregation uniqueObjectsAggregation = resolveChildAggregation(response.getAggregations(), UNIQUE_OBJECTS_AGGREGATION_NAME);
    if (!(uniqueObjectsAggregation instanceof Terms)) {
      LOGGER.warning("Could not retrieve result values when searching for Objects.");
      return result;
    }

    List<? extends Terms.Bucket> buckets = Terms.class.cast(uniqueObjectsAggregation).getBuckets();
    if (CollectionUtils.isEmpty(buckets)) {
      // No buckets means no results.
      return result;
    }

    // Each bucket contains one unique Object, where the TopHits aggregation provides the document source.
    for (Terms.Bucket bucket : buckets) {
      Aggregation uniqueObjectsSourceAggregation = bucket.getAggregations().get(UNIQUE_OBJECTS_SOURCE_AGGREGATION_NAME);
      if (!(uniqueObjectsSourceAggregation instanceof TopHits)) continue;

      // Each bucket should contain only one hit with one unique Object.
      SearchHits hits = TopHits.class.cast(uniqueObjectsSourceAggregation).getHits();
      if (hits.getHits().length < 1) continue;

      // Retrieve Object document from provided search hit.
      ObjectDocument document = decodeObjectDocument(toBytes(hits.getAt(0).getSourceRef()));
      if (document != null) {
        result.add(document);
      }
    }

    return result;
  }

  private ObjectStatisticsResult retrieveObjectStatisticsResult(SearchResponse response) {
    Aggregation uniqueObjectsAggregation = resolveChildAggregation(response.getAggregations(), UNIQUE_OBJECTS_AGGREGATION_NAME);
    if (!(uniqueObjectsAggregation instanceof Terms)) {
      LOGGER.warning("Could not retrieve results when calculating statistics for Objects.");
      return ObjectStatisticsResult.builder().build();
    }

    List<? extends Terms.Bucket> uniqueObjectBuckets = Terms.class.cast(uniqueObjectsAggregation).getBuckets();
    if (CollectionUtils.isEmpty(uniqueObjectBuckets)) {
      // No buckets means no results.
      return ObjectStatisticsResult.builder().build();
    }

    ObjectStatisticsResult.Builder resultBuilder = ObjectStatisticsResult.builder();

    // Each bucket contains one unique Object. Calculate the statistics for each Object.
    for (Terms.Bucket objectBucket : uniqueObjectBuckets) {
      UUID objectID = UUID.fromString(objectBucket.getKeyAsString());

      // Resolve buckets of unique FactTypes ...
      Aggregation uniqueFactTypesAggregation = resolveChildAggregation(objectBucket.getAggregations(), UNIQUE_FACT_TYPES_AGGREGATION_NAME);
      if (!(uniqueFactTypesAggregation instanceof Terms)) continue;

      List<? extends Terms.Bucket> uniqueFactTypeBuckets = Terms.class.cast(uniqueFactTypesAggregation).getBuckets();
      if (CollectionUtils.isEmpty(uniqueFactTypeBuckets)) continue;

      // ... and add the statistics for each FactType to the result.
      for (Terms.Bucket factTypeBucket : uniqueFactTypeBuckets) {
        UUID factTypeID = UUID.fromString(factTypeBucket.getKeyAsString());
        int factCount = (int) factTypeBucket.getDocCount();
        long lastAddedTimestamp = retrieveMaxTimestamp(factTypeBucket, MAX_LAST_ADDED_TIMESTAMP_AGGREGATION_NAME);
        long lastSeenTimestamp = retrieveMaxTimestamp(factTypeBucket, MAX_LAST_SEEN_TIMESTAMP_AGGREGATION_NAME);
        resultBuilder.addStatistic(objectID, new ObjectStatisticsResult.FactStatistic(factTypeID, factCount, lastAddedTimestamp, lastSeenTimestamp));
      }
    }

    return resultBuilder.build();
  }

  private long retrieveMaxTimestamp(Terms.Bucket bucket, String targetAggregationName) {
    Aggregation maxAggregation = bucket.getAggregations().get(targetAggregationName);
    if (!(maxAggregation instanceof Max)) {
      LOGGER.warning("Could not retrieve maximum timestamp when calculating statistics for Objects.");
      return -1;
    }

    // Retrieve maximum timestamp from the max aggregation.
    return Math.round(Max.class.cast(maxAggregation).getValue());
  }

  private Aggregation resolveChildAggregation(Aggregations aggregations, String targetAggregationName) {
    if (aggregations == null) return null;

    for (Aggregation aggregation : aggregations) {
      // Check if 'aggregation' is already the target aggregation.
      if (aggregation.getName().equals(targetAggregationName)) {
        return aggregation;
      }

      // Otherwise check all sub aggregations if applicable.
      if (HasAggregations.class.isAssignableFrom(aggregation.getClass())) {
        Aggregation target = resolveChildAggregation(HasAggregations.class.cast(aggregation).getAggregations(), targetAggregationName);
        if (target != null) return target;
      }
    }

    // Couldn't find target aggregation.
    return null;
  }

  private FactDocument decodeFactDocument(UUID factID, byte[] source) {
    try {
      FactDocument fact = FACT_DOCUMENT_READER.readValue(source);
      // Need to set ID manually because it's not indexed as an own field.
      return fact.setId(factID);
    } catch (IOException ex) {
      LOGGER.warning(ex, "Could not deserialize Fact with id = %s. Source document not stored?", factID);
      return null;
    }
  }

  private ObjectDocument decodeObjectDocument(byte[] source) {
    try {
      return OBJECT_DOCUMENT_READER.readValue(source);
    } catch (IOException ex) {
      LOGGER.warning(ex, "Could not deserialize Object. Source of nested document not returned?");
      return null;
    }
  }

  private String toString(Object object) {
    return ObjectUtils.ifNotNull(object, Object::toString);
  }

  private Set<String> toString(Set<?> collection) {
    return SetUtils.set(collection, Object::toString);
  }

  private RuntimeException logAndExit(Exception ex, String msg) {
    LOGGER.error(ex, msg);
    return new IllegalStateException(msg, ex);
  }

}
