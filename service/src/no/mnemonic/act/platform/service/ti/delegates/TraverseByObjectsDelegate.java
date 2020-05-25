package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.OperationTimeoutException;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectsRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.handlers.ObjectTypeHandler;
import no.mnemonic.act.platform.service.ti.handlers.TraverseGraphHandler;
import no.mnemonic.act.platform.service.ti.tinkerpop.TraverseParams;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TraverseByObjectsDelegate implements Delegate {

  private static final Pattern TYPE_VALUE_PATTERN = Pattern.compile("([^/]+)/(.+)");

  private final TiSecurityContext securityContext;
  private final TraverseGraphHandler traverseGraphHandler;
  private final ObjectFactDao objectFactDao;
  private final ObjectTypeHandler objectTypeHandler;

  @Inject
  public TraverseByObjectsDelegate(TiSecurityContext securityContext,
                                   TraverseGraphHandler traverseGraphHandler,
                                   ObjectFactDao objectFactDao,
                                   ObjectTypeHandler objectTypeHandler) {
    this.securityContext = securityContext;
    this.traverseGraphHandler = traverseGraphHandler;
    this.objectFactDao = objectFactDao;
    this.objectTypeHandler = objectTypeHandler;
  }

  public ResultSet<?> handle(TraverseGraphByObjectsRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    securityContext.checkPermission(TiFunctionConstants.traverseFactObjects);

    Set<ObjectRecord> objects = new HashSet<>();
    for (String objectIdentifier : request.getObjects()) {
      ObjectRecord objectRecord = resolveObject(objectIdentifier);
      securityContext.checkReadPermission(objectRecord);
      objects.add(objectRecord);
    }

    return traverseGraphHandler.traverse(
            SetUtils.set(objects, ObjectRecord::getId),
            request.getQuery(),
            TraverseParams.builder()
                    .setIncludeRetracted(request.getIncludeRetracted())
                    .setAfterTimestamp(request.getAfter())
                    .setBeforeTimestamp(request.getBefore())
                    .setLimit(request.getLimit())
                    .build());
  }


  private ObjectRecord resolveObject(String object) throws InvalidArgumentException {
    if (StringUtils.isBlank(object)) return null;

    // If input is a UUID just try to fetch Object by ID.
    if (StringUtils.isUUID(object)) {
      return objectFactDao.getObject(UUID.fromString(object));
    }

    // Otherwise try to fetch Object by type and value.
    Matcher matcher = TYPE_VALUE_PATTERN.matcher(object);
    if (!matcher.matches()) {
      // Input doesn't conform to 'type/value' pattern. Can't fetch Object by type and value.
      return null;
    }

    // Extract type and value from input.
    String type = matcher.group(1);
    String value = matcher.group(2);

    // Validate that object type exists (otherwise getObject(type, value) will thrown an IllegalArgumentException).
    objectTypeHandler.assertObjectTypeExists(type, "type");

    // Try to fetch Object by type and value.
    return objectFactDao.getObject(type, value);
  }
}
