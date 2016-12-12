package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import no.mnemonic.act.platform.api.json.TimestampSerializer;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Fact {

  private final UUID id;
  private final FactType.Info type;
  private final String value;
  private final Fact.Info inReferenceTo;
  private final Organization.Info organization;
  private final Source.Info source;
  private final AccessMode accessMode;
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long timestamp;
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long lastSeenTimestamp;
  private final List<FactObjectBinding> objects;
  // TODO: Add confidenceLevel once defined.

  private Fact(UUID id, FactType.Info type, String value, Info inReferenceTo, Organization.Info organization, Source.Info source,
               AccessMode accessMode, Long timestamp, Long lastSeenTimestamp, List<FactObjectBinding> objects) {
    this.id = id;
    this.type = type;
    this.value = value;
    this.inReferenceTo = inReferenceTo;
    this.organization = organization;
    this.source = source;
    this.accessMode = accessMode;
    this.timestamp = timestamp;
    this.lastSeenTimestamp = lastSeenTimestamp;
    this.objects = ObjectUtils.ifNotNull(objects, Collections::unmodifiableList);
  }

  public UUID getId() {
    return id;
  }

  public FactType.Info getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public Info getInReferenceTo() {
    return inReferenceTo;
  }

  public Organization.Info getOrganization() {
    return organization;
  }

  public Source.Info getSource() {
    return source;
  }

  public AccessMode getAccessMode() {
    return accessMode;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public Long getLastSeenTimestamp() {
    return lastSeenTimestamp;
  }

  public List<FactObjectBinding> getObjects() {
    return objects;
  }

  public Info toInfo() {
    return new Info(id, type, value);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id;
    private FactType.Info type;
    private String value;
    private Fact.Info inReferenceTo;
    private Organization.Info organization;
    private Source.Info source;
    private AccessMode accessMode;
    private Long timestamp;
    private Long lastSeenTimestamp;
    private List<FactObjectBinding> objects;

    private Builder() {
    }

    public Fact build() {
      return new Fact(id, type, value, inReferenceTo, organization, source, accessMode, timestamp, lastSeenTimestamp, objects);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setType(FactType.Info type) {
      this.type = type;
      return this;
    }

    public Builder setValue(String value) {
      this.value = value;
      return this;
    }

    public Builder setInReferenceTo(Info inReferenceTo) {
      this.inReferenceTo = inReferenceTo;
      return this;
    }

    public Builder setOrganization(Organization.Info organization) {
      this.organization = organization;
      return this;
    }

    public Builder setSource(Source.Info source) {
      this.source = source;
      return this;
    }

    public Builder setAccessMode(AccessMode accessMode) {
      this.accessMode = accessMode;
      return this;
    }

    public Builder setTimestamp(Long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder setLastSeenTimestamp(Long lastSeenTimestamp) {
      this.lastSeenTimestamp = lastSeenTimestamp;
      return this;
    }

    public Builder setObjects(List<FactObjectBinding> objects) {
      this.objects = objects;
      return this;
    }

    public Builder addObject(FactObjectBinding object) {
      this.objects = ListUtils.addToList(this.objects, object);
      return this;
    }
  }

  public static class FactObjectBinding {
    private final Object.Info object;
    private final Direction direction;

    public FactObjectBinding(Object.Info object, Direction direction) {
      this.object = object;
      this.direction = direction;
    }

    public Object.Info getObject() {
      return object;
    }

    public Direction getDirection() {
      return direction;
    }
  }

  public class Info {
    private final UUID id;
    private final FactType.Info type;
    private final String value;

    private Info(UUID id, FactType.Info type, String value) {
      this.id = id;
      this.type = type;
      this.value = value;
    }

    public UUID getId() {
      return id;
    }

    public FactType.Info getType() {
      return type;
    }

    public String getValue() {
      return value;
    }
  }

}
