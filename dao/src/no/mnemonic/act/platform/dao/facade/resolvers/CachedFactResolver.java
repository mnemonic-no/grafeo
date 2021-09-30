package no.mnemonic.act.platform.dao.facade.resolvers;

import no.mnemonic.act.platform.dao.api.record.FactRecord;

import java.util.UUID;

/**
 * Resolver which fetches {@link FactRecord}s and caches them for later retrieval.
 * <p>
 * The cache semantics (node local or distributed cache, TTL, etc.) are implementation-specific.
 */
public interface CachedFactResolver {

  /**
   * Retrieve a {@link FactRecord} by its UUID. Returns null if the record does not exist in the database.
   *
   * @param id UUID of Fact
   * @return Resolved Fact or null
   */
  FactRecord getFact(UUID id);

  /**
   * Retrieve a {@link FactRecord} by its hash value. Returns null if the record does not exist in the database.
   *
   * @param factHash Hash of Fact
   * @return Resolved Fact or null
   */
  FactRecord getFact(String factHash);

  /**
   * Evict a previously cached {@link FactRecord} from the cache.
   *
   * @param fact Fact to evict from cache
   */
  void evict(FactRecord fact);

}
