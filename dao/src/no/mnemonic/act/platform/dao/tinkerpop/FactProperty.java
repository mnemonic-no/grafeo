package no.mnemonic.act.platform.dao.tinkerpop;

import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.commons.utilities.ObjectUtils;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.apache.tinkerpop.gremlin.structure.Property.Exceptions.propertyRemovalNotSupported;

/**
 * Base class for all exposed properties from a Fact. Every subclass holds one property related to a Fact.
 *
 * @param <V> Type of property value
 */
abstract class FactProperty<V> implements Property<V> {

  private final FactEntity fact;
  private final FactEdge owner;

  private FactProperty(FactEntity fact, FactEdge owner) {
    this.fact = ObjectUtils.notNull(fact, "'fact' is null!");
    this.owner = ObjectUtils.notNull(owner, "'owner' is null!");
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

  protected FactEntity getFact() {
    // Need to expose 'fact' to inner static classes.
    return fact;
  }

  static class FactID extends FactProperty<UUID> {
    FactID(FactEntity fact, FactEdge owner) {
      super(fact, owner);
    }

    @Override
    public String key() {
      return "factID";
    }

    @Override
    public UUID value() throws NoSuchElementException {
      return getFact().getId();
    }
  }

  static class Value extends FactProperty<String> {
    Value(FactEntity fact, FactEdge owner) {
      super(fact, owner);
    }

    @Override
    public String key() {
      return "value";
    }

    @Override
    public String value() throws NoSuchElementException {
      return getFact().getValue();
    }
  }

  static class InReferenceToID extends FactProperty<UUID> {
    InReferenceToID(FactEntity fact, FactEdge owner) {
      super(fact, owner);
    }

    @Override
    public String key() {
      return "inReferenceToID";
    }

    @Override
    public UUID value() throws NoSuchElementException {
      return getFact().getInReferenceToID();
    }
  }

  static class OrganizationID extends FactProperty<UUID> {
    OrganizationID(FactEntity fact, FactEdge owner) {
      super(fact, owner);
    }

    @Override
    public String key() {
      return "organizationID";
    }

    @Override
    public UUID value() throws NoSuchElementException {
      return getFact().getOrganizationID();
    }
  }

  static class SourceID extends FactProperty<UUID> {
    SourceID(FactEntity fact, FactEdge owner) {
      super(fact, owner);
    }

    @Override
    public String key() {
      return "sourceID";
    }

    @Override
    public UUID value() throws NoSuchElementException {
      return getFact().getSourceID();
    }
  }

  static class AccessMode extends FactProperty<no.mnemonic.act.platform.entity.cassandra.AccessMode> {
    AccessMode(FactEntity fact, FactEdge owner) {
      super(fact, owner);
    }

    @Override
    public String key() {
      return "accessMode";
    }

    @Override
    public no.mnemonic.act.platform.entity.cassandra.AccessMode value() throws NoSuchElementException {
      return getFact().getAccessMode();
    }
  }

  static class Timestamp extends FactProperty<Long> {
    Timestamp(FactEntity fact, FactEdge owner) {
      super(fact, owner);
    }

    @Override
    public String key() {
      return "timestamp";
    }

    @Override
    public Long value() throws NoSuchElementException {
      return getFact().getTimestamp();
    }
  }

  static class LastSeenTimestamp extends FactProperty<Long> {
    LastSeenTimestamp(FactEntity fact, FactEdge owner) {
      super(fact, owner);
    }

    @Override
    public String key() {
      return "lastSeenTimestamp";
    }

    @Override
    public Long value() throws NoSuchElementException {
      return getFact().getLastSeenTimestamp();
    }
  }
}
