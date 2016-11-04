package no.mnemonic.act.platform.dao.tinkerpop.properties;

import no.mnemonic.act.platform.dao.tinkerpop.ObjectVertex;
import no.mnemonic.act.platform.entity.cassandra.ObjectEntity;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.apache.tinkerpop.gremlin.structure.VertexProperty.Exceptions.metaPropertiesNotSupported;
import static org.apache.tinkerpop.gremlin.structure.VertexProperty.Exceptions.userSuppliedIdsNotSupported;

public class ObjectValueProperty implements VertexProperty<String> {

  private final ObjectEntity object;
  private final ObjectVertex owner;

  public ObjectValueProperty(ObjectEntity object, ObjectVertex owner) {
    this.object = object;
    this.owner = owner;
  }

  @Override
  public String key() {
    return "value";
  }

  @Override
  public String value() throws NoSuchElementException {
    return object.getValue();
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
    throw new UnsupportedOperationException("Removing properties not supported");
  }

  @Override
  public Object id() {
    throw userSuppliedIdsNotSupported();
  }

  @Override
  public <V> Property<V> property(String s, V v) {
    throw metaPropertiesNotSupported();
  }

  @Override
  public <U> Iterator<Property<U>> properties(String... strings) {
    throw metaPropertiesNotSupported();
  }

}
