package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.model.v1.Origin;
import no.mnemonic.services.grafeo.api.request.v1.SearchOriginRequest;
import no.mnemonic.services.grafeo.api.service.v1.StreamingResultSet;
import no.mnemonic.services.grafeo.dao.cassandra.OriginManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.OriginResponseConverter;

import jakarta.inject.Inject;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class OriginSearchDelegate implements Delegate {

  private static final int DEFAULT_LIMIT = 25;

  private final GrafeoSecurityContext securityContext;
  private final OriginManager originManager;
  private final OriginResponseConverter originResponseConverter;

  @Inject
  public OriginSearchDelegate(GrafeoSecurityContext securityContext,
                              OriginManager originManager,
                              OriginResponseConverter originResponseConverter) {
    this.securityContext = securityContext;
    this.originManager = originManager;
    this.originResponseConverter = originResponseConverter;
  }

  public ResultSet<Origin> handle(SearchOriginRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    // Verify that the user is allowed to view Origins in general.
    securityContext.checkPermission(FunctionConstants.viewGrafeoOrigin);

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
