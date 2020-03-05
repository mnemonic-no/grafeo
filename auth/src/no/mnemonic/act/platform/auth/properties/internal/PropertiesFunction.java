package no.mnemonic.act.platform.auth.properties.internal;

import java.util.Objects;

/**
 * Internal representation of a single Function.
 */
public class PropertiesFunction {

  private final String name;

  PropertiesFunction(String name) {
    this.name = name;
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
    PropertiesFunction other = (PropertiesFunction) o;
    return Objects.equals(name, other.getName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  public static Builder builder() {
    return new Builder();
  }

  @SuppressWarnings("unchecked")
  public static class Builder<T extends Builder> {
    String name;

    public PropertiesFunction build() {
      return new PropertiesFunction(name);
    }

    public T setName(String name) {
      this.name = name;
      return (T) this;
    }
  }
}
