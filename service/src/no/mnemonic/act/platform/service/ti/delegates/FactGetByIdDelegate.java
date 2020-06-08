package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.GetFactByIdRequest;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.FactResponseConverter;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactRequestResolver;

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
