package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.OperationTimeoutException;
import no.mnemonic.services.grafeo.api.request.v1.TraverseGraphByObjectsRequest;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.handlers.ObjectTypeHandler;
import no.mnemonic.services.grafeo.service.implementation.handlers.TraverseGraphHandler;
import no.mnemonic.services.grafeo.service.implementation.resolvers.AccessControlCriteriaResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.IndexSelectCriteriaResolver;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.TraverseParams;

import jakarta.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static no.mnemonic.services.grafeo.service.implementation.converters.request.RequestConverterUtils.handleTimeFieldSearchRequest;

public class TraverseByObjectsDelegate implements Delegate {

  private static final Pattern TYPE_VALUE_PATTERN = Pattern.compile("([^/]+)/(.+)");

  private final GrafeoSecurityContext securityContext;
  private final TraverseGraphHandler traverseGraphHandler;
  private final ObjectFactDao objectFactDao;
  private final ObjectTypeHandler objectTypeHandler;
  private final AccessControlCriteriaResolver accessControlCriteriaResolver;
  private final IndexSelectCriteriaResolver indexSelectCriteriaResolver;

  @Inject
  public TraverseByObjectsDelegate(GrafeoSecurityContext securityContext,
                                   TraverseGraphHandler traverseGraphHandler,
                                   ObjectFactDao objectFactDao,
                                   ObjectTypeHandler objectTypeHandler,
                                   AccessControlCriteriaResolver accessControlCriteriaResolver,
                                   IndexSelectCriteriaResolver indexSelectCriteriaResolver) {
    this.securityContext = securityContext;
    this.traverseGraphHandler = traverseGraphHandler;
    this.objectFactDao = objectFactDao;
    this.objectTypeHandler = objectTypeHandler;
    this.accessControlCriteriaResolver = accessControlCriteriaResolver;
    this.indexSelectCriteriaResolver = indexSelectCriteriaResolver;
  }

  public ResultSet<?> handle(TraverseGraphByObjectsRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    securityContext.checkPermission(FunctionConstants.traverseGrafeoFact);

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
                    .setBaseSearchCriteria(handleTimeFieldSearchRequest(FactSearchCriteria.builder(), request)
                            .setAccessControlCriteria(accessControlCriteriaResolver.get())
                            .setIndexSelectCriteria(indexSelectCriteriaResolver.validateAndCreateCriteria(request.getStartTimestamp(), request.getEndTimestamp()))
                            .build())
                    .setIncludeRetracted(request.getIncludeRetracted())
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
