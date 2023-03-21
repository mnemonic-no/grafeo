package no.mnemonic.services.grafeo.service.ti.caches;

import com.hazelcast.core.HazelcastInstance;
import no.mnemonic.services.grafeo.service.providers.AbstractHazelcastMapProvider;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

@Singleton
public class DistributedFactByHashDaoCache extends AbstractHazelcastMapProvider<String, UUID> {

  public static final String FACT_BY_HASH_MAP_NAME = "ACT.Service.Map.DaoCache.FactByHash";

  @Inject
  public DistributedFactByHashDaoCache(HazelcastInstance hazelcastInstance) {
    super(hazelcastInstance, FACT_BY_HASH_MAP_NAME);
  }
}
