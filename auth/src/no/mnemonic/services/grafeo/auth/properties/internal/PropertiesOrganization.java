package no.mnemonic.services.grafeo.auth.properties.internal;

import java.util.Objects;

/**
 * Internal representation of a single Organization.
 */
public class PropertiesOrganization {

  private final long internalID;
  private final String name;

  PropertiesOrganization(long internalID, String name) {
    this.internalID = internalID;
    this.name = name;
  }

  public long getInternalID() {
    return internalID;
  }

  public String getName() {
    return name;
  }

  public boolean isGroup() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PropertiesOrganization other = (PropertiesOrganization) o;
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

    public PropertiesOrganization build() {
      return new PropertiesOrganization(internalID, name);
    }

    public T setInternalID(long internalID) {
      this.internalID = internalID;
      return (T) this;
    }

    public T setName(String name) {
      this.name = name;
      return (T) this;
    }
  }
}
