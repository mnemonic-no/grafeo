package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.OperationTimeoutException;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectTypeValueRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.handlers.ObjectTypeHandler;
import no.mnemonic.act.platform.service.ti.handlers.TraverseGraphHandler;
import no.mnemonic.act.platform.service.ti.tinkerpop.TraverseParams;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;
import java.util.Collections;

public class TraverseByObjectTypeValueDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final TraverseGraphHandler traverseGraphHandler;
  private final ObjectFactDao objectFactDao;
  private final ObjectTypeHandler objectTypeHandler;

  @Inject
  public TraverseByObjectTypeValueDelegate(TiSecurityContext securityContext,
                                           TraverseGraphHandler traverseGraphHandler,
                                           ObjectFactDao objectFactDao,
                                           ObjectTypeHandler objectTypeHandler) {
    this.securityContext = securityContext;
    this.traverseGraphHandler = traverseGraphHandler;
    this.objectFactDao = objectFactDao;
    this.objectTypeHandler = objectTypeHandler;
  }

  public ResultSet<?> handle(TraverseGraphByObjectTypeValueRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    securityContext.checkPermission(TiFunctionConstants.traverseFactObjects);
    objectTypeHandler.assertObjectTypeExists(request.getType(), "type");

    ObjectRecord object = objectFactDao.getObject(request.getType(), request.getValue());
    // Verify that user has access to starting point of graph traversal.
    securityContext.checkReadPermission(object);

    return traverseGraphHandler.traverse(
            Collections.singleton(object.getId()),
            request.getQuery(),
            TraverseParams.builder()
                    .setIncludeRetracted(request.getIncludeRetracted())
                    .setAfterTimestamp(request.getAfter())
                    .setBeforeTimestamp(request.getBefore())
                    .build());
  }
}
