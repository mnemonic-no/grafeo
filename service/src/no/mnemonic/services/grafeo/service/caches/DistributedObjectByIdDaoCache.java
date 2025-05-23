package no.mnemonic.services.grafeo.service.caches;

import com.hazelcast.core.HazelcastInstance;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.service.providers.AbstractHazelcastMapProvider;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.UUID;

@Singleton
public class DistributedObjectByIdDaoCache extends AbstractHazelcastMapProvider<UUID, ObjectRecord> {

  public static final String OBJECT_BY_ID_MAP_NAME = "ACT.Service.Map.DaoCache.ObjectById";

  @Inject
  public DistributedObjectByIdDaoCache(HazelcastInstance hazelcastInstance) {
    super(hazelcastInstance, OBJECT_BY_ID_MAP_NAME);
  }
}
