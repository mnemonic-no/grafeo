package no.mnemonic.services.grafeo.service.caches;

import com.hazelcast.core.HazelcastInstance;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.service.providers.AbstractHazelcastMapProvider;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.UUID;

@Singleton
public class DistributedFactByIdDaoCache extends AbstractHazelcastMapProvider<UUID, FactRecord> {

  public static final String FACT_BY_ID_MAP_NAME = "ACT.Service.Map.DaoCache.FactById";

  @Inject
  public DistributedFactByIdDaoCache(HazelcastInstance hazelcastInstance) {
    super(hazelcastInstance, FACT_BY_ID_MAP_NAME);
  }
}
