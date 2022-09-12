package no.mnemonic.act.platform.service.ti.caches;

import com.hazelcast.core.HazelcastInstance;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.service.providers.AbstractHazelcastMapProvider;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

@Singleton
public class DistributedObjectByIdDaoCache extends AbstractHazelcastMapProvider<UUID, ObjectRecord> {

  public static final String OBJECT_BY_ID_MAP_NAME = "ACT.Service.Map.DaoCache.ObjectById";

  @Inject
  public DistributedObjectByIdDaoCache(HazelcastInstance hazelcastInstance) {
    super(hazelcastInstance, OBJECT_BY_ID_MAP_NAME);
  }
}
