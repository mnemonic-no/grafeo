package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.SearchObjectTypeRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.ObjectTypeResponseConverter;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class ObjectTypeSearchDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final ObjectManager objectManager;
  private final ObjectTypeResponseConverter objectTypeResponseConverter;

  @Inject
  public ObjectTypeSearchDelegate(TiSecurityContext securityContext,
                                  ObjectManager objectManager,
                                  ObjectTypeResponseConverter objectTypeResponseConverter) {
    this.securityContext = securityContext;
    this.objectManager = objectManager;
    this.objectTypeResponseConverter = objectTypeResponseConverter;
  }

  public ResultSet<ObjectType> handle(SearchObjectTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.viewTypes);

    // No filtering is defined in SearchObjectTypeRequest yet, just fetch all ObjectTypes.
    List<ObjectType> types = objectManager
            .fetchObjectTypes()
            .stream()
            .map(objectTypeResponseConverter)
            .collect(Collectors.toList());

    return StreamingResultSet.<ObjectType>builder()
            .setCount(types.size())
            .setLimit(0)
            .setValues(types)
            .build();
  }
}
