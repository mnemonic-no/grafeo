package no.mnemonic.act.platform.entity.cassandra;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static no.mnemonic.act.platform.entity.cassandra.CassandraEntity.*;
import static no.mnemonic.act.platform.entity.cassandra.FactTypeEntity.TABLE;

@Table(
        keyspace = KEY_SPACE,
        name = TABLE,
        readConsistency = READ_CONSISTENCY,
        writeConsistency = WRITE_CONSISTENCY
)
public class FactTypeEntity implements CassandraEntity {

  public static final String TABLE = "fact_type";

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final ObjectReader reader = mapper.readerFor(mapper.getTypeFactory().constructCollectionType(List.class, FactObjectBindingDefinition.class));
  private static final ObjectWriter writer = mapper.writerFor(mapper.getTypeFactory().constructCollectionType(List.class, FactObjectBindingDefinition.class));

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
  // In order to not create another table relevantObjectBindings are stored as a JSON string.
  @Column(name = "relevant_object_bindings")
  private String relevantObjectBindingsStored;
  // But they are also available as objects.
  @Transient
  private List<FactObjectBindingDefinition> relevantObjectBindings;

  public UUID getId() {
    return id;
  }

  public FactTypeEntity setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getNamespaceID() {
    return namespaceID;
  }

  public FactTypeEntity setNamespaceID(UUID namespaceID) {
    this.namespaceID = namespaceID;
    return this;
  }

  public String getName() {
    return name;
  }

  public FactTypeEntity setName(String name) {
    this.name = name;
    return this;
  }

  public String getValidator() {
    return validator;
  }

  public FactTypeEntity setValidator(String validator) {
    this.validator = validator;
    return this;
  }

  public String getValidatorParameter() {
    return validatorParameter;
  }

  public FactTypeEntity setValidatorParameter(String validatorParameter) {
    this.validatorParameter = validatorParameter;
    return this;
  }

  public String getEntityHandler() {
    return entityHandler;
  }

  public FactTypeEntity setEntityHandler(String entityHandler) {
    this.entityHandler = entityHandler;
    return this;
  }

  public String getEntityHandlerParameter() {
    return entityHandlerParameter;
  }

  public FactTypeEntity setEntityHandlerParameter(String entityHandlerParameter) {
    this.entityHandlerParameter = entityHandlerParameter;
    return this;
  }

  public String getRelevantObjectBindingsStored() {
    return relevantObjectBindingsStored;
  }

  public FactTypeEntity setRelevantObjectBindingsStored(String relevantObjectBindingsStored) throws IOException {
    this.relevantObjectBindingsStored = relevantObjectBindingsStored;
    this.relevantObjectBindings = reader.readValue(relevantObjectBindingsStored);
    return this;
  }

  public List<FactObjectBindingDefinition> getRelevantObjectBindings() {
    return relevantObjectBindings;
  }

  public FactTypeEntity setRelevantObjectBindings(List<FactObjectBindingDefinition> relevantObjectBindings) throws IOException {
    this.relevantObjectBindings = relevantObjectBindings;
    this.relevantObjectBindingsStored = writer.writeValueAsString(relevantObjectBindings);
    return this;
  }

  public static class FactObjectBindingDefinition {

    private UUID objectTypeID;
    @JsonIgnore
    private Direction direction;

    public UUID getObjectTypeID() {
      return objectTypeID;
    }

    public FactObjectBindingDefinition setObjectTypeID(UUID objectTypeID) {
      this.objectTypeID = objectTypeID;
      return this;
    }

    public Direction getDirection() {
      return direction;
    }

    public FactObjectBindingDefinition setDirection(Direction direction) {
      this.direction = direction;
      return this;
    }

    @JsonGetter("direction")
    public int getDirectionValue() {
      return direction != null ? direction.value() : 0;
    }

    @JsonSetter("direction")
    public FactObjectBindingDefinition setDirectionValue(int value) {
      this.direction = Direction.getValueMap().getOrDefault(value, Direction.None);
      return this;
    }
  }

}
