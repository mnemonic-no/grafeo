package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.request.v1.SearchFactTypeRequest;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;

import java.util.List;
import java.util.stream.Collectors;

public class FactTypeSearchDelegate {

  public static FactTypeSearchDelegate create() {
    return new FactTypeSearchDelegate();
  }

  public ResultSet<FactType> handle(SearchFactTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    SecurityContext.get().checkPermission(TiFunctionConstants.viewTypes);

    // No filtering is defined in SearchFactTypeRequest yet, just fetch all FactTypes.
    List<FactType> types = TiRequestContext.get().getFactManager()
            .fetchFactTypes()
            .stream()
            .map(TiRequestContext.get().getFactTypeConverter())
            .collect(Collectors.toList());

    return ResultSet.<FactType>builder()
            .setCount(types.size())
            .setLimit(0)
            .setValues(types)
            .build();
  }

}
