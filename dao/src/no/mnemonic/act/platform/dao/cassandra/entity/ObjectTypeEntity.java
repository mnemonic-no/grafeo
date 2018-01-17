package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.*;
import static no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity.TABLE;

@Table(
        keyspace = KEY_SPACE,
        name = TABLE,
        readConsistency = READ_CONSISTENCY,
        writeConsistency = WRITE_CONSISTENCY
)
public class ObjectTypeEntity implements CassandraEntity {

  public static final String TABLE = "object_type";

  @PartitionKey
  private UUID id;
  @Column(name = "namespace_id")
  private UUID namespaceID;
  private String name;
  private String validator;
  @Column(name = "validator_parameter")
  private String validatorParameter;
  @Column(name = "entity_handler")
  private String entityHandler;
  @Column(name = "entity_handler_parameter")
  private String entityHandlerParameter;

  public UUID getId() {
    return id;
  }

  public ObjectTypeEntity setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getNamespaceID() {
    return namespaceID;
  }

  public ObjectTypeEntity setNamespaceID(UUID namespaceID) {
    this.namespaceID = namespaceID;
    return this;
  }

  public String getName() {
    return name;
  }

  public ObjectTypeEntity setName(String name) {
    this.name = name;
    return this;
  }

  public String getValidator() {
    return validator;
  }

  public ObjectTypeEntity setValidator(String validator) {
    this.validator = validator;
    return this;
  }

  public String getValidatorParameter() {
    return validatorParameter;
  }

  public ObjectTypeEntity setValidatorParameter(String validatorParameter) {
    this.validatorParameter = validatorParameter;
    return this;
  }

  public String getEntityHandler() {
    return entityHandler;
  }

  public ObjectTypeEntity setEntityHandler(String entityHandler) {
    this.entityHandler = entityHandler;
    return this;
  }

  public String getEntityHandlerParameter() {
    return entityHandlerParameter;
  }

  public ObjectTypeEntity setEntityHandlerParameter(String entityHandlerParameter) {
    this.entityHandlerParameter = entityHandlerParameter;
    return this;
  }

}
