package no.mnemonic.services.grafeo.dao.cassandra.entity;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import com.datastax.oss.driver.api.mapper.annotations.Transient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.unmodifiableMap;
import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;
import static no.mnemonic.commons.utilities.collections.MapUtils.map;
import static no.mnemonic.services.grafeo.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity.TABLE;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class FactTypeEntity implements CassandraEntity {

  public static final String TABLE = "fact_type";
  public static final float DEFAULT_CONFIDENCE = 1.0f;

  private static final ObjectMapper mapper = JsonMapper.builder().build();
  private static final ObjectReader factObjectBindingDefinitionReader = mapper.readerFor(mapper.getTypeFactory().constructCollectionType(Set.class, FactObjectBindingDefinition.class));
  private static final ObjectWriter factObjectBindingDefinitionWriter = mapper.writerFor(mapper.getTypeFactory().constructCollectionType(Set.class, FactObjectBindingDefinition.class));
  private static final ObjectReader metaFactBindingDefinitionReader = mapper.readerFor(mapper.getTypeFactory().constructCollectionType(Set.class, MetaFactBindingDefinition.class));
  private static final ObjectWriter metaFactBindingDefinitionWriter = mapper.writerFor(mapper.getTypeFactory().constructCollectionType(Set.class, MetaFactBindingDefinition.class));
  private static final Logger logger = Logging.getLogger(FactTypeEntity.class);

  public enum Flag implements CassandraEnum<Flag> {
    Deleted(0);

    private static final Map<Integer, Flag> enumValues = unmodifiableMap(map(v -> T(v.value(), v), values()));
    private final int value;

    Flag(int value) {
      this.value = value;
    }

    @Override
    public int value() {
      return value;
    }

    public static Map<Integer, Flag> getValueMap() {
      return enumValues;
    }
  }

  @PartitionKey
  private UUID id;
  @CqlName("namespace_id")
  private UUID namespaceID;
  private String name;
  private String validator;
  @CqlName("validator_parameter")
  private String validatorParameter;
  // In order to not create another table relevantObjectBindings are stored as a JSON string.
  @CqlName("relevant_object_bindings")
  private String relevantObjectBindingsStored;
  // But they are also available as objects.
  @Transient
  private Set<FactObjectBindingDefinition> relevantObjectBindings;
  // In order to not create another table relevantFactBindings are stored as a JSON string.
  @CqlName("relevant_fact_bindings")
  private String relevantFactBindingsStored;
  // But they are also available as objects.
  @Transient
  private Set<MetaFactBindingDefinition> relevantFactBindings;
  @CqlName("default_confidence")
  private Float defaultConfidence;
  private Set<Flag> flags;

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
      this.relevantObjectBindings = !StringUtils.isBlank(relevantObjectBindingsStored) ? factObjectBindingDefinitionReader.readValue(relevantObjectBindingsStored) : null;
    } catch (IOException ex) {
      logAndRethrow(ex, String.format("Could not read 'relevantObjectBindings' for FactType with name = %s.", getName()));
    }

    return this;
  }

  public Set<FactObjectBindingDefinition> getRelevantObjectBindings() {
    return relevantObjectBindings;
  }

  public FactTypeEntity setRelevantObjectBindings(Set<FactObjectBindingDefinition> relevantObjectBindings) {
    this.relevantObjectBindings = relevantObjectBindings;

    try {
      this.relevantObjectBindingsStored = !CollectionUtils.isEmpty(relevantObjectBindings) ? factObjectBindingDefinitionWriter.writeValueAsString(relevantObjectBindings) : null;
    } catch (IOException ex) {
      logAndRethrow(ex, String.format("Could not write 'relevantObjectBindings' for FactType with name = %s.", getName()));
    }

    return this;
  }

  public FactTypeEntity addRelevantObjectBinding(FactObjectBindingDefinition relevantObjectBinding) {
    // Need to call setRelevantObjectBindings() in order to store JSON blob.
    return setRelevantObjectBindings(SetUtils.addToSet(relevantObjectBindings, relevantObjectBinding));
  }

  public String getRelevantFactBindingsStored() {
    return relevantFactBindingsStored;
  }

  public FactTypeEntity setRelevantFactBindingsStored(String relevantFactBindingsStored) {
    this.relevantFactBindingsStored = relevantFactBindingsStored;

    try {
      this.relevantFactBindings = !StringUtils.isBlank(relevantFactBindingsStored) ? metaFactBindingDefinitionReader.readValue(relevantFactBindingsStored) : null;
    } catch (IOException ex) {
      logAndRethrow(ex, String.format("Could not read 'relevantFactBindings' for FactType with name = %s.", getName()));
    }

    return this;
  }

  public Set<MetaFactBindingDefinition> getRelevantFactBindings() {
    return relevantFactBindings;
  }

  public FactTypeEntity setRelevantFactBindings(Set<MetaFactBindingDefinition> relevantFactBindings) {
    this.relevantFactBindings = relevantFactBindings;

    try {
      this.relevantFactBindingsStored = !CollectionUtils.isEmpty(relevantFactBindings) ? metaFactBindingDefinitionWriter.writeValueAsString(relevantFactBindings) : null;
    } catch (IOException ex) {
      logAndRethrow(ex, String.format("Could not write 'relevantFactBindings' for FactType with name = %s.", getName()));
    }

    return this;
  }

  public FactTypeEntity addRelevantFactBinding(MetaFactBindingDefinition relevantFactBinding) {
    // Need to call setRelevantFactBindings() in order to store JSON blob.
    return setRelevantFactBindings(SetUtils.addToSet(relevantFactBindings, relevantFactBinding));
  }

  public Float getDefaultConfidence() {
    // Required for backwards-compatibility where the 'default_confidence' column is unset.
    return ObjectUtils.ifNull(defaultConfidence, DEFAULT_CONFIDENCE);
  }

  public FactTypeEntity setDefaultConfidence(Float defaultConfidence) {
    this.defaultConfidence = defaultConfidence;
    return this;
  }

  public Set<Flag> getFlags() {
    return flags;
  }

  public FactTypeEntity setFlags(Set<Flag> flags) {
    this.flags = flags;
    return this;
  }

  public FactTypeEntity addFlag(Flag flag) {
    this.flags = SetUtils.addToSet(this.flags, flag);
    return this;
  }

  public boolean isSet(Flag flag) {
    return SetUtils.set(flags).contains(flag);
  }

  private void logAndRethrow(IOException ex, String msg) {
    logger.error(ex, msg);
    throw new UncheckedIOException(msg, ex);
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

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      FactObjectBindingDefinition that = (FactObjectBindingDefinition) o;
      return bidirectionalBinding == that.bidirectionalBinding &&
              Objects.equals(sourceObjectTypeID, that.sourceObjectTypeID) &&
              Objects.equals(destinationObjectTypeID, that.destinationObjectTypeID);
    }

    @Override
    public int hashCode() {
      return Objects.hash(sourceObjectTypeID, destinationObjectTypeID, bidirectionalBinding);
    }
  }

  public static class MetaFactBindingDefinition {
    // Right now this class only contains one field, but storing it as a JSON blob makes
    // it easier to extend it in opposite to using a plain Cassandra set of UUIDs.
    private UUID factTypeID;

    public UUID getFactTypeID() {
      return factTypeID;
    }

    public MetaFactBindingDefinition setFactTypeID(UUID factTypeID) {
      this.factTypeID = factTypeID;
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      MetaFactBindingDefinition that = (MetaFactBindingDefinition) o;
      return Objects.equals(factTypeID, that.factTypeID);
    }

    @Override
    public int hashCode() {
      return Objects.hash(factTypeID);
    }
  }

}
