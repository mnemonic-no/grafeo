package no.mnemonic.act.platform.dao.tinkerpop.properties;

import no.mnemonic.act.platform.dao.tinkerpop.FactEdge;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;

import java.util.NoSuchElementException;

public class FactValueProperty implements Property<String> {

  private final FactEntity fact;
  private final FactEdge owner;

  public FactValueProperty(FactEntity fact, FactEdge owner) {
    this.fact = fact;
    this.owner = owner;
  }

  @Override
  public String key() {
    return "value";
  }

  @Override
  public String value() throws NoSuchElementException {
    return fact.getValue();
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
    throw new UnsupportedOperationException("Removing properties not supported");
  }

}
