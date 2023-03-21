package no.mnemonic.services.grafeo.service.ti.tinkerpop.utils;

import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;

import javax.inject.Inject;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves Object and Fact types. Acts as a level of indirection to avoid coupling the
 * Tinkerpop to FactManager and ObjectManager directly.
 * <p>
 * Also holds data classes for ObjectType and FactType records
 */
public class ObjectFactTypeResolver {

  private final FactManager factManager;
  private final ObjectManager objectManager;

  @Inject
  public ObjectFactTypeResolver(FactManager factManager, ObjectManager objectManager) {
    this.factManager = factManager;
    this.objectManager = objectManager;
  }

  /**
   * Resolves the id of a fact type given its name
   *
   * @param type The name of the fact type
   * @return The id of the fact type
   */
  public UUID factTypeNameToId(String type) {
    FactTypeEntity entity = factManager.getFactType(type);
    if (entity == null) return null;
    return entity.getId();
  }

  /**
   * Resolves the ids of fact types given their names
   *
   * @param types Set of fact type names
   * @return Set of fact type ids
   */
  public Set<UUID> factTypeNamesToIds(Set<String> types) {
    return SetUtils.set(types, this::factTypeNameToId);
  }

  /**
   * Fetch an ObjectTypeStruct by ID
   *
   * @param id The object type ID
   * @return Existing ObjectTypeStruct
   */
  public ObjectTypeStruct toObjectTypeStruct(UUID id) {
    ObjectTypeEntity entity = objectManager.getObjectType(id);
    if (entity == null) {
      return null;
    }
    return ObjectTypeStruct.builder().setId(entity.getId()).setName(entity.getName()).build();
  }

  /**
   * Fetch a FactTypeStruct by ID
   *
   * @param id The fact type ID
   * @return Existing FactTypeStruct
   */
  public FactTypeStruct toFactTypeStruct(UUID id) {
    FactTypeEntity entity = factManager.getFactType(id);
    if (entity == null) {
      return null;
    }
    return FactTypeStruct.builder().setId(entity.getId()).setName(entity.getName()).build();
  }

  public static class ObjectTypeStruct {
    private final UUID id;
    private final String name;

    private ObjectTypeStruct(UUID id, String name) {
      this.id = id;
      this.name = name;
    }

    public UUID getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ObjectTypeStruct that = (ObjectTypeStruct) o;
      return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, name);
    }

    public static Builder builder () {
      return new Builder();
    }

    public static class Builder {
      private UUID id;
      private String name;

      private Builder() {}

      public ObjectTypeStruct build() {
        return new ObjectTypeStruct(id, name);
      }

      public Builder setId(UUID id) {
        this.id = id;
        return this;
      }

      public Builder setName(String name) {
        this.name = name;
        return this;
      }
    }
  }

  public static class FactTypeStruct {
    private final UUID id;
    private final String name;

    private FactTypeStruct(UUID id, String name) {
      this.id = id;
      this.name = name;
    }

    public UUID getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      FactTypeStruct that = (FactTypeStruct) o;
      return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, name);
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private UUID id;
      private String name;

      private Builder() {}

      public FactTypeStruct build() {
        return new FactTypeStruct(id, name);
      }

      public Builder setId(UUID id) {
        this.id = id;
        return this;
      }

      public Builder setName(String name) {
        this.name = name;
        return this;
      }
    }
  }
}
