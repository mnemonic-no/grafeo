package no.mnemonic.act.platform.service.ti.tinkerpop;

import no.mnemonic.commons.utilities.ObjectUtils;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import java.util.Iterator;
import java.util.UUID;

import static org.apache.tinkerpop.gremlin.structure.Property.Exceptions.propertyRemovalNotSupported;
import static org.apache.tinkerpop.gremlin.structure.VertexProperty.Exceptions.metaPropertiesNotSupported;

/**
 * Exposes Object properties as Vertex properties.
 *
 */
public class ObjectProperty implements VertexProperty<String> {

  private final String key;
  private final String value;

  private final ObjectVertex owner;
  private final UUID id;

  public ObjectProperty(ObjectVertex owner, String key, String value) {
    this.owner = ObjectUtils.notNull(owner, "'owner' is null!");
    this.key = key;
    this.value = value;
    this.id = UUID.randomUUID(); // Generate a random ID for each new instance.
  }

  @Override
  public boolean isPresent() {
    return true;
  }

  @Override
  public Vertex element() {
    return owner;
  }

  @Override
  public void remove() {
    throw propertyRemovalNotSupported();
  }

  @Override
  public Object id() {
    return id;
  }

  @Override
  public <U> Property<U> property(String key, U values) {
    throw metaPropertiesNotSupported();
  }

  @Override
  public <U> Iterator<Property<U>> properties(String... propertyKeys) {
    throw metaPropertiesNotSupported();
  }

  @Override
  public String key() {return key; }

  @Override
  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return StringFactory.propertyString(this);
  }
}
