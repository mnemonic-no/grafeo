package no.mnemonic.act.platform.service.ti.caches;

import com.hazelcast.core.HazelcastInstance;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.service.providers.AbstractHazelcastMapProvider;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

@Singleton
public class DistributedFactByIdDaoCache extends AbstractHazelcastMapProvider<UUID, FactRecord> {

  public static final String FACT_BY_ID_MAP_NAME = "ACT.Service.Map.DaoCache.FactById";

  @Inject
  public DistributedFactByIdDaoCache(HazelcastInstance hazelcastInstance) {
    super(hazelcastInstance, FACT_BY_ID_MAP_NAME);
  }
}
