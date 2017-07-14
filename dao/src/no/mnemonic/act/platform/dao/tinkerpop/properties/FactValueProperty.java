package no.mnemonic.act.platform.dao.tinkerpop.properties;

import no.mnemonic.act.platform.dao.tinkerpop.FactEdge;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.commons.utilities.ObjectUtils;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;

import java.util.NoSuchElementException;

import static org.apache.tinkerpop.gremlin.structure.Property.Exceptions.propertyRemovalNotSupported;

/**
 * Property class holding a Fact's value.
 */
public class FactValueProperty implements Property<String> {

  private final FactEntity fact;
  private final FactEdge owner;

  public FactValueProperty(FactEntity fact, FactEdge owner) {
    this.fact = ObjectUtils.notNull(fact, "'fact' is null!");
    this.owner = ObjectUtils.notNull(owner, "'owner' is null!");
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
    throw propertyRemovalNotSupported();
  }

}
