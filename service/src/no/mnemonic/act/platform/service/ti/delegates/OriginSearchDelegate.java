package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.api.request.v1.SearchOriginRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.OriginResponseConverter;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class OriginSearchDelegate implements Delegate {

  private static final int DEFAULT_LIMIT = 25;

  private final TiSecurityContext securityContext;
  private final OriginManager originManager;
  private final OriginResponseConverter originResponseConverter;

  @Inject
  public OriginSearchDelegate(TiSecurityContext securityContext,
                              OriginManager originManager,
                              OriginResponseConverter originResponseConverter) {
    this.securityContext = securityContext;
    this.originManager = originManager;
    this.originResponseConverter = originResponseConverter;
  }

  public ResultSet<Origin> handle(SearchOriginRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    // Verify that the user is allowed to view Origins in general.
    securityContext.checkPermission(TiFunctionConstants.viewThreatIntelOrigin);

    int limit = ObjectUtils.ifNull(request.getLimit(), DEFAULT_LIMIT);
    List<Origin> origins = originManager.fetchOrigins()
            .stream()
            // Apply 'type' filter.
            .filter(filterByType(request))
            // Apply 'includeDeleted' filter.
            .filter(filterIncludedDeleted(request))
            // Only return accessible Origins.
            .filter(securityContext::hasReadPermission)
            // Convert from entity to model.
            .map(originResponseConverter)
            // Apply limit where 0 means all results.
            .limit(limit == 0 ? Integer.MAX_VALUE : limit)
            .collect(Collectors.toList());

    return StreamingResultSet.<Origin>builder()
            .setCount(origins.size())
            .setLimit(limit)
            .setValues(origins)
            .build();
  }

  private Predicate<OriginEntity> filterByType(SearchOriginRequest request) {
    return origin -> CollectionUtils.isEmpty(request.getType())
            || origin.getType() == null // Just a fail-safe, should never happen.
            || SetUtils.set(request.getType(), Enum::name).contains(origin.getType().name());
  }

  private Predicate<OriginEntity> filterIncludedDeleted(SearchOriginRequest request) {
    return origin -> {
      boolean includeDeleted = ObjectUtils.ifNull(request.getIncludeDeleted(), false);
      return includeDeleted || !origin.isSet(OriginEntity.Flag.Deleted);
    };
  }
}
