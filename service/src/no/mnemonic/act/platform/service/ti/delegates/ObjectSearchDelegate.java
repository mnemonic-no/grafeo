package no.mnemonic.act.platform.service.ti.delegates;

import com.google.common.collect.Streams;
import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.SearchObjectRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.api.ObjectStatisticsCriteria;
import no.mnemonic.act.platform.dao.api.ObjectStatisticsResult;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.dao.elastic.document.SearchResult;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.ObjectConverter;
import no.mnemonic.act.platform.service.ti.converters.SearchObjectRequestConverter;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ObjectSearchDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final ObjectManager objectManager;
  private final FactSearchManager factSearchManager;
  private final SearchObjectRequestConverter requestConverter;
  private final Function<UUID, FactType> factTypeConverter;
  private final Function<UUID, ObjectType> objectTypeConverter;

  @Inject
  public ObjectSearchDelegate(TiSecurityContext securityContext,
                              ObjectManager objectManager,
                              FactSearchManager factSearchManager,
                              SearchObjectRequestConverter requestConverter,
                              Function<UUID, FactType> factTypeConverter,
                              Function<UUID, ObjectType> objectTypeConverter) {
    this.securityContext = securityContext;
    this.objectManager = objectManager;
    this.factSearchManager = factSearchManager;
    this.requestConverter = requestConverter;
    this.factTypeConverter = factTypeConverter;
    this.objectTypeConverter = objectTypeConverter;
  }

  public ResultSet<Object> handle(SearchObjectRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.viewFactObjects);

    // Search for Objects in ElasticSearch and pick out all Object IDs.
    SearchResult<ObjectDocument> searchResult = factSearchManager.searchObjects(requestConverter.apply(request));
    List<UUID> objectID = searchResult.getValues()
            .stream()
            .map(ObjectDocument::getId)
            .collect(Collectors.toList());

    // Return early if no Objects could be found because calculating the Fact statistics will fail without any Object IDs.
    if (CollectionUtils.isEmpty(objectID)) {
      return StreamingResultSet.<Object>builder()
              .setCount(searchResult.getCount())
              .setLimit(searchResult.getLimit())
              .build();
    }

    // Use the Object IDs to retrieve the Fact statistics for all Objects from ElasticSearch.
    ObjectStatisticsCriteria criteria = ObjectStatisticsCriteria.builder()
            .setObjectID(SetUtils.set(objectID))
            .setCurrentUserID(securityContext.getCurrentUserID())
            .setAvailableOrganizationID(securityContext.getAvailableOrganizationID())
            .build();
    ObjectStatisticsResult statisticsResult = factSearchManager.calculateObjectStatistics(criteria);

    // Use the Object IDs to look up the authoritative data in Cassandra. This relies exclusively on access control
    // implemented in ElasticSearch. Explicitly checking access to each Object would be too expensive because this
    // requires fetching Facts for each Object. In addition, accidentally returning non-accessible Objects because
    // of an error in the ElasticSearch access control implementation will only leak the information that the Object
    // exists (plus potentially the Fact statistics) and will not give further access to any Facts.
    ObjectConverter converter = createObjectConverter(statisticsResult);
    Iterator<Object> objects = Streams.stream(objectManager.getObjects(objectID))
            .map(converter)
            .iterator();

    return StreamingResultSet.<Object>builder()
            .setCount(searchResult.getCount())
            .setLimit(searchResult.getLimit())
            .setValues(objects)
            .build();
  }

  private ObjectConverter createObjectConverter(ObjectStatisticsResult statistics) {
    return new ObjectConverter(objectTypeConverter, factTypeConverter, statistics::getStatistics);
  }
}
