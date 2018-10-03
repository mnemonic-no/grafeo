package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.*;
import static no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity.TABLE;

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

  public String getRelevantObjectBindingsStored() {
    return relevantObjectBindingsStored;
  }

  public FactTypeEntity setRelevantObjectBindingsStored(String relevantObjectBindingsStored) {
    this.relevantObjectBindingsStored = relevantObjectBindingsStored;

    try {
      this.relevantObjectBindings = !StringUtils.isBlank(relevantObjectBindingsStored) ? reader.readValue(relevantObjectBindingsStored) : null;
    } catch (IOException e) {
      throw new RuntimeException(String.format("Could not read 'relevantObjectBindings' for FactType with name = %s.", getName()));
    }

    return this;
  }

  public List<FactObjectBindingDefinition> getRelevantObjectBindings() {
    return relevantObjectBindings;
  }

  public FactTypeEntity setRelevantObjectBindings(List<FactObjectBindingDefinition> relevantObjectBindings) {
    this.relevantObjectBindings = relevantObjectBindings;

    try {
      this.relevantObjectBindingsStored = !CollectionUtils.isEmpty(relevantObjectBindings) ? writer.writeValueAsString(relevantObjectBindings) : null;
    } catch (IOException e) {
      throw new RuntimeException(String.format("Could not write 'relevantObjectBindings' for FactType with name = %s.", getName()));
    }

    return this;
  }

  public static class FactObjectBindingDefinition {

    private UUID sourceObjectTypeID;
    private UUID destinationObjectTypeID;
    private boolean bidirectionalBinding;

    public UUID getSourceObjectTypeID() {
      return sourceObjectTypeID;
    }

    public FactObjectBindingDefinition setSourceObjectTypeID(UUID sourceObjectTypeID) {
      this.sourceObjectTypeID = sourceObjectTypeID;
      return this;
    }

    public UUID getDestinationObjectTypeID() {
      return destinationObjectTypeID;
    }

    public FactObjectBindingDefinition setDestinationObjectTypeID(UUID destinationObjectTypeID) {
      this.destinationObjectTypeID = destinationObjectTypeID;
      return this;
    }

    public boolean isBidirectionalBinding() {
      return bidirectionalBinding;
    }

    public FactObjectBindingDefinition setBidirectionalBinding(boolean bidirectionalBinding) {
      this.bidirectionalBinding = bidirectionalBinding;
      return this;
    }
  }

}
