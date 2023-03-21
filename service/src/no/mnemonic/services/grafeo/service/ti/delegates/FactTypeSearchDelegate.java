package no.mnemonic.services.grafeo.service.ti.delegates;

import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.model.v1.FactType;
import no.mnemonic.services.grafeo.api.request.v1.SearchFactTypeRequest;
import no.mnemonic.services.grafeo.api.service.v1.StreamingResultSet;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.service.ti.TiFunctionConstants;
import no.mnemonic.services.grafeo.service.ti.TiSecurityContext;
import no.mnemonic.services.grafeo.service.ti.converters.response.FactTypeResponseConverter;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class FactTypeSearchDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final FactManager factManager;
  private final FactTypeResponseConverter factTypeResponseConverter;

  @Inject
  public FactTypeSearchDelegate(TiSecurityContext securityContext,
                                FactManager factManager,
                                FactTypeResponseConverter factTypeResponseConverter) {
    this.securityContext = securityContext;
    this.factManager = factManager;
    this.factTypeResponseConverter = factTypeResponseConverter;
  }

  public ResultSet<FactType> handle(SearchFactTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.viewThreatIntelType);

    // No filtering is defined in SearchFactTypeRequest yet, just fetch all FactTypes.
    List<FactType> types = factManager
            .fetchFactTypes()
            .stream()
            .map(factTypeResponseConverter)
            .collect(Collectors.toList());

    return StreamingResultSet.<FactType>builder()
            .setCount(types.size())
            .setLimit(0)
            .setValues(types)
            .build();
  }
}
