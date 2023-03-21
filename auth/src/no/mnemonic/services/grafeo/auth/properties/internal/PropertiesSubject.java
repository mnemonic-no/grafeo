package no.mnemonic.services.grafeo.auth.properties.internal;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.MapUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Internal representation of a single Subject which holds the permissions this Subject has to Organizations.
 */
public class PropertiesSubject {

  private final long internalID;
  private final String name;
  // Holds internalID of an organization.
  private final long affiliation;
  // Key is internalID of organization and value is a set of function/function group names.
  private final Map<Long, Set<String>> permissions;

  PropertiesSubject(long internalID, String name, long affiliation, Map<Long, Set<String>> permissions) {
    this.internalID = internalID;
    this.name = name;
    this.affiliation = affiliation;
    this.permissions = ObjectUtils.ifNotNull(permissions, Collections::unmodifiableMap, Collections.emptyMap());
  }

  public long getInternalID() {
    return internalID;
  }

  public String getName() {
    return name;
  }

  public long getAffiliation() {
    return affiliation;
  }

  public Map<Long, Set<String>> getPermissions() {
    return permissions;
  }

  public boolean isGroup() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PropertiesSubject other = (PropertiesSubject) o;
    return internalID == other.getInternalID();
  }

  @Override
  public int hashCode() {
    return Objects.hash(internalID);
  }

  public static Builder builder() {
    return new Builder();
  }

  @SuppressWarnings("unchecked")
  public static class Builder<T extends Builder> {
    long internalID;
    String name;
    long affiliation;
    Map<Long, Set<String>> permissions;

    public PropertiesSubject build() {
      return new PropertiesSubject(internalID, name, affiliation, permissions);
    }

    public T setInternalID(long internalID) {
      this.internalID = internalID;
      return (T) this;
    }

    public T setName(String name) {
      this.name = name;
      return (T) this;
    }

    public T setAffiliation(long affiliation) {
      this.affiliation = affiliation;
      return (T) this;
    }

    public T setPermissions(Map<Long, Set<String>> permissions) {
      this.permissions = permissions;
      return (T) this;
    }

    public T addPermission(long organization, Set<String> functions) {
      this.permissions = MapUtils.addToMap(this.permissions, organization, functions);
      return (T) this;
    }
  }
}
