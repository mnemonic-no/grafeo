package no.mnemonic.services.grafeo.dao.elastic;

import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.io.CharStreams;
import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.metrics.*;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.ObjectStatisticsCriteria;
import no.mnemonic.services.grafeo.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.services.grafeo.dao.elastic.document.FactDocument;
import no.mnemonic.services.grafeo.dao.elastic.result.ScrollingSearchResult;
import no.mnemonic.services.grafeo.dao.elastic.result.SearchResult;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.mnemonic.services.grafeo.dao.elastic.helpers.DailyIndexNamesGenerator.formatIndexName;
import static no.mnemonic.services.grafeo.dao.elastic.helpers.DailyIndexNamesGenerator.generateIndexNames;

/**
 * Class for indexing Facts into ElasticSearch as well as for retrieving and searching indexed Facts.
 */
@Singleton
public class FactSearchManager implements LifecycleAspect, MetricAspect {

  public enum TargetIndex {
    Daily("act-daily-"),
    TimeGlobal("act-time-global");

    private final String name;

    TargetIndex(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  private static final String ILM_POLICY_DAILY_NAME = "act-daily-retention-policy";
  private static final String BASE_TEMPLATE_NAME = "act-base-template";
  private static final String DAILY_TEMPLATE_NAME = "act-daily-template";
  private static final String TIME_GLOBAL_TEMPLATE_NAME = "act-time-global-template";
  private static final String ILM_POLICY_DAILY_JSON = "ilm_policy_daily.json";
  private static final String BASE_TEMPLATE_JSON = "template_base.json";
  private static final String DAILY_TEMPLATE_JSON = "template_daily.json";
  private static final String TIME_GLOBAL_TEMPLATE_JSON = "template_time_global.json";
  private static final int MAX_RESULT_WINDOW = 10_000; // Must be the same value as specified in template_base.json.

  private static final String FACTS_COUNT_AGGREGATION_NAME = "FactsCountAggregation";
  private static final String NESTED_OBJECTS_AGGREGATION_NAME = "NestedObjectsAggregation";
  private static final String FILTER_OBJECTS_AGGREGATION_NAME = "FilterObjectsAggregation";
  private static final String OBJECTS_COUNT_AGGREGATION_NAME = "ObjectsCountAggregation";
  private static final String UNIQUE_OBJECTS_AGGREGATION_NAME = "UniqueObjectsAggregation";
  private static final String REVERSED_FACTS_AGGREGATION_NAME = "ReversedFactsAggregation";
  private static final String MIN_MAX_FACTS_FILTERED_AGGREGATION_NAME = "MinMaxFactsFilteredAggregation";
  private static final String UNIQUE_FACT_TYPES_AGGREGATION_NAME = "UniqueFactTypesAggregation";
  private static final String FACTS_COUNT_PER_TYPE_AGGREGATION_NAME = "FactsCountPerTypeAggregation";
  private static final String MAX_LAST_ADDED_TIMESTAMP_AGGREGATION_NAME = "MaxLastAddedTimestampAggregation";
  private static final String MAX_LAST_SEEN_TIMESTAMP_AGGREGATION_NAME = "MaxLastSeenTimestampAggregation";

  private static final Logger LOGGER = Logging.getLogger(FactSearchManager.class);
  private static final ObjectMapper MAPPER = JsonMapper.builder().build();

  private final PerformanceMonitor indexMonitor = new PerformanceMonitor(TimeUnit.MINUTES, 60, 1);
  private final PerformanceMonitor factSearchInitialMonitor = new PerformanceMonitor(TimeUnit.MINUTES, 60, 1);
  private final PerformanceMonitor factSearchNextMonitor = new PerformanceMonitor(TimeUnit.MINUTES, 60, 1);
  private final PerformanceMonitor objectSearchMonitor = new PerformanceMonitor(TimeUnit.MINUTES, 60, 1);
  private final PerformanceMonitor objectStatisticsMonitor = new PerformanceMonitor(TimeUnit.MINUTES, 60, 1);

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
    // Changing an ILM policy will trigger some action on the indices associated with the policy. Because of that,
    // avoid uploading a new version when nothing has changed. The configuration defines an "internalVersion" field
    // as part of "_meta". Fetch the current configuration from the server and compare the stored "internalVersion"
    // with the one from the configuration. If the configuration has a newer "internalVersion" upload the new policy.
    if (shouldUpdateDailyIlmPolicy()) {
      uploadConfiguration("/_ilm/policy/", ILM_POLICY_DAILY_NAME, ILM_POLICY_DAILY_JSON);
    }

    // Changes to templates won't affect existing indices. Therefore, simply upload the templates every time.
    // Index mappings are configured as a component template which will form the base for all index templates.
    // This ensures that all indices will use the same mappings.
    uploadConfiguration("/_component_template/", BASE_TEMPLATE_NAME, BASE_TEMPLATE_JSON);
    uploadConfiguration("/_index_template/", DAILY_TEMPLATE_NAME, DAILY_TEMPLATE_JSON);
    uploadConfiguration("/_index_template/", TIME_GLOBAL_TEMPLATE_NAME, TIME_GLOBAL_TEMPLATE_JSON);
  }

  @Override
  public void stopComponent() {
    // NOOP
  }

