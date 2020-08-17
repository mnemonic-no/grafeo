package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchFactRequest;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.request.SearchFactRequestConverter;
import no.mnemonic.act.platform.service.ti.handlers.FactSearchHandler;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;

public class FactSearchDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final SearchFactRequestConverter requestConverter;
  private final FactSearchHandler factSearchHandler;

  @Inject
  public FactSearchDelegate(TiSecurityContext securityContext,
                            SearchFactRequestConverter requestConverter,
                            FactSearchHandler factSearchHandler) {
    this.securityContext = securityContext;
    this.requestConverter = requestConverter;
    this.factSearchHandler = factSearchHandler;
  }

  public ResultSet<Fact> handle(SearchFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.viewThreatIntelFact);

    FactSearchCriteria criteria = requestConverter.apply(request);
    if (criteria.isUnbounded()) {
      throw new AccessDeniedException("Unbounded searches are not allowed. Specify at least one search parameter (in addition to 'limit').");
    }

    return factSearchHandler.search(criteria, request.getIncludeRetracted());
  }
}
