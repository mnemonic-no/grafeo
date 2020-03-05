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
import no.mnemonic.act.platform.service.ti.converters.OriginConverter;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class OriginSearchDelegate implements Delegate {

  private static final int DEFAULT_LIMIT = 25;

  private final TiSecurityContext securityContext;
  private final OriginManager originManager;
  private final OriginConverter originConverter;

  @Inject
  public OriginSearchDelegate(TiSecurityContext securityContext,
                              OriginManager originManager,
                              OriginConverter originConverter) {
    this.securityContext = securityContext;
    this.originManager = originManager;
    this.originConverter = originConverter;
  }

  public ResultSet<Origin> handle(SearchOriginRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    // Verify that the user is allowed to view Origins in general.
    securityContext.checkPermission(TiFunctionConstants.viewOrigins);

    int limit = ObjectUtils.ifNull(request.getLimit(), DEFAULT_LIMIT);
    List<Origin> origins = originManager.fetchOrigins()
            .stream()
            // Apply 'includeDeleted' filter.
            .filter(filterIncludedDeleted(request))
            // Only return accessible Origins.
            .filter(securityContext::hasReadPermission)
            // Convert from entity to model.
            .map(originConverter)
            // Apply limit where 0 means all results.
            .limit(limit == 0 ? Integer.MAX_VALUE : limit)
            .collect(Collectors.toList());

    return StreamingResultSet.<Origin>builder()
            .setCount(origins.size())
            .setLimit(limit)
            .setValues(origins)
            .build();
  }

  private Predicate<OriginEntity> filterIncludedDeleted(SearchOriginRequest request) {
    return origin -> {
      Set<OriginEntity.Flag> flags = ObjectUtils.ifNull(origin.getFlags(), Collections.emptySet());
      boolean includeDeleted = ObjectUtils.ifNull(request.getIncludeDeleted(), false);
      return includeDeleted || !flags.contains(OriginEntity.Flag.Deleted);
    };
  }
}
