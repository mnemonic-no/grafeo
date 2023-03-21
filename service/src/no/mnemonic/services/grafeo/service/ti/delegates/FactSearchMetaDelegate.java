package no.mnemonic.services.grafeo.service.ti.delegates;

import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.Fact;
import no.mnemonic.services.grafeo.api.request.v1.SearchMetaFactsRequest;
import no.mnemonic.services.grafeo.service.ti.TiFunctionConstants;
import no.mnemonic.services.grafeo.service.ti.TiSecurityContext;
import no.mnemonic.services.grafeo.service.ti.converters.request.SearchMetaFactsRequestConverter;
import no.mnemonic.services.grafeo.service.ti.handlers.FactSearchHandler;
import no.mnemonic.services.grafeo.service.ti.resolvers.request.FactRequestResolver;

import javax.inject.Inject;

public class FactSearchMetaDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final SearchMetaFactsRequestConverter requestConverter;
  private final FactRequestResolver factRequestResolver;
  private final FactSearchHandler factSearchHandler;

  @Inject
  public FactSearchMetaDelegate(TiSecurityContext securityContext,
                                SearchMetaFactsRequestConverter requestConverter,
                                FactRequestResolver factRequestResolver,
                                FactSearchHandler factSearchHandler) {
    this.securityContext = securityContext;
    this.requestConverter = requestConverter;
    this.factRequestResolver = factRequestResolver;
    this.factSearchHandler = factSearchHandler;
  }

  public ResultSet<Fact> handle(SearchMetaFactsRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Verify that user is allowed to view Facts.
    securityContext.checkPermission(TiFunctionConstants.viewThreatIntelFact);
    // Fetch referenced Fact, verify that it exists and that user is allowed to access the Fact.
    securityContext.checkReadPermission(factRequestResolver.resolveFact(request.getFact()));
    // Search for meta Facts bound to the referenced Fact.
    return factSearchHandler.search(requestConverter.apply(request), request.getIncludeRetracted());
  }
}
