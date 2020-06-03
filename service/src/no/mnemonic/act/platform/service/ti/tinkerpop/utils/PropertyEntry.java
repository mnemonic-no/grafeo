package no.mnemonic.act.platform.service.ti.tinkerpop.utils;

/**
 * Represents a property in the form of a name that points to a value.
 *
 * @param <T> The type of value
 */
public class PropertyEntry<T> {

  private final String name;
  private final T value;

  public PropertyEntry(String name, T value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public T getValue() {
    return value;
  }
}
