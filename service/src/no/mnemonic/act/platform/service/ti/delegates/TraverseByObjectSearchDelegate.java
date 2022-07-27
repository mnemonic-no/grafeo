package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.OperationTimeoutException;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectSearchRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.request.SearchObjectRequestConverter;
import no.mnemonic.act.platform.service.ti.handlers.TraverseGraphHandler;
import no.mnemonic.act.platform.service.ti.resolvers.AccessControlCriteriaResolver;
import no.mnemonic.act.platform.service.ti.resolvers.IndexSelectCriteriaResolver;
import no.mnemonic.act.platform.service.ti.tinkerpop.TraverseParams;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;
import java.util.Collection;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;

public class TraverseByObjectSearchDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final TraverseGraphHandler traverseGraphHandler;
  private final SearchObjectRequestConverter requestConverter;
  private final ObjectFactDao objectFactDao;
  private final AccessControlCriteriaResolver accessControlCriteriaResolver;
  private final IndexSelectCriteriaResolver indexSelectCriteriaResolver;

  @Inject
  public TraverseByObjectSearchDelegate(TiSecurityContext securityContext,
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
    securityContext.checkPermission(TiFunctionConstants.traverseThreatIntelFact);

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
                    .setAccessControlCriteria(accessControlCriteriaResolver.get())
                    .setIndexSelectCriteria(indexSelectCriteriaResolver.validateAndCreateCriteria(
                            request.getTraverse().getAfter(),
                            request.getTraverse().getBefore()))
                    .setIncludeRetracted(request.getTraverse().getIncludeRetracted())
                    .setAfterTimestamp(request.getTraverse().getAfter())
                    .setBeforeTimestamp(request.getTraverse().getBefore())
                    .setLimit(request.getTraverse().getLimit())
                    .build());
  }
}
