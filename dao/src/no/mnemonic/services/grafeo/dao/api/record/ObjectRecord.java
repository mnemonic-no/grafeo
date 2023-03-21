package no.mnemonic.services.grafeo.dao.api.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Record representing an Object.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectRecord {

  private UUID id;
  private UUID typeID;
  private String value;

  public UUID getId() {
    return id;
  }

  public ObjectRecord setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getTypeID() {
    return typeID;
  }

  public ObjectRecord setTypeID(UUID typeID) {
    this.typeID = typeID;
    return this;
  }

  public String getValue() {
    return value;
  }

  public ObjectRecord setValue(String value) {
    this.value = value;
    return this;
  }
}
