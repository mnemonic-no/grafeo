package no.mnemonic.services.grafeo.service.caches;

import com.hazelcast.core.HazelcastInstance;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.service.providers.AbstractHazelcastMapProvider;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DistributedObjectByTypeValueDaoCache extends AbstractHazelcastMapProvider<String, ObjectRecord> {

  public static final String OBJECT_BY_TYPE_VALUE_MAP_NAME = "ACT.Service.Map.DaoCache.ObjectByTypeValue";

  @Inject
  public DistributedObjectByTypeValueDaoCache(HazelcastInstance hazelcastInstance) {
    super(hazelcastInstance, OBJECT_BY_TYPE_VALUE_MAP_NAME);
  }
}
