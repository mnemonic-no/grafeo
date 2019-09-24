package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchMetaFactsRequest;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.SearchMetaFactsRequestConverter;
import no.mnemonic.act.platform.service.ti.handlers.FactSearchHandler;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;

public class FactSearchMetaDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final SearchMetaFactsRequestConverter requestConverter;
  private final FactSearchHandler factSearchHandler;

  @Inject
  public FactSearchMetaDelegate(TiSecurityContext securityContext,
                                SearchMetaFactsRequestConverter requestConverter,
                                FactSearchHandler factSearchHandler) {
    this.securityContext = securityContext;
    this.requestConverter = requestConverter;
    this.factSearchHandler = factSearchHandler;
  }

  public ResultSet<Fact> handle(SearchMetaFactsRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Verify that user is allowed to view Facts.
    securityContext.checkPermission(TiFunctionConstants.viewFactObjects);
    // Fetch referenced Fact, verify that it exists and that user is allowed to access the Fact.
    securityContext.checkReadPermission(fetchExistingFact(request.getFact()));
    // Search for meta Facts bound to the referenced Fact.
    return factSearchHandler.search(requestConverter.apply(request), request.getIncludeRetracted());
  }
}
