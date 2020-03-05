package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.request.v1.SearchFactTypeRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.FactTypeConverter;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class FactTypeSearchDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final FactManager factManager;
  private final FactTypeConverter factTypeConverter;

  @Inject
  public FactTypeSearchDelegate(TiSecurityContext securityContext,
                                FactManager factManager,
                                FactTypeConverter factTypeConverter) {
    this.securityContext = securityContext;
    this.factManager = factManager;
    this.factTypeConverter = factTypeConverter;
  }

  public ResultSet<FactType> handle(SearchFactTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.viewTypes);

    // No filtering is defined in SearchFactTypeRequest yet, just fetch all FactTypes.
    List<FactType> types = factManager
            .fetchFactTypes()
            .stream()
            .map(factTypeConverter)
            .collect(Collectors.toList());

    return StreamingResultSet.<FactType>builder()
            .setCount(types.size())
            .setLimit(0)
            .setValues(types)
            .build();
  }
}