  @Override
  public Metrics getMetrics() throws MetricException {
    return new MetricsData()
            .addData("indexInvocations", indexMonitor.getTotalInvocations())
            .addData("indexTimeSpent", indexMonitor.getTotalTimeSpent())
            .addData("factSearchInitialInvocations", factSearchInitialMonitor.getTotalInvocations())
            .addData("factSearchInitialTimeSpent", factSearchInitialMonitor.getTotalTimeSpent())
            .addData("factSearchNextInvocations", factSearchNextMonitor.getTotalInvocations())
            .addData("factSearchNextTimeSpent", factSearchNextMonitor.getTotalTimeSpent())
            .addData("objectSearchInvocations", objectSearchMonitor.getTotalInvocations())
            .addData("objectSearchTimeSpent", objectSearchMonitor.getTotalTimeSpent())
            .addData("objectStatisticsInvocations", objectStatisticsMonitor.getTotalInvocations())
            .addData("objectStatisticsTimeSpent", objectStatisticsMonitor.getTotalTimeSpent());
  }

  /**
   * Retrieve an indexed Fact by its UUID. Returns NULL if Fact cannot be fetched from ElasticSearch.
   *
   * @param id    UUID of indexed Fact
   * @param index Index from which the Fact will be retrieved
   * @return Indexed Fact or NULL if not available
   */
  public FactDocument getFact(UUID id, String index) {
    if (id == null) return null;
    GetResponse<FactDocument> response;

    try {
      GetRequest request = GetRequest.of(r -> r.index(index).id(id.toString()));
      response = clientFactory.getClient().get(request, FactDocument.class);
    } catch (ElasticsearchException | IOException ex) {
      throw logAndExit(ex, String.format("Could not perform request to fetch Fact with id = %s from index = %s.", id, index));
    }

    if (response.found()) {
      LOGGER.debug("Successfully fetched Fact with id = %s from index = %s.", id, index);
      return response.source();
    } else {
      // Fact isn't indexed in ElasticSearch, log warning and return null.
      LOGGER.warning("Could not fetch Fact with id = %s from index = %s. Fact not indexed?", id, index);
      return null;
    }
  }

