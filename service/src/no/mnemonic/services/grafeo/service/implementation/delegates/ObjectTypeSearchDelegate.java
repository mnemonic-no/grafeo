package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.model.v1.ObjectType;
import no.mnemonic.services.grafeo.api.request.v1.SearchObjectTypeRequest;
import no.mnemonic.services.grafeo.api.service.v1.StreamingResultSet;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.ObjectTypeResponseConverter;

import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class ObjectTypeSearchDelegate implements Delegate {

  private final GrafeoSecurityContext securityContext;
  private final ObjectManager objectManager;
  private final ObjectTypeResponseConverter objectTypeResponseConverter;

  @Inject
  public ObjectTypeSearchDelegate(GrafeoSecurityContext securityContext,
                                  ObjectManager objectManager,
                                  ObjectTypeResponseConverter objectTypeResponseConverter) {
    this.securityContext = securityContext;
    this.objectManager = objectManager;
    this.objectTypeResponseConverter = objectTypeResponseConverter;
  }

  public ResultSet<ObjectType> handle(SearchObjectTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(FunctionConstants.viewGrafeoType);

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
