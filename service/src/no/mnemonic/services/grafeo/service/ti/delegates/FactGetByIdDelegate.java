package no.mnemonic.services.grafeo.service.ti.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.Fact;
import no.mnemonic.services.grafeo.api.request.v1.GetFactByIdRequest;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.service.ti.TiFunctionConstants;
import no.mnemonic.services.grafeo.service.ti.TiSecurityContext;
import no.mnemonic.services.grafeo.service.ti.converters.response.FactResponseConverter;
import no.mnemonic.services.grafeo.service.ti.resolvers.request.FactRequestResolver;

import javax.inject.Inject;

public class FactGetByIdDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final FactRequestResolver factRequestResolver;
  private final FactResponseConverter factResponseConverter;

  @Inject
  public FactGetByIdDelegate(TiSecurityContext securityContext,
                             FactRequestResolver factRequestResolver,
                             FactResponseConverter factResponseConverter) {
    this.securityContext = securityContext;
    this.factRequestResolver = factRequestResolver;
    this.factResponseConverter = factResponseConverter;
  }

  public Fact handle(GetFactByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    securityContext.checkPermission(TiFunctionConstants.viewThreatIntelFact);
    FactRecord record = factRequestResolver.resolveFact(request.getId());
    securityContext.checkReadPermission(record);
    return factResponseConverter.apply(record);
  }
}
