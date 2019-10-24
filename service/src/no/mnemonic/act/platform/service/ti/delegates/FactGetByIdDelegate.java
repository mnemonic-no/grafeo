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
import no.mnemonic.act.platform.service.ti.converters.FactRecordConverter;
import no.mnemonic.act.platform.service.ti.resolvers.FactResolver;

import javax.inject.Inject;

public class FactGetByIdDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final FactResolver factResolver;
  private final FactRecordConverter factConverter;

  @Inject
  public FactGetByIdDelegate(TiSecurityContext securityContext,
                             FactResolver factResolver,
                             FactRecordConverter factConverter) {
    this.securityContext = securityContext;
    this.factResolver = factResolver;
    this.factConverter = factConverter;
  }

  public Fact handle(GetFactByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    securityContext.checkPermission(TiFunctionConstants.viewFactObjects);
    FactRecord record = factResolver.resolveFact(request.getId());
    securityContext.checkReadPermission(record);
    return factConverter.apply(record);
  }
}
