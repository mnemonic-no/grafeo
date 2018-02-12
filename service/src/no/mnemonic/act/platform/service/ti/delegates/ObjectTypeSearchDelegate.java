package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.SearchObjectTypeRequest;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;

import java.util.List;
import java.util.stream.Collectors;

public class ObjectTypeSearchDelegate {

  public static ObjectTypeSearchDelegate create() {
    return new ObjectTypeSearchDelegate();
  }

  public ResultSet<ObjectType> handle(SearchObjectTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    SecurityContext.get().checkPermission(TiFunctionConstants.viewTypes);

    // No filtering is defined in SearchObjectTypeRequest yet, just fetch all ObjectTypes.
    List<ObjectType> types = TiRequestContext.get().getObjectManager()
            .fetchObjectTypes()
            .stream()
            .map(TiRequestContext.get().getObjectTypeConverter())
            .collect(Collectors.toList());

    return ResultSet.<ObjectType>builder()
            .setCount(types.size())
            .setLimit(0)
            .setValues(types)
            .build();
  }

}
