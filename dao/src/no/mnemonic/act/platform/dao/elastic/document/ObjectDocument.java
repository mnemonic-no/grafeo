package no.mnemonic.act.platform.dao.elastic.document;

import java.util.UUID;

public class ObjectDocument implements ElasticDocument {

  public enum Direction {
    None, FactIsSource, FactIsDestination, BiDirectional
  }

  // 'id' is indexed as an own field because Objects aren't index separately but as part of Facts.
  private UUID id;
  private UUID typeID;
  private String typeName;
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

  public String getTypeName() {
    return typeName;
  }

  public ObjectDocument setTypeName(String typeName) {
    this.typeName = typeName;
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
