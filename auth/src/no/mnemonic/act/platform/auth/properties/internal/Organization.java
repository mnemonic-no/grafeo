package no.mnemonic.act.platform.auth.properties.internal;

import java.util.Objects;

/**
 * Internal representation of a single Organization.
 */
public class Organization {

  private final long internalID;
  private final String name;

  Organization(long internalID, String name) {
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
    Organization other = (Organization) o;
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

    public Organization build() {
      return new Organization(internalID, name);
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
