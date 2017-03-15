package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.model.v1.ObjectFactsStatistic;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.GetObjectByIdRequest;
import no.mnemonic.act.platform.api.request.v1.GetObjectByTypeValueRequest;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.act.platform.entity.cassandra.FactTypeEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.commons.utilities.collections.CollectionUtils;

import java.util.*;

public class ObjectGetDelegate extends AbstractDelegate {

  public static ObjectGetDelegate create() {
    return new ObjectGetDelegate();
  }

  public Object handle(GetObjectByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    TiSecurityContext.get().checkPermission(TiFunctionConstants.viewFactObjects);

    return handle(TiRequestContext.get().getObjectManager().getObject(request.getId()));
  }

  public Object handle(GetObjectByTypeValueRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    TiSecurityContext.get().checkPermission(TiFunctionConstants.viewFactObjects);
    assertObjectTypeExists(request.getType(), "type");

    return handle(TiRequestContext.get().getObjectManager().getObject(request.getType(), request.getValue()));
  }

  private Object handle(ObjectEntity object) throws AccessDeniedException {
    if (object == null) {
      // User should not get a different response if an Object is not in the system or if user does not have access to it.
      throw new AccessDeniedException("No access to Object.");
    }

    List<FactEntity> facts = resolveFactsForObject(object.getId());
    if (CollectionUtils.isEmpty(facts)) {
      // User does not have access to any Facts bound to this Object.
      throw new AccessDeniedException("No access to Object.");
    }

    return convertObject(object, calculateStatistics(facts));
  }

  private Map<UUID, Statistic> calculateStatistics(List<FactEntity> facts) {
    Map<UUID, Statistic> factStatistics = new HashMap<>();

    for (FactEntity fact : facts) {
      // Updated statistic for Fact.
      factStatistics.computeIfAbsent(fact.getTypeID(), k -> new Statistic())
              .incCount()
              .setLastAddedTimestamp(fact.getTimestamp())
              .setLastSeenTimestamp(fact.getLastSeenTimestamp());
    }

    return factStatistics;
  }

  private Object convertObject(ObjectEntity object, Map<UUID, Statistic> factStatistics) {
    ObjectTypeEntity objectTypeEntity = TiRequestContext.get().getObjectManager().getObjectType(object.getTypeID());
    ObjectType objectType = TiRequestContext.get().getObjectTypeConverter().apply(objectTypeEntity);

    List<ObjectFactsStatistic> statistics = new ArrayList<>();
    for (UUID factTypeID : factStatistics.keySet()) {
      FactTypeEntity factTypeEntity = TiRequestContext.get().getFactManager().getFactType(factTypeID);
      FactType factType = TiRequestContext.get().getFactTypeConverter().apply(factTypeEntity);
      ObjectFactsStatistic stat = ObjectFactsStatistic.builder()
              .setType(factType.toInfo())
              .setCount(factStatistics.get(factTypeID).getCount())
              .setLastAddedTimestamp(factStatistics.get(factTypeID).getLastAddedTimestamp())
              .setLastSeenTimestamp(factStatistics.get(factTypeID).getLastSeenTimestamp())
              .build();
      statistics.add(stat);
    }

    return Object.builder()
            .setId(object.getId())
            .setType(objectType.toInfo())
            .setValue(object.getValue())
            .setStatistics(statistics)
            .build();
  }

  /**
   * Simple helper class to store the statistics for one FactType.
   */
  private class Statistic {
    private int count;
    private long lastAddedTimestamp;
    private long lastSeenTimestamp;

    int getCount() {
      return count;
    }

    Statistic incCount() {
      this.count++;
      return this;
    }

    long getLastAddedTimestamp() {
      return lastAddedTimestamp;
    }

    Statistic setLastAddedTimestamp(long lastAddedTimestamp) {
      this.lastAddedTimestamp = Math.max(this.lastAddedTimestamp, lastAddedTimestamp);
      return this;
    }

    long getLastSeenTimestamp() {
      return lastSeenTimestamp;
    }

    Statistic setLastSeenTimestamp(long lastSeenTimestamp) {
      this.lastSeenTimestamp = Math.max(this.lastSeenTimestamp, lastSeenTimestamp);
      return this;
    }
  }

}
