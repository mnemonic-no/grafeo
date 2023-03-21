package no.mnemonic.services.grafeo.dao.elastic.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true) // Required for backwards compatibility.
public class ObjectDocument implements ElasticDocument {

  public enum Direction {
    FactIsSource, FactIsDestination, BiDirectional
  }

  // 'id' is indexed as an own field because Objects aren't index separately but as part of Facts.
  private UUID id;
  private UUID typeID;
  private String value;
  private Direction direction;

  public UUID getId() {
    return id;
  }

  public ObjectDocument setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getTypeID() {
    return typeID;
  }

  public ObjectDocument setTypeID(UUID typeID) {
    this.typeID = typeID;
    return this;
  }

  public String getValue() {
    return value;
  }

  public ObjectDocument setValue(String value) {
    this.value = value;
    return this;
  }

  public Direction getDirection() {
    return direction;
  }

  public ObjectDocument setDirection(Direction direction) {
    this.direction = direction;
    return this;
  }

}
