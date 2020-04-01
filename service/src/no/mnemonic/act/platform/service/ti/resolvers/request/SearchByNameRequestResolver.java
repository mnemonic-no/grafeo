package no.mnemonic.act.platform.service.ti.resolvers.request;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolver implementing id-or-name logic when searching for entities.
 * <p>
 * When searching for related entities users can specify in the same field the id or name of an entity to search for.
 * If the field contains an id that id will be used for search directly. If it contains a name the corresponding entity
 * will be fetched by name before searching by its id.
 * <p>
 * Use this resolver to resolve the ids from an id-or-name search field before passing them to the search back-end.
 */
public class SearchByNameRequestResolver {

  private final FactManager factManager;
  private final ObjectManager objectManager;
  private final OriginManager originManager;
  private final OrganizationResolver organizationResolver;

  @Inject
  public SearchByNameRequestResolver(FactManager factManager,
                                     ObjectManager objectManager,
                                     OriginManager originManager,
                                     OrganizationResolver organizationResolver) {
    this.factManager = factManager;
    this.objectManager = objectManager;
    this.originManager = originManager;
    this.organizationResolver = organizationResolver;
  }

  /**
   * Resolve ids for FactTypes from an id-or-name search field.
   *
   * @param values Set of Strings
   * @return Set of UUIDs
   * @throws InvalidArgumentException If FactTypes for one or more names do not exist.
   */
  public Set<UUID> resolveFactType(Set<String> values) throws InvalidArgumentException {
    return resolve(values, name -> ObjectUtils.ifNotNull(factManager.getFactType(name), FactTypeEntity::getId), "factType");
  }

  /**
   * Resolve ids for ObjectTypes from an id-or-name search field.
   *
   * @param values Set of Strings
   * @return Set of UUIDs
   * @throws InvalidArgumentException If ObjectTypes for one or more names do not exist.
   */
  public Set<UUID> resolveObjectType(Set<String> values) throws InvalidArgumentException {
    return resolve(values, name -> ObjectUtils.ifNotNull(objectManager.getObjectType(name), ObjectTypeEntity::getId), "objectType");
  }

  /**
   * Resolve ids for Origins from an id-or-name search field.
   *
   * @param values Set of Strings
   * @return Set of UUIDs
   * @throws InvalidArgumentException If Origins for one or more names do not exist.
   */
  public Set<UUID> resolveOrigin(Set<String> values) throws InvalidArgumentException {
    return resolve(values, name -> ObjectUtils.ifNotNull(originManager.getOrigin(name), OriginEntity::getId), "origin");
  }

  /**
   * Resolve ids for Organizations from an id-or-name search field.
   *
   * @param values Set of Strings
   * @return Set of UUIDs
   * @throws InvalidArgumentException If Organizations for one or more names do not exist.
   */
  public Set<UUID> resolveOrganization(Set<String> values) throws InvalidArgumentException {
    return resolve(values, name -> ObjectUtils.ifNotNull(organizationResolver.resolveOrganization(name), Organization::getId), "organization");
  }

  private Set<UUID> resolve(Set<String> values, Function<String, UUID> byNameResolver, String property) throws InvalidArgumentException {
    if (CollectionUtils.isEmpty(values)) return Collections.emptySet();

    // Keep track of names which can't be resolved.
    Set<String> unresolvedNames = new HashSet<>();
    // Filter out values which are already an id.
    Set<UUID> id = onlyUUID(values);

    // For all other values try to resolve id by the entity's name.
    for (String name : noneUUID(values)) {
      UUID resolvedId = byNameResolver.apply(name);
      if (resolvedId != null) {
        id.add(resolvedId);
      } else {
        unresolvedNames.add(name);
      }
    }

    // If at least one entity couldn't be resolved throw an InvalidArgumentException.
    throwIfUnresolved(unresolvedNames, property);

    return id;
  }

  private void throwIfUnresolved(Set<String> unresolvedNames, String property) throws InvalidArgumentException {
    if (CollectionUtils.isEmpty(unresolvedNames)) return;

    InvalidArgumentException exception = new InvalidArgumentException();
    for (String name : unresolvedNames) {
      exception.addValidationError(String.format("Entity with name = %s does not exist.", name), "entity.not.exist", property, name);
    }

    throw exception;
  }

  private Set<UUID> onlyUUID(Set<String> values) {
    return values.stream()
            .filter(StringUtils::isUUID)
            .map(UUID::fromString)
            .collect(Collectors.toSet());
  }

  private Set<String> noneUUID(Set<String> values) {
    return values.stream()
            .filter(value -> !StringUtils.isUUID(value))
            .collect(Collectors.toSet());
  }
}
