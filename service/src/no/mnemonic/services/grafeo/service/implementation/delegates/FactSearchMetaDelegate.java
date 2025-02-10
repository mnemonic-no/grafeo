package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.Fact;
import no.mnemonic.services.grafeo.api.request.v1.SearchMetaFactsRequest;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.request.SearchMetaFactsRequestConverter;
import no.mnemonic.services.grafeo.service.implementation.handlers.FactSearchHandler;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactRequestResolver;

import jakarta.inject.Inject;

public class FactSearchMetaDelegate implements Delegate {

  private final GrafeoSecurityContext securityContext;
  private final SearchMetaFactsRequestConverter requestConverter;
  private final FactRequestResolver factRequestResolver;
  private final FactSearchHandler factSearchHandler;

  @Inject
  public FactSearchMetaDelegate(GrafeoSecurityContext securityContext,
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
    securityContext.checkPermission(FunctionConstants.viewGrafeoFact);
    // Fetch referenced Fact, verify that it exists and that user is allowed to access the Fact.
    securityContext.checkReadPermission(factRequestResolver.resolveFact(request.getFact()));
    // Search for meta Facts bound to the referenced Fact.
    return factSearchHandler.search(requestConverter.apply(request), request.getIncludeRetracted());
  }
}
