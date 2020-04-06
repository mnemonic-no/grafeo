package no.mnemonic.act.platform.service.ti.tinkerpop;

import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.commons.utilities.ObjectUtils;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
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

  static class FactID extends FactProperty<String> {
    FactID(FactEntity fact, FactEdge owner) {
      super(fact, owner);
    }

    @Override
    public String key() {
      return "factID";
    }

    @Override
    public String value() {
      return ObjectUtils.ifNotNull(getFact().getId(), Objects::toString);
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
    public String value() {
      return getFact().getValue();
    }
  }

  static class InReferenceToID extends FactProperty<String> {
    InReferenceToID(FactEntity fact, FactEdge owner) {
      super(fact, owner);
    }

    @Override
    public String key() {
      return "inReferenceToID";
    }

    @Override
    public String value() {
      return ObjectUtils.ifNotNull(getFact().getInReferenceToID(), UUID::toString);
    }
  }

  static class OrganizationID extends FactProperty<String> {
    OrganizationID(FactEntity fact, FactEdge owner) {
      super(fact, owner);
    }

    @Override
    public String key() {
      return "organizationID";
    }

    @Override
    public String value() {
      return ObjectUtils.ifNotNull(getFact().getOrganizationID(), UUID::toString);
    }
  }

  static class OriginID extends FactProperty<String> {
    OriginID(FactEntity fact, FactEdge owner) {
      super(fact, owner);
    }

    @Override
    public String key() {
      return "originID";
    }

    @Override
    public String value() {
      return ObjectUtils.ifNotNull(getFact().getOriginID(), UUID::toString);
    }
  }

  static class Trust extends FactProperty<Float> {
    Trust(FactEntity fact, FactEdge owner) {
      super(fact, owner);
    }

    @Override
    public String key() {
      return "trust";
    }

    @Override
    public Float value() {
      return getFact().getTrust();
    }
  }

  static class Confidence extends FactProperty<Float> {
    Confidence(FactEntity fact, FactEdge owner) {
      super(fact, owner);
    }

    @Override
    public String key() {
      return "confidence";
    }

    @Override
    public Float value() {
      return getFact().getConfidence();
    }
  }

  static class Certainty extends FactProperty<Float> {
    Certainty(FactEntity fact, FactEdge owner) {
      super(fact, owner);
    }

    @Override
    public String key() {
      return "certainty";
    }

    @Override
    public Float value() {
      float certainty = getFact().getTrust() * getFact().getConfidence();
      // Round 'certainty' to two decimal points.
      return BigDecimal.valueOf(certainty)
              .setScale(2, RoundingMode.HALF_UP)
              .floatValue();
    }
  }

  static class AccessMode extends FactProperty<String> {
    AccessMode(FactEntity fact, FactEdge owner) {
      super(fact, owner);
    }

    @Override
    public String key() {
      return "accessMode";
    }

    @Override
    public String value() {
      return ObjectUtils.ifNotNull(getFact().getAccessMode(), Enum::name);
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
    public Long value() {
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
    public Long value() {
      return getFact().getLastSeenTimestamp();
    }
  }
}
