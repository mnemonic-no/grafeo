package no.mnemonic.services.grafeo.service.implementation;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.MapUtils;
import no.mnemonic.services.triggers.pipeline.api.AccessMode;
import no.mnemonic.services.triggers.pipeline.api.TriggerEvent;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Class representing a {@link TriggerEvent} created by the GrafeoService.
 */
public class GrafeoServiceEvent implements TriggerEvent {

  private static final String TI_SERVICE_NAME = "ThreatIntelligenceService";

  public enum ContextParameter {
    AddedFact, RetractionFact, RetractedFact
  }

  public enum EventName {
    FactAdded, FactRetracted
  }

  private final UUID id;
  private final long timestamp;
  private final EventName event;
  private final UUID organization;
  private final AccessMode accessMode;
  private final Map<String, ?> contextParameters;

  private GrafeoServiceEvent(EventName event, UUID organization, AccessMode accessMode, Map<String, ?> contextParameters) {
    this.id = UUID.randomUUID();
    this.timestamp = Instant.now().toEpochMilli();
    this.event = ObjectUtils.notNull(event, "'event' is a required property!");
    this.organization = ObjectUtils.notNull(organization, "'organization' is a required property!");
    this.accessMode = ObjectUtils.notNull(accessMode, "'accessMode' is a required property!");
    this.contextParameters = ObjectUtils.ifNotNull(contextParameters, Collections::unmodifiableMap);
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String getService() {
    return TI_SERVICE_NAME;
  }

  @Override
  public String getEvent() {
    return event.name();
  }

  @Override
  public UUID getOrganization() {
    return organization;
  }

  @Override
  public AccessMode getAccessMode() {
    return accessMode;
  }

  @Override
  public String getScope() {
    return null; // Scopes are not used.
  }

  @Override
  public Map<String, ?> getContextParameters() {
    return contextParameters;
  }

  public static Builder forEvent(EventName event) {
    return new Builder(event);
  }

  public static class Builder {
    private EventName event;
    private UUID organization;
    private AccessMode accessMode;
    private Map<String, Object> contextParameters;

    private Builder(EventName event) {
      this.event = event;
    }

    public GrafeoServiceEvent build() {
      return new GrafeoServiceEvent(event, organization, accessMode, contextParameters);
    }

    public Builder setOrganization(UUID organization) {
      this.organization = organization;
      return this;
    }

    public Builder setAccessMode(no.mnemonic.services.grafeo.api.model.v1.AccessMode accessMode) {
      this.accessMode = convertAccessMode(accessMode);
      return this;
    }

    public Builder setContextParameters(Map<String, Object> contextParameters) {
      this.contextParameters = contextParameters;
      return this;
    }

    public Builder addContextParameter(String parameterName, Object parameterValue) {
      this.contextParameters = MapUtils.addToMap(this.contextParameters, parameterName, parameterValue);
      return this;
    }

    private AccessMode convertAccessMode(no.mnemonic.services.grafeo.api.model.v1.AccessMode accessMode) {
      if (accessMode == null) return null;

      switch (accessMode) {
        case Public:
          return AccessMode.Public;
        case RoleBased:
          return AccessMode.RoleBased;
        case Explicit:
          return AccessMode.Private;
        default:
          throw new IllegalArgumentException("Cannot convert access mode " + accessMode);
      }
    }
  }
}
