package no.mnemonic.services.grafeo.service.ti.tinkerpop;

import no.mnemonic.commons.utilities.ObjectUtils;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import java.util.NoSuchElementException;
import java.util.Objects;

import static org.apache.tinkerpop.gremlin.structure.Property.Exceptions.propertyRemovalNotSupported;

/**
 * Class for exposed properties of a Fact.
 *
 * @param <V> Type of property value
 */
class FactProperty<V> implements Property<V> {
  private final String key;
  private final V value;

  private final FactEdge owner;

  public FactProperty(FactEdge owner, String key, V value) {
    this.owner = ObjectUtils.notNull(owner, "'owner' is null!");
    this.key = ObjectUtils.notNull(key, "'key' is null!");
    this.value = value;
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public V value() throws NoSuchElementException {
    return value;
  }

  @Override
  public boolean isPresent() {
    return true;
  }

  @Override
  public Element element() {
    return owner;
  }

  @Override
  public void remove() {
    throw propertyRemovalNotSupported();
  }

  @Override
  public String toString() {
    return StringFactory.propertyString(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FactProperty<?> that = (FactProperty<?>) o;
    return key.equals(that.key) &&
            Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }
}
