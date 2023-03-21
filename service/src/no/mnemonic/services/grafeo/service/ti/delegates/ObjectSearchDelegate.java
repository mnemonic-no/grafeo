package no.mnemonic.services.grafeo.service.ti.delegates;

import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.model.v1.Object;
import no.mnemonic.services.grafeo.api.request.v1.SearchObjectRequest;
import no.mnemonic.services.grafeo.api.service.v1.StreamingResultSet;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.ObjectStatisticsCriteria;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.services.grafeo.dao.api.result.ResultContainer;
import no.mnemonic.services.grafeo.service.ti.TiFunctionConstants;
import no.mnemonic.services.grafeo.service.ti.TiSecurityContext;
import no.mnemonic.services.grafeo.service.ti.converters.request.SearchObjectRequestConverter;
import no.mnemonic.services.grafeo.service.ti.converters.response.ObjectResponseConverter;
import no.mnemonic.services.grafeo.service.ti.resolvers.AccessControlCriteriaResolver;
import no.mnemonic.services.grafeo.service.ti.resolvers.response.FactTypeByIdResponseResolver;
import no.mnemonic.services.grafeo.service.ti.resolvers.response.ObjectTypeByIdResponseResolver;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Function;

public class ObjectSearchDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final AccessControlCriteriaResolver accessControlCriteriaResolver;
  private final ObjectFactDao objectFactDao;
  private final SearchObjectRequestConverter requestConverter;
  private final FactTypeByIdResponseResolver factTypeConverter;
  private final ObjectTypeByIdResponseResolver objectTypeConverter;

  @Inject
  public ObjectSearchDelegate(TiSecurityContext securityContext,
                              AccessControlCriteriaResolver accessControlCriteriaResolver,
                              ObjectFactDao objectFactDao,
                              SearchObjectRequestConverter requestConverter,
                              FactTypeByIdResponseResolver factTypeConverter,
                              ObjectTypeByIdResponseResolver objectTypeConverter) {
    this.securityContext = securityContext;
    this.accessControlCriteriaResolver = accessControlCriteriaResolver;
    this.objectFactDao = objectFactDao;
    this.requestConverter = requestConverter;
    this.factTypeConverter = factTypeConverter;
    this.objectTypeConverter = objectTypeConverter;
  }

  public ResultSet<Object> handle(SearchObjectRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.viewThreatIntelFact);

    FactSearchCriteria criteria = requestConverter.apply(request);
    if (criteria.isUnbounded()) {
      throw new AccessDeniedException("Unbounded searches are not allowed. Specify at least one search parameter (in addition to 'limit').");
    }

    ResultContainer<ObjectRecord> searchResult = objectFactDao.searchObjects(criteria);

    // Return search result and add statistics while iterating over the result.
    return StreamingResultSet.<Object>builder()
            .setCount(searchResult.getCount())
            .setLimit(criteria.getLimit())
            // Use the same IndexSelectCriteria for statistics calculation than what is used for the search itself.
            .setValues(new AddStatisticsIterator(searchResult, request, criteria.getIndexSelectCriteria()))
            .build();
  }

  /**
   * This iterator wraps the search result iterator and while the result is consumed it converts the input from
   * ObjectRecord to the Object model. At the same time it adds the statistics about Facts to the returned Objects.
   */
  private class AddStatisticsIterator implements Iterator<Object> {

    // A batch size of 1000 seems to be a good trade-off between the number of requests against ElasticSearch and
    // the amount of work ElasticSearch has to perform per request. The current maximum limit for the Object search
    // is 10.000, i.e. at most 10 request will be send to ElasticSearch.
    private static final int MAXIMUM_BATCH_SIZE = 1000;

    private final ResultContainer<ObjectRecord> input;
    private final SearchObjectRequest request;
    private final IndexSelectCriteria indexSelectCriteria;
    private Iterator<Object> output;

    private AddStatisticsIterator(ResultContainer<ObjectRecord> input,
                                  SearchObjectRequest request,
                                  IndexSelectCriteria indexSelectCriteria) {
      this.input = input;
      this.request = request;
      this.indexSelectCriteria = indexSelectCriteria;
    }

    @Override
    public boolean hasNext() {
      // If this is the initial batch or the current batch has be consumed completely, fetch the next batch.
      if (output == null || !output.hasNext()) {
        output = nextOutputBatch();
      }

      return output.hasNext();
    }

    @Override
    public Object next() {
      return output.next();
    }

    private Iterator<Object> nextOutputBatch() {
      List<ObjectRecord> currentBatch = new ArrayList<>(MAXIMUM_BATCH_SIZE);

      int currentBatchSize = 0;
      // Consume input until no more data is available or maximum batch size has be reached.
      while (input.hasNext() && currentBatchSize < MAXIMUM_BATCH_SIZE) {
        currentBatch.add(input.next());
        currentBatchSize++;
      }

      // Return early because calculating the statistics will fail without any Object IDs.
      if (currentBatch.isEmpty()) {
        return Collections.emptyIterator();
      }

      // Don't explicitly check access to each Object here as this would be too expensive because it requires fetching
      // Facts for each Object. Rely on the access control implemented in ElasticSearch instead. Accidentally returning
      // non-accessible Objects because of an error in the ElasticSearch access control implementation would only leak
      // the information that the Object exists (plus potentially the Fact statistics) and will not give further access
      // to any Facts.
      return currentBatch.stream()
              .map(new ObjectResponseConverter(objectTypeConverter, factTypeConverter, initializeStatisticsResolver(currentBatch)))
              .iterator();
    }

    private Function<UUID, Collection<ObjectStatisticsContainer.FactStatistic>> initializeStatisticsResolver(List<ObjectRecord> currentBatch) {
      Function<UUID, Collection<ObjectStatisticsContainer.FactStatistic>> resolver = id -> Collections.emptyList();

      // Only include statistics if the user has explicitly asked for it.
      if (request.isIncludeStatistics()) {
        // Use the Object IDs to retrieve the Fact statistics for one batch of Objects.
        ObjectStatisticsCriteria criteria = ObjectStatisticsCriteria.builder()
                .setObjectID(SetUtils.set(currentBatch, ObjectRecord::getId))
                .setStartTimestamp(request.getAfter())
                .setEndTimestamp(request.getBefore())
                .setAccessControlCriteria(accessControlCriteriaResolver.get())
                .setIndexSelectCriteria(indexSelectCriteria)
                .build();
        ObjectStatisticsContainer statistics = objectFactDao.calculateObjectStatistics(criteria);
        resolver = statistics::getStatistics;
      }

      return resolver;
    }
  }
}
