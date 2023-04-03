package no.mnemonic.services.grafeo.service.implementation.tinkerpop.utils;

/**
 * Represents a property in the form of a name that points to a value.
 *
 * @param <T> The type of value
 */
public class PropertyEntry<T> {

  private final String name;
  private final T value;
  private final long timestamp;

  public PropertyEntry(String name, T value) {
    this(name, value, -1);
  }

  public PropertyEntry(String name, T value, long timestamp) {
    this.name = name;
    this.value = value;
    this.timestamp = timestamp;
  }

  public String getName() {
    return name;
  }

  public T getValue() {
    return value;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
