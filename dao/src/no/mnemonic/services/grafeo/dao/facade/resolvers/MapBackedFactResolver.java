package no.mnemonic.services.grafeo.dao.facade.resolvers;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.bindings.DaoCache;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactEntity;
import no.mnemonic.services.grafeo.dao.facade.converters.FactRecordConverter;

import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;

/**
 * {@link CachedFactResolver} implementation which is backed by a {@link Map}.
 */
public class MapBackedFactResolver implements CachedFactResolver {

  private final FactManager factManager;
  private final FactRecordConverter factRecordConverter;
  private final Map<UUID, FactRecord> factByIdCache;
  private final Map<String, UUID> factByHashCache;

  @Inject
  public MapBackedFactResolver(
          FactManager factManager,
          FactRecordConverter factRecordConverter,
          @DaoCache Map<UUID, FactRecord> factByIdCache,
          @DaoCache Map<String, UUID> factByHashCache) {
    this.factManager = factManager;
    this.factRecordConverter = factRecordConverter;
    this.factByIdCache = factByIdCache;
    this.factByHashCache = factByHashCache;
  }

  @Override
  public FactRecord getFact(UUID id) {
    if (id == null) return null;

    return factByIdCache.computeIfAbsent(id,
            key -> factRecordConverter.fromEntity(factManager.getFact(id)));
  }

  @Override
  public FactRecord getFact(String factHash) {
    if (StringUtils.isBlank(factHash)) return null;

    // Look up UUID from 'factByHashCache' and use the result to fetch the actual record from 'factByIdCache'.
    UUID id = factByHashCache.computeIfAbsent(factHash,
            key -> ObjectUtils.ifNotNull(factManager.getFact(factHash), FactEntity::getId));
    return getFact(id);
  }

  @Override
  public void evict(FactRecord fact) {
    if (fact == null || fact.getId() == null) return;
    factByIdCache.remove(fact.getId());
  }
}