  /**
   * Index a Fact into ElasticSearch.
   *
   * @param fact  Fact to index
   * @param index Index into which the Fact will be indexed
   * @return Indexed Fact
   */
  public FactDocument indexFact(FactDocument fact, TargetIndex index) {
    if (fact == null || fact.getId() == null) return null;
    IndexResponse response;

    String indexName = resolveIndexName(fact, index);
    try (TimerContext ignored = TimerContext.timerMillis(indexMonitor::invoked)) {
      IndexRequest<FactDocument> request = IndexRequest.of(r -> r
              .refresh(isTestEnvironment ? Refresh.True : Refresh.False)
              .index(indexName)
              .id(fact.getId().toString())
              .document(fact));
      response = clientFactory.getClient().index(request);
    } catch (ElasticsearchException | IOException ex) {
      throw logAndExit(ex, String.format("Could not perform request to index Fact with id = %s into index = %s.", fact.getId(), indexName));
    }

    if (response.result() == Result.Created) {
      LOGGER.debug("Successfully indexed Fact with id = %s into index = %s.", fact.getId(), indexName);
    } else if (response.result() == Result.Updated) {
      LOGGER.debug("Successfully re-indexed existing Fact with id = %s into index = %s.", fact.getId(), indexName);
    } else {
      LOGGER.warning("Could not index Fact with id = %s into index = %s.", fact.getId(), indexName);
    }

    return fact;
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
  public ScrollingSearchResult<UUID> searchFacts(FactSearchCriteria criteria) {
    if (criteria == null) return ScrollingSearchResult.<UUID>builder().build();

    SearchResponse<Void> response;
    try (TimerContext ignored = TimerContext.timerMillis(factSearchInitialMonitor::invoked)) {
      response = clientFactory.getClient().search(buildFactsSearchRequest(criteria), Void.class);
    } catch (ElasticsearchException | IOException ex) {
      throw logAndExit(ex, "Could not perform request to search for Facts.");
    }

    if (response.shards().total().intValue() == 0) {
      LOGGER.warning("Search for Facts did not hit any shards.");
      return ScrollingSearchResult.<UUID>builder().build();
    }

    int count = retrieveCountFromAggregations(response.aggregations(), FACTS_COUNT_AGGREGATION_NAME);

    LOGGER.debug("Successfully initiated streaming of search results. Start fetching data.");
    return ScrollingSearchResult.<UUID>builder()
            .setInitialBatch(createFactsBatch(response))
            .setFetchNextBatch(this::fetchNextFactsBatch)
            .setCount(count)
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
  public SearchResult<UUID> searchObjects(FactSearchCriteria criteria) {
    if (criteria == null) return SearchResult.<UUID>builder().build();

    SearchResponse<Void> response;
    try (TimerContext ignored = TimerContext.timerMillis(objectSearchMonitor::invoked)) {
      response = clientFactory.getClient().search(buildObjectsSearchRequest(criteria), Void.class);
    } catch (ElasticsearchException | IOException ex) {
      throw logAndExit(ex, "Could not perform request to search for Objects.");
    }

    if (response.shards().total().intValue() == 0) {
      LOGGER.warning("Search for Objects did not hit any shards.");
      return SearchResult.<UUID>builder().setLimit(criteria.getLimit()).build();
    }

    int count = retrieveCountFromAggregations(response.aggregations(), OBJECTS_COUNT_AGGREGATION_NAME);
    List<UUID> result = retrieveSearchObjectsResultValues(response.aggregations());

    LOGGER.debug("Successfully retrieved %d Objects from a total of %d matching Objects.", result.size(), count);
    return SearchResult.<UUID>builder()
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
  public ObjectStatisticsContainer calculateObjectStatistics(ObjectStatisticsCriteria criteria) {
    if (criteria == null) return ObjectStatisticsContainer.builder().build();

    SearchResponse<Void> response;
    try (TimerContext ignored = TimerContext.timerMillis(objectStatisticsMonitor::invoked)) {
      response = clientFactory.getClient().search(buildObjectStatisticsSearchRequest(criteria), Void.class);
    } catch (ElasticsearchException | IOException ex) {
      throw logAndExit(ex, "Could not perform request to calculate Object statistics.");
    }

    if (response.shards().total().intValue() == 0) {
      LOGGER.warning("Calculation of Object statistics did not hit any shards.");
      return ObjectStatisticsContainer.builder().build();
    }

    ObjectStatisticsContainer result = retrieveObjectStatisticsResult(response.aggregations());

    LOGGER.debug("Successfully retrieved statistics for %d Objects.", result.getStatisticsCount());
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

  private boolean shouldUpdateDailyIlmPolicy() {
    try {
      // The high-level REST client does NOT include the "_meta" field in the response.
      // Instead, use the low-level REST client and parse the response manually.
      Request request = new Request("GET", "/_ilm/policy/" + ILM_POLICY_DAILY_NAME);
      Response response = clientFactory.getRestClient().performRequest(request);
      return parsePolicyInternalVersionFromResponse(response) < parsePolicyInternalVersionFromConfiguration();
    } catch (ResponseException ex) {
      if (ex.getResponse().getStatusLine().getStatusCode() == 404) {
        LOGGER.info("ILM policy '%s' could not be found on the server.", ILM_POLICY_DAILY_NAME);
        return true;
      }

      throw logAndExit(ex, String.format("Could not perform request to fetch ILM policy '%s'.", ILM_POLICY_DAILY_NAME));
    } catch (IOException ex) {
      throw logAndExit(ex, String.format("Could not perform request to fetch ILM policy '%s'.", ILM_POLICY_DAILY_NAME));
    }
  }

  private int parsePolicyInternalVersionFromResponse(Response response) {
    try {
      JsonNode content = MAPPER.readTree(response.getEntity().getContent());
      if (!content.has(ILM_POLICY_DAILY_NAME) || !content.get(ILM_POLICY_DAILY_NAME).isObject()) return -1;

      return parsePolicyInternalVersionFromNode(content.get(ILM_POLICY_DAILY_NAME));
    } catch (IOException ex) {
      throw logAndExit(ex, String.format("Could not parse response for ILM policy '%s'.", ILM_POLICY_DAILY_NAME));
    }
  }

  private int parsePolicyInternalVersionFromConfiguration() {
    try (InputStream payload = FactSearchManager.class.getClassLoader().getResourceAsStream(ILM_POLICY_DAILY_JSON)) {
      return parsePolicyInternalVersionFromNode(MAPPER.readTree(payload));
    } catch (IOException ex) {
      throw logAndExit(ex, String.format("Could not parse configuration for ILM policy '%s'.", ILM_POLICY_DAILY_NAME));
    }
  }

  private int parsePolicyInternalVersionFromNode(JsonNode node) {
    if (!node.has("policy") || !node.get("policy").isObject()) return -1;

    JsonNode policy = node.get("policy");
    if (!policy.has("_meta") || !policy.get("_meta").isObject()) return -1;

    JsonNode meta = policy.get("_meta");
    if (!meta.has("internalVersion") || !meta.get("internalVersion").isInt()) return -1;

    return meta.get("internalVersion").asInt();
  }

  private void uploadConfiguration(String endpoint, String name, String jsonFile) {
    // It's much easier to just use the low-level REST client to upload the configuration.
    // Simply read the JSON content from the resource and push to the server.
    try (InputStream payload = FactSearchManager.class.getClassLoader().getResourceAsStream(jsonFile);
         InputStreamReader reader = new InputStreamReader(payload)) {
      Request request = new Request("PUT", endpoint + name);
      request.setJsonEntity(CharStreams.toString(reader));
      clientFactory.getRestClient().performRequest(request);
    } catch (IOException ex) {
      throw logAndExit(ex, String.format("Could not perform request to upload configuration '%s'.", name));
    }

    LOGGER.info("Successfully uploaded configuration '%s'.", name);
  }

  private ScrollingSearchResult.ScrollingBatch<UUID> fetchNextFactsBatch(String scrollId) {
    ScrollResponse<Void> response;
    try (TimerContext ignored = TimerContext.timerMillis(factSearchNextMonitor::invoked)) {
      ScrollRequest request = ScrollRequest.of(r -> r
              .scrollId(scrollId)
              .scroll(t -> t.time(searchScrollExpiration)));
      response = clientFactory.getClient().scroll(request, Void.class);
    } catch (ElasticsearchException | IOException ex) {
      LOGGER.warning(ex, "Could not perform request to retrieve next batch of search results. Stop scrolling.");
      return ScrollingSearchResult.emptyBatch();
    }

    return createFactsBatch(response);
  }

  private ScrollingSearchResult.ScrollingBatch<UUID> createFactsBatch(ResponseBody<Void> response) {
    List<UUID> values = response.hits().hits()
            .stream()
            .map(Hit::id)
            .filter(Objects::nonNull)
            .map(UUID::fromString)
            .toList();
    LOGGER.debug("Successfully retrieved next batch of search results (batch: %d, total: %d).",
            values.size(), ObjectUtils.ifNotNull(response.hits().total(), TotalHits::value, -1));

    boolean finished = values.size() < searchScrollSize;
    if (finished) {
      LOGGER.debug("Successfully retrieved all search results. No more data available.");
      // Close search context when all results have been fetched. If the client doesn't consume all results the context
      // will be kept open until ElasticSearch cleans it up automatically after the expiration time elapsed.
      closeSearchContext(response.scrollId());
    }

    return new ScrollingSearchResult.ScrollingBatch<>(response.scrollId(), values.iterator(), finished);
  }

  private void closeSearchContext(String scrollId) {
    // Perform this clean-up asynchronously because the client doesn't require the result.
    clientFactory.getAsyncClient().clearScroll(r -> r.scrollId(scrollId))
            .whenComplete((response, exception) -> {
              if (response != null && response.succeeded()) {
                LOGGER.debug("Successfully closed search context.");
              } else {
                LOGGER.warning("Could not close search context.");
              }

              if (exception != null) {
                LOGGER.warning(exception, "Could not close search context.");
              }
            });
  }

  private SearchRequest buildFactsSearchRequest(FactSearchCriteria criteria) {
    return searchRequestBuilder(criteria.getIndexSelectCriteria())
            // Not interested in the source as only the UUID of the matching document is needed.
            .source(s -> s.fetch(false))
            .size(searchScrollSize)
            .scroll(t -> t.time(searchScrollExpiration))
            .query(buildFactsQuery(criteria))
            // Use an aggregation to calculate the count because with daily indices the search result will contain duplicates.
            .aggregations(FACTS_COUNT_AGGREGATION_NAME, buildFactsCountAggregation())
            .build();
  }

  private SearchRequest buildObjectsSearchRequest(FactSearchCriteria criteria) {
    return searchRequestBuilder(criteria.getIndexSelectCriteria())
            // Not interested in the search hits as the search result is part of the returned aggregations.
            .size(0)
            // Not interested in total hits as the count is calculated as part of the returned aggregations.
            .trackTotalHits(t -> t.enabled(false))
            // Reduce documents to Facts matching the search criteria.
            .query(buildFactsQuery(criteria))
            .aggregations(NESTED_OBJECTS_AGGREGATION_NAME, buildObjectsAggregation(criteria))
            .build();
  }

  private SearchRequest buildObjectStatisticsSearchRequest(ObjectStatisticsCriteria criteria) {
    return searchRequestBuilder(criteria.getIndexSelectCriteria())
            // Not interested in the search hits as the search result is part of the returned aggregations.
            .size(0)
            // Not interested in total hits as the statistics search doesn't include a count.
            .trackTotalHits(t -> t.enabled(false))
            // Reduce documents to Facts matching the search criteria.
            .query(buildObjectStatisticsFactsQuery(criteria))
            .aggregations(NESTED_OBJECTS_AGGREGATION_NAME, buildObjectStatisticsAggregation(criteria))
            .build();
  }

  private SearchRequest.Builder searchRequestBuilder(IndexSelectCriteria criteria) {
    // Set common options required for all searches.
    return new SearchRequest.Builder()
            .index(selectIndices(criteria))
            // ALLOW_NO_INDICES and IGNORE_UNAVAILABLE are required in case the user specifies a time period where no indices exist.
            .allowNoIndices(true)
            .ignoreUnavailable(true)
            // Every index is explicitly selected, thus, now wildcard expansion is required.
            .expandWildcards(ExpandWildcard.None);
  }

  private List<String> selectIndices(IndexSelectCriteria criteria) {
    List<String> indices = generateIndexNames(criteria.getIndexStartTimestamp(), criteria.getIndexEndTimestamp(), TargetIndex.Daily.getName());
    // When querying daily indices always query the time global index in addition.
    indices.add(TargetIndex.TimeGlobal.getName());
    return indices;
  }

  private Query buildFactsQuery(FactSearchCriteria criteria) {
    BoolQuery.Builder rootQuery = new BoolQuery.Builder();
    applySimpleFilterQueries(criteria, rootQuery);
    applyKeywordSearchQuery(criteria, rootQuery);
    applyTimestampSearchQuery(criteria, rootQuery);
    applyNumberSearchQuery(criteria, rootQuery);

    // Always apply access control query.
    rootQuery.filter(createAccessControlQuery(criteria.getAccessControlCriteria()));
    return rootQuery.build()._toQuery();
  }

  private void applySimpleFilterQueries(FactSearchCriteria criteria, BoolQuery.Builder rootQuery) {
    if (!CollectionUtils.isEmpty(criteria.getFactID())) {
      rootQuery.filter(termsQuery("_id", criteria.getFactID()));
    }

    if (!CollectionUtils.isEmpty(criteria.getFactTypeID())) {
      rootQuery.filter(termsQuery("typeID", criteria.getFactTypeID()));
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

    if (!CollectionUtils.isEmpty(criteria.getOriginID())) {
      rootQuery.filter(termsQuery("sourceID", criteria.getOriginID()));
    }

    if (!CollectionUtils.isEmpty(criteria.getObjectID())) {
      rootQuery.filter(nestedQuery("objects", termsQuery("objects.id", criteria.getObjectID())));
    }

    if (!CollectionUtils.isEmpty(criteria.getObjectTypeID())) {
      rootQuery.filter(nestedQuery("objects", termsQuery("objects.typeID", criteria.getObjectTypeID())));
    }

    if (!CollectionUtils.isEmpty(criteria.getObjectValue())) {
      rootQuery.filter(nestedQuery("objects", termsQuery("objects.value", criteria.getObjectValue())));
    }

    if (criteria.getFactBinding() != null) {
      rootQuery.filter(termQuery("objectCount", criteria.getFactBinding().getObjectCount()));
    }
  }

  private void applyKeywordSearchQuery(FactSearchCriteria criteria, BoolQuery.Builder rootQuery) {
    if (StringUtils.isBlank(criteria.getKeywords())) return;
    applyFieldStrategy(rootQuery, field -> createFieldQuery(field, criteria.getKeywords()),
            criteria.getKeywordFieldStrategy(), criteria.getKeywordMatchStrategy());
  }

  private void applyTimestampSearchQuery(FactSearchCriteria criteria, BoolQuery.Builder rootQuery) {
    if (criteria.getStartTimestamp() == null && criteria.getEndTimestamp() == null) return;

    // For daily indices, apply time field and match strategies.
    BoolQuery.Builder dailyQuery = new BoolQuery.Builder();
    applyFieldStrategy(dailyQuery, field -> createFieldQuery(field, criteria.getStartTimestamp(), criteria.getEndTimestamp()),
            criteria.getTimeFieldStrategy(), criteria.getTimeMatchStrategy());

    // For time global index, always search on 'timestamp' and ignore 'startTimestamp'. The 'flags' field is used
    // as an indicator whether the document resides inside a daily or the time global index.
    BoolQuery.Builder timeGlobalQuery = new BoolQuery.Builder()
            .filter(termQuery("flags", FactDocument.Flag.TimeGlobalIndex))
            .filter(createFieldQuery("timestamp", null, criteria.getEndTimestamp()));

    rootQuery.filter(f -> f.bool(b -> b
            .should(dailyQuery.build()._toQuery())
            .should(timeGlobalQuery.build()._toQuery())));
  }

  private void applyNumberSearchQuery(FactSearchCriteria criteria, BoolQuery.Builder rootQuery) {
    if (criteria.getMinNumber() == null && criteria.getMaxNumber() == null) return;
    applyFieldStrategy(rootQuery, field -> RangeQuery.of(q -> q
            .number(n -> n.field(field)
                    .gte(ObjectUtils.ifNotNull(criteria.getMinNumber(), Number::doubleValue))
                    .lte(ObjectUtils.ifNotNull(criteria.getMaxNumber(), Number::doubleValue)))
    )._toQuery(), criteria.getNumberFieldStrategy(), criteria.getNumberMatchStrategy());
  }

  private void applyFieldStrategy(BoolQuery.Builder rootQuery, Function<String, Query> fieldQueryResolver,
                                  Set<? extends FactSearchCriteria.FieldStrategy> fieldStrategies,
                                  FactSearchCriteria.MatchStrategy matchStrategy) {
    // Determine all fields to query.
    Set<String> fieldsToQuery = fieldStrategies.stream()
            .flatMap(strategy -> strategy.getFields().stream())
            .collect(Collectors.toSet());

    BoolQuery.Builder strategyQuery = new BoolQuery.Builder();
    for (String field : fieldsToQuery) {
      if (matchStrategy == FactSearchCriteria.MatchStrategy.all) {
        // Field query must match all fields.
        strategyQuery.filter(fieldQueryResolver.apply(field));
      } else {
        // Field query should match at least one field.
        strategyQuery.should(fieldQueryResolver.apply(field));
      }
    }

    rootQuery.filter(strategyQuery.build()._toQuery());
  }

  private Query createFieldQuery(String field, String keywords) {
    Query query = SimpleQueryStringQuery.of(q -> q
            .query(keywords)
            .fields(field)
            // Values are indexed differently. Avoid errors when executing an IP search against a text field, for example.
            .lenient(true)
    )._toQuery();
    // If field starts with the prefix 'objects.' it's part of the nested objects, thus, it must be wrapped inside a nested query.
    return field.startsWith("objects.") ? nestedQuery("objects", query) : query;
  }

  private Query createFieldQuery(String field, Long startTimestamp, Long endTimestamp) {
    // Negative timestamps are omitted by providing NULL to from() and to().
    return RangeQuery.of(q -> q
            .date(d -> d.field(field)
                    .gte(startTimestamp != null && startTimestamp > 0 ? String.valueOf(startTimestamp) : null)
                    .lte(endTimestamp != null && endTimestamp > 0 ? String.valueOf(endTimestamp) : null))
    )._toQuery();
  }

  private Query createAccessControlQuery(AccessControlCriteria accessControlCriteria) {
    // Query to verify that user has access to Fact ...
    return BoolQuery.of(accessQuery -> accessQuery
            // ... if Fact is public.
            .should(termQuery("accessMode", FactDocument.AccessMode.Public))
            // ... if AccessMode == Explicit user must be in ACL.
            .should(BoolQuery.of(explicitQuery -> explicitQuery
                    .filter(termQuery("accessMode", FactDocument.AccessMode.Explicit))
                    .filter(termsQuery("acl", accessControlCriteria.getCurrentUserIdentities())))._toQuery()
            )
            // ... if AccessMode == RoleBased user must be in ACL or have access to the owning Organization.
            .should(BoolQuery.of(roleBasedQuery -> roleBasedQuery
                    .filter(termQuery("accessMode", FactDocument.AccessMode.RoleBased))
                    .filter(BoolQuery.of(aclOrOrganizationQuery -> aclOrOrganizationQuery
                            .should(termsQuery("acl", accessControlCriteria.getCurrentUserIdentities()))
                            .should(termsQuery("organizationID", accessControlCriteria.getAvailableOrganizationID())))._toQuery()
                    ))._toQuery()
            ))._toQuery();
  }

  private Aggregation buildFactsCountAggregation() {
    // Calculate the number of unique Facts by id. This will give the 'count' value.
    // If 'count' is smaller than MAX_RESULT_WINDOW a correct value is expected, thus,
    // the precision threshold is set to MAX_RESULT_WINDOW. Everything above will be approximate!
    return CardinalityAggregation.of(c -> c
            .field("id")
            .precisionThreshold(MAX_RESULT_WINDOW)
    )._toAggregation();
  }

  private Aggregation buildObjectsAggregation(FactSearchCriteria criteria) {
    return Aggregation.of(root -> root
            // 1. Map to nested Object documents.
            .nested(n -> n.path("objects"))
            // 2. Reduce to Objects matching the search criteria.
            .aggregations(FILTER_OBJECTS_AGGREGATION_NAME, f -> f.filter(buildObjectsQuery(criteria))
                    // 3. Calculate the number of unique Objects by id. This will give the 'count' value.
                    // If 'count' is smaller than MAX_RESULT_WINDOW a correct value is expected, thus,
                    // the precision threshold is set to MAX_RESULT_WINDOW.
                    .aggregations(OBJECTS_COUNT_AGGREGATION_NAME, c -> c.cardinality(c1 -> c1
                            .field("objects.id")
                            .precisionThreshold(MAX_RESULT_WINDOW)))
                    // 4. Reduce to buckets of unique Objects by id, restricted to the search criteria's limit.
                    // This will give the actual search results.
                    .aggregations(UNIQUE_OBJECTS_AGGREGATION_NAME, t -> applyOptionalBucketSelector(criteria, t.terms(t1 -> t1
                            .field("objects.id")
                            .size(calculateMaximumSize(criteria)))))));
  }

  private Aggregation.Builder.ContainerBuilder applyOptionalBucketSelector(
          FactSearchCriteria criteria, Aggregation.Builder.ContainerBuilder uniqueObjectsAggr) {
    if (criteria.getMinimumFactsCount() == null && criteria.getMaximumFactsCount() == null) {
      // No additional selection of buckets.
      return uniqueObjectsAggr;
    }

    long min = ObjectUtils.ifNull(criteria.getMinimumFactsCount(), 0);
    long max = ObjectUtils.ifNull(criteria.getMaximumFactsCount(), Integer.MAX_VALUE);

    return uniqueObjectsAggr
            // Reverse nested aggregation to have access to parent Facts.
            .aggregations(REVERSED_FACTS_AGGREGATION_NAME, r -> r.reverseNested(r1 -> r1)
                    // Calculate the number of Facts. The result will exclude Facts which have been filtered out previously.
                    .aggregations(FACTS_COUNT_AGGREGATION_NAME, buildFactsCountAggregation()))
            // Only select Objects, i.e. omit buckets, which have min <= count(facts) <= max.
            .aggregations(MIN_MAX_FACTS_FILTERED_AGGREGATION_NAME, selector -> selector
                    .bucketSelector(s -> s
                            .bucketsPath(path -> path
                                    .dict(Collections.singletonMap("count", REVERSED_FACTS_AGGREGATION_NAME + ">" + FACTS_COUNT_AGGREGATION_NAME)))
                            .script(script -> script
                                    .source(String.format("params.count >= %d && params.count <= %d", min, max)))));
  }

  private Query buildObjectsQuery(FactSearchCriteria criteria) {
    BoolQuery.Builder rootQuery = new BoolQuery.Builder();

    // Apply all simple filter queries on Objects. It's not necessary to wrap them inside a nested query because the
    // query is executed inside a nested aggregation which has direct access to the nested documents.
    if (!CollectionUtils.isEmpty(criteria.getObjectID())) {
      rootQuery.filter(termsQuery("objects.id", criteria.getObjectID()));
    }

    if (!CollectionUtils.isEmpty(criteria.getObjectTypeID())) {
      rootQuery.filter(termsQuery("objects.typeID", criteria.getObjectTypeID()));
    }

    if (!CollectionUtils.isEmpty(criteria.getObjectValue())) {
      rootQuery.filter(termsQuery("objects.value", criteria.getObjectValue()));
    }

    // Apply keyword search on Object values if necessary.
    Set<FactSearchCriteria.KeywordFieldStrategy> objectFieldStrategy = onlyObjectFieldStrategy(criteria);
    if (!StringUtils.isBlank(criteria.getKeywords()) && !CollectionUtils.isEmpty(objectFieldStrategy)) {
      // Values are indexed differently. Avoid errors by setting 'lenient' to true.
      applyFieldStrategy(rootQuery, field -> SimpleQueryStringQuery.of(q -> q
              .query(criteria.getKeywords())
              .fields(field)
              .lenient(true)
      )._toQuery(), objectFieldStrategy, criteria.getKeywordMatchStrategy());
    }

    return rootQuery.build()._toQuery();
  }

  private Set<FactSearchCriteria.KeywordFieldStrategy> onlyObjectFieldStrategy(FactSearchCriteria criteria) {
    Set<FactSearchCriteria.KeywordFieldStrategy> strategy = SetUtils.set();

    // When keyword search is performed directly on Objects only consider the relevant field strategies.
    if (criteria.getKeywordFieldStrategy().contains(FactSearchCriteria.KeywordFieldStrategy.all) ||
            criteria.getKeywordFieldStrategy().contains(FactSearchCriteria.KeywordFieldStrategy.objectValueText)) {
      strategy.add(FactSearchCriteria.KeywordFieldStrategy.objectValueText);
    }
    if (criteria.getKeywordFieldStrategy().contains(FactSearchCriteria.KeywordFieldStrategy.all) ||
            criteria.getKeywordFieldStrategy().contains(FactSearchCriteria.KeywordFieldStrategy.objectValueIp)) {
      strategy.add(FactSearchCriteria.KeywordFieldStrategy.objectValueIp);
    }
    if (criteria.getKeywordFieldStrategy().contains(FactSearchCriteria.KeywordFieldStrategy.all) ||
            criteria.getKeywordFieldStrategy().contains(FactSearchCriteria.KeywordFieldStrategy.objectValueDomain)) {
      strategy.add(FactSearchCriteria.KeywordFieldStrategy.objectValueDomain);
    }

    return strategy;
  }

  private int calculateMaximumSize(FactSearchCriteria criteria) {
    return criteria.getLimit() > 0 && criteria.getLimit() < MAX_RESULT_WINDOW ? criteria.getLimit() : MAX_RESULT_WINDOW;
  }

  private Query buildObjectStatisticsFactsQuery(ObjectStatisticsCriteria criteria) {
    BoolQuery.Builder factsQuery = new BoolQuery.Builder();
    // Always apply access control query.
    factsQuery.filter(createAccessControlQuery(criteria.getAccessControlCriteria()));
    // Optionally omit Facts which haven't been seen within the given time frame.
    if (criteria.getStartTimestamp() != null || criteria.getEndTimestamp() != null) {
      factsQuery.filter(createFieldQuery("lastSeenTimestamp", criteria.getStartTimestamp(), criteria.getEndTimestamp()));
    }

    return factsQuery.build()._toQuery();
  }

  private Aggregation buildObjectStatisticsAggregation(ObjectStatisticsCriteria criteria) {
    Query objectsQuery = termsQuery("objects.id", criteria.getObjectID());

    return Aggregation.of(root -> root
            // 1. Map to nested Object documents.
            .nested(n -> n.path("objects"))
            // 2. Reduce to only the Objects for which statistics should be calculated.
            .aggregations(FILTER_OBJECTS_AGGREGATION_NAME, f -> f.filter(objectsQuery)
                    // 3. Reduce to buckets of unique Objects by id. There shouldn't be more buckets than the
                    // number of Objects for which statistics will be calculated ('size' parameter).
                    .aggregations(UNIQUE_OBJECTS_AGGREGATION_NAME, t -> t.terms(t1 -> t1
                                    .field("objects.id")
                                    .size(criteria.getObjectID().size()))
                            // 4. Reverse nested aggregation to have access to parent Facts.
                            .aggregations(REVERSED_FACTS_AGGREGATION_NAME, r -> r.reverseNested(r1 -> r1)
                                    // 5. Create one bucket for each FactType. Set 'size' to MAX_RESULT_WINDOW
                                    // in order to get the statistics for all FactTypes.
                                    .aggregations(UNIQUE_FACT_TYPES_AGGREGATION_NAME, t2 -> t2.terms(t3 -> t3
                                                    .field("typeID")
                                                    .size(MAX_RESULT_WINDOW))
                                            // 6. Calculate the number of unique Facts. This will give the number of
                                            // Facts per FactType. Note that values above MAX_RESULT_WINDOW will be
                                            // approximate. That is acceptable because this aggregation returns statistics,
                                            // i.e. the exact numbers are not important.
                                            .aggregations(FACTS_COUNT_PER_TYPE_AGGREGATION_NAME, c -> c.cardinality(c1 -> c1
                                                    .field("id")
                                                    .precisionThreshold(MAX_RESULT_WINDOW)))
                                            // 7. Calculate the maximum lastAddedTimestamp per FactType.
                                            .aggregations(MAX_LAST_ADDED_TIMESTAMP_AGGREGATION_NAME, m -> m.max(m1 -> m1
                                                    .field("timestamp")))
                                            // 8. Calculate the maximum lastSeenTimestamp per FactType.
                                            .aggregations(MAX_LAST_SEEN_TIMESTAMP_AGGREGATION_NAME, m -> m.max(m1 -> m1
                                                    .field("lastSeenTimestamp"))))))));
  }

  private int retrieveCountFromAggregations(Map<String, Aggregate> aggregations, String aggregationName) {
    Aggregate countAggregation = resolveChildAggregation(aggregations, aggregationName);
    if (countAggregation == null || !countAggregation.isCardinality()) {
      LOGGER.warning("Could not retrieve count for aggregation %s.", aggregationName);
      return -1;
    }

    // Retrieve count from the cardinality aggregation.
    return (int) countAggregation.cardinality().value();
  }

  private List<UUID> retrieveSearchObjectsResultValues(Map<String, Aggregate> aggregations) {
    Aggregate uniqueObjectsAggregation = resolveChildAggregation(aggregations, UNIQUE_OBJECTS_AGGREGATION_NAME);
    if (uniqueObjectsAggregation == null || !uniqueObjectsAggregation.isSterms()) {
      LOGGER.warning("Could not retrieve result values when searching for Objects.");
      return ListUtils.list();
    }

    List<StringTermsBucket> buckets = uniqueObjectsAggregation.sterms().buckets().array();
    if (CollectionUtils.isEmpty(buckets)) {
      // No buckets mean no results.
      return ListUtils.list();
    }

    // Each bucket contains one unique Object where the key is the Object's ID.
    return buckets.stream()
            .map(StringTermsBucket::key)
            .map(key -> UUID.fromString(key.stringValue()))
            .collect(Collectors.toList());
  }

  private ObjectStatisticsContainer retrieveObjectStatisticsResult(Map<String, Aggregate> aggregations) {
    Aggregate uniqueObjectsAggregation = resolveChildAggregation(aggregations, UNIQUE_OBJECTS_AGGREGATION_NAME);
    if (uniqueObjectsAggregation == null || !uniqueObjectsAggregation.isSterms()) {
      LOGGER.warning("Could not retrieve results when calculating statistics for Objects.");
      return ObjectStatisticsContainer.builder().build();
    }

    List<StringTermsBucket> uniqueObjectBuckets = uniqueObjectsAggregation.sterms().buckets().array();
    if (CollectionUtils.isEmpty(uniqueObjectBuckets)) {
      // No buckets means no results.
      return ObjectStatisticsContainer.builder().build();
    }

    ObjectStatisticsContainer.Builder resultBuilder = ObjectStatisticsContainer.builder();

    // Each bucket contains one unique Object. Calculate the statistics for each Object.
    for (StringTermsBucket objectBucket : uniqueObjectBuckets) {
      UUID objectID = UUID.fromString(objectBucket.key().stringValue());

      // Resolve buckets of unique FactTypes ...
      Aggregate uniqueFactTypesAggregation = resolveChildAggregation(objectBucket.aggregations(), UNIQUE_FACT_TYPES_AGGREGATION_NAME);
      if (uniqueFactTypesAggregation == null || !uniqueFactTypesAggregation.isSterms()) continue;

      List<StringTermsBucket> uniqueFactTypeBuckets = uniqueFactTypesAggregation.sterms().buckets().array();
      if (CollectionUtils.isEmpty(uniqueFactTypeBuckets)) continue;

      // ... and add the statistics for each FactType to the result.
      for (StringTermsBucket factTypeBucket : uniqueFactTypeBuckets) {
        UUID factTypeID = UUID.fromString(factTypeBucket.key().stringValue());
        int factCount = retrieveCountFromAggregations(factTypeBucket.aggregations(), FACTS_COUNT_PER_TYPE_AGGREGATION_NAME);
        long lastAddedTimestamp = retrieveMaxTimestamp(factTypeBucket, MAX_LAST_ADDED_TIMESTAMP_AGGREGATION_NAME);
        long lastSeenTimestamp = retrieveMaxTimestamp(factTypeBucket, MAX_LAST_SEEN_TIMESTAMP_AGGREGATION_NAME);
        resultBuilder.addStatistic(objectID, new ObjectStatisticsContainer.FactStatistic(factTypeID, factCount, lastAddedTimestamp, lastSeenTimestamp));
      }
    }

    return resultBuilder.build();
  }

  private long retrieveMaxTimestamp(StringTermsBucket bucket, String targetAggregationName) {
    Aggregate maxAggregation = bucket.aggregations().get(targetAggregationName);
    if (maxAggregation == null || !maxAggregation.isMax()) {
      LOGGER.warning("Could not retrieve maximum timestamp when calculating statistics for Objects.");
      return -1;
    }

    // Retrieve maximum timestamp from the max aggregation.
    return Math.round(maxAggregation.max().value());
  }

  private Aggregate resolveChildAggregation(Map<String, Aggregate> aggregations, String targetAggregationName) {
    if (aggregations == null) return null;

    for (Map.Entry<String, Aggregate> aggregation : aggregations.entrySet()) {
      // Check if 'aggregation' is already the target aggregation.
      if (aggregation.getKey().equals(targetAggregationName)) {
        return aggregation.getValue();
      }

      // Otherwise check all sub aggregations if applicable.
      if (aggregation.getValue()._get() instanceof SingleBucketAggregateBase sub) {
        Aggregate target = resolveChildAggregation(sub.aggregations(), targetAggregationName);
        if (target != null) return target;
      }
    }

    // Couldn't find target aggregation.
    return null;
  }

  private String resolveIndexName(FactDocument fact, TargetIndex index) {
    if (index == TargetIndex.Daily) {
      // Always index a Fact into daily indices based on its 'lastSeenTimestamp' field.
      return formatIndexName(fact.getLastSeenTimestamp(), index.getName());
    }

    return index.getName();
  }

  private Query termsQuery(String field, Collection<?> values) {
    return new TermsQuery.Builder()
            .field(field)
            .terms(t -> t.value(ListUtils.list(values, FieldValue::of)))
            .build()._toQuery();
  }

  private Query termQuery(String field, Object value) {
    return new TermQuery.Builder()
            .field(field)
            .value(FieldValue.of(value))
            .build()._toQuery();
  }

  private Query nestedQuery(String path, Query query) {
    return new NestedQuery.Builder()
            .path(path)
            .query(query)
            .scoreMode(ChildScoreMode.None)
            .build()._toQuery();
  }

  private RuntimeException logAndExit(Exception ex, String msg) {
    LOGGER.error(ex, msg);
    return new IllegalStateException(msg, ex);
  }

}
