package no.mnemonic.act.platform.dao.facade.resolvers;

import no.mnemonic.act.platform.dao.api.record.ObjectRecord;

import java.util.UUID;

/**
 * Resolver which fetches {@link ObjectRecord}s and caches them for later retrieval.
 * <p>
 * The cache semantics (node local or distributed cache, TTL, etc.) are implementation-specific.
 */
public interface CachedObjectResolver {

  /**
   * Retrieve an {@link ObjectRecord} by its UUID. Returns null if the record does not exist in the database.
   *
   * @param id UUID of Object
   * @return Resolved Object or null
   */
  ObjectRecord getObject(UUID id);

  /**
   * Retrieve an {@link ObjectRecord} by its type and value. Returns null if the record does not exist in the database.
   *
   * @param type  Name of ObjectType
   * @param value Value of Object
   * @return Resolved Object or null
   */
  ObjectRecord getObject(String type, String value);

}
