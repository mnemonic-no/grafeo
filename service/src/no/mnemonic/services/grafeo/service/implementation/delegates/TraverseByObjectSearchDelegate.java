package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.OperationTimeoutException;
import no.mnemonic.services.grafeo.api.request.v1.TraverseGraphByObjectSearchRequest;
import no.mnemonic.services.grafeo.api.service.v1.StreamingResultSet;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.api.result.ResultContainer;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.request.SearchObjectRequestConverter;
import no.mnemonic.services.grafeo.service.implementation.handlers.TraverseGraphHandler;
import no.mnemonic.services.grafeo.service.implementation.resolvers.AccessControlCriteriaResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.IndexSelectCriteriaResolver;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.TraverseParams;

import javax.inject.Inject;
import java.util.Collection;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static no.mnemonic.services.grafeo.service.implementation.converters.request.RequestConverterUtils.handleTimeFieldSearchRequest;

public class TraverseByObjectSearchDelegate implements Delegate {

  private final GrafeoSecurityContext securityContext;
  private final TraverseGraphHandler traverseGraphHandler;
  private final SearchObjectRequestConverter requestConverter;
  private final ObjectFactDao objectFactDao;
  private final AccessControlCriteriaResolver accessControlCriteriaResolver;
  private final IndexSelectCriteriaResolver indexSelectCriteriaResolver;

  @Inject
  public TraverseByObjectSearchDelegate(GrafeoSecurityContext securityContext,
                                        TraverseGraphHandler traverseGraphHandler,
                                        SearchObjectRequestConverter requestConverter,
                                        ObjectFactDao objectFactDao,
                                        AccessControlCriteriaResolver accessControlCriteriaResolver,
                                        IndexSelectCriteriaResolver indexSelectCriteriaResolver) {
    this.securityContext = securityContext;
    this.traverseGraphHandler = traverseGraphHandler;
    this.requestConverter = requestConverter;
    this.objectFactDao = objectFactDao;
    this.accessControlCriteriaResolver = accessControlCriteriaResolver;
    this.indexSelectCriteriaResolver = indexSelectCriteriaResolver;
  }

  public ResultSet<?> handle(TraverseGraphByObjectSearchRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    securityContext.checkPermission(FunctionConstants.traverseGrafeoFact);

    // Search for Objects and use the result as starting points for the graph traversal.
    // The search will only return Objects the current user has access to, thus, there is no need to check
    // Object access here (in contrast to the traversal with a single starting Object).
    FactSearchCriteria criteria = requestConverter.apply(request.getSearch());
    ResultContainer<ObjectRecord> searchResult = objectFactDao.searchObjects(criteria);

    Collection<UUID> objectIds = set(searchResult.iterator(), ObjectRecord::getId);
    if (objectIds.isEmpty()) {
      // Search returned no results, just return empty traversal result as well.
      return StreamingResultSet.builder().build();
    }

    return traverseGraphHandler.traverse(
            objectIds,
            request.getTraverse().getQuery(),
            TraverseParams.builder()
                    .setBaseSearchCriteria(handleTimeFieldSearchRequest(FactSearchCriteria.builder(), request.getTraverse())
                            .setAccessControlCriteria(accessControlCriteriaResolver.get())
                            .setIndexSelectCriteria(indexSelectCriteriaResolver.validateAndCreateCriteria(
                                    request.getTraverse().getStartTimestamp(),
                                    request.getTraverse().getEndTimestamp()))
                            .build())
                    .setIncludeRetracted(request.getTraverse().getIncludeRetracted())
                    .setLimit(request.getTraverse().getLimit())
                    .build());
  }
}
