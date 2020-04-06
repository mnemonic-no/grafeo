package no.mnemonic.act.platform.service.ti.tinkerpop;

import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
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
 * Base class for all exposed properties from an Object. Every subclass holds one property related to an Object.
 *
 * @param <V> Type of property value
 */
abstract class ObjectProperty<V> implements VertexProperty<V> {

  private final ObjectEntity object;
  private final ObjectVertex owner;
  private final UUID id;

  private ObjectProperty(ObjectEntity object, ObjectVertex owner) {
    this.object = ObjectUtils.notNull(object, "'object' is null!");
    this.owner = ObjectUtils.notNull(owner, "'owner' is null!");
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
  public String toString() {
    return StringFactory.propertyString(this);
  }

  protected ObjectEntity getObject() {
    // Need to expose 'object' to inner static classes.
    return object;
  }

  static class Value extends ObjectProperty<String> {
    Value(ObjectEntity object, ObjectVertex owner) {
      super(object, owner);
    }

    @Override
    public String key() {
      return "value";
    }

    @Override
    public String value() {
      return getObject().getValue();
    }
  }
}
