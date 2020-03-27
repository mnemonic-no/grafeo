package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.request.v1.SearchObjectRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.criteria.ObjectStatisticsCriteria;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.FactTypeByIdConverter;
import no.mnemonic.act.platform.service.ti.converters.ObjectConverter;
import no.mnemonic.act.platform.service.ti.converters.ObjectTypeByIdConverter;
import no.mnemonic.act.platform.service.ti.converters.SearchObjectRequestConverter;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ObjectSearchDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final ObjectFactDao objectFactDao;
  private final SearchObjectRequestConverter requestConverter;
  private final FactTypeByIdConverter factTypeConverter;
  private final ObjectTypeByIdConverter objectTypeConverter;

  @Inject
  public ObjectSearchDelegate(TiSecurityContext securityContext,
                              ObjectFactDao objectFactDao,
                              SearchObjectRequestConverter requestConverter,
                              FactTypeByIdConverter factTypeConverter,
                              ObjectTypeByIdConverter objectTypeConverter) {
    this.securityContext = securityContext;
    this.objectFactDao = objectFactDao;
    this.requestConverter = requestConverter;
    this.factTypeConverter = factTypeConverter;
    this.objectTypeConverter = objectTypeConverter;
  }

  public ResultSet<Object> handle(SearchObjectRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.viewFactObjects);

    FactSearchCriteria criteria = requestConverter.apply(request);
    ResultContainer<ObjectRecord> searchResult = objectFactDao.searchObjects(criteria);

    // Return search result and add statistics while iterating over the result.
    return StreamingResultSet.<Object>builder()
            .setCount(searchResult.getCount())
            .setLimit(criteria.getLimit())
            .setValues(new AddStatisticsIterator(searchResult))
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
    private Iterator<Object> output;

    private AddStatisticsIterator(ResultContainer<ObjectRecord> input) {
      this.input = input;
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

      // Use the Object IDs to retrieve the Fact statistics for one batch of Objects.
      ObjectStatisticsCriteria criteria = ObjectStatisticsCriteria.builder()
              .setObjectID(SetUtils.set(currentBatch, ObjectRecord::getId))
              .setCurrentUserID(securityContext.getCurrentUserID())
              .setAvailableOrganizationID(securityContext.getAvailableOrganizationID())
              .build();
      ObjectStatisticsContainer statistics = objectFactDao.calculateObjectStatistics(criteria);

      // Don't explicitly check access to each Object here as this would be too expensive because it requires fetching
      // Facts for each Object. Rely on the access control implemented in ElasticSearch instead. Accidentally returning
      // non-accessible Objects because of an error in the ElasticSearch access control implementation would only leak
      // the information that the Object exists (plus potentially the Fact statistics) and will not give further access
      // to any Facts.
      return currentBatch.stream()
              .map(new ObjectConverter(objectTypeConverter, factTypeConverter, statistics::getStatistics))
              .iterator();
    }
  }
}
