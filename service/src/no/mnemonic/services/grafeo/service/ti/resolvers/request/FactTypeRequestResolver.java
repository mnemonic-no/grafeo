package no.mnemonic.services.grafeo.service.ti.resolvers.request;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;

import javax.inject.Inject;
import java.util.Objects;
import java.util.UUID;

import static no.mnemonic.services.grafeo.service.ti.ThreatIntelligenceServiceImpl.GLOBAL_NAMESPACE;

public class FactTypeRequestResolver {

  static final UUID RETRACTION_FACT_TYPE_ID = UUID.nameUUIDFromBytes("SystemRetractionFactType".getBytes());
  static final String RETRACTION_FACT_TYPE_NAME = "Retraction";

  private final FactManager factManager;

  @Inject
  public FactTypeRequestResolver(FactManager factManager) {
    this.factManager = factManager;
  }

  /**
   * Fetch an existing FactType by ID.
   *
   * @param id UUID of FactType
   * @return Existing FactType
   * @throws ObjectNotFoundException Thrown if FactType cannot be found
   */
  public FactTypeEntity fetchExistingFactType(UUID id) throws ObjectNotFoundException {
    FactTypeEntity entity = factManager.getFactType(id);
    if (entity == null) {
      throw new ObjectNotFoundException(String.format("FactType with id = %s does not exist.", id),
        "fact.type.not.exist", "id", ObjectUtils.ifNotNull(id, Object::toString, "NULL"));
    }
    return entity;
  }

  /**
   * Tries to resolve a FactTypeEntity.
   * <p>
   * If the provided 'type' parameter is a String representing a UUID the FactType will be fetched by UUID, otherwise
   * it will be fetched by name. An InvalidArgumentException is thrown if the FactType cannot be resolved.
   *
   * @param type Type UUID or type name
   * @return Resolved FactTypeEntity
   * @throws AccessDeniedException    If system-defined Retraction FactType is resolved
   * @throws InvalidArgumentException If FactType cannot be resolved
   */
  public FactTypeEntity resolveFactType(String type) throws AccessDeniedException, InvalidArgumentException {
    FactTypeEntity typeEntity;

    try {
      typeEntity = factManager.getFactType(UUID.fromString(type));
    } catch (IllegalArgumentException ignored) {
      // Can't convert 'type' field to UUID, try to fetch FactType by name.
      typeEntity = factManager.getFactType(type);
    }

    if (typeEntity == null) {
      throw new InvalidArgumentException().addValidationError("FactType does not exist.", "fact.type.not.exist", "type", type);
    }

    if (Objects.equals(typeEntity.getId(), RETRACTION_FACT_TYPE_ID)) {
      throw new AccessDeniedException("Not allowed to manually use system-defined Retraction FactType. Use /retract endpoint instead.");
    }

    return typeEntity;
  }

  /**
   * Resolves the FactType used for retracting Facts. It will create the required FactType if it does not exist yet.
   * <p>
   * TODO: Creating the FactType should be moved to a bootstrap method (together with other system types).
   *
   * @return Retraction FactType
   */
  public FactTypeEntity resolveRetractionFactType() {
    FactTypeEntity typeEntity = factManager.getFactType(RETRACTION_FACT_TYPE_ID);

    if (typeEntity == null) {
      // Need to create Retraction FactType. It doesn't need any bindings because it will directly reference the retracted Fact.
      typeEntity = factManager.saveFactType(new FactTypeEntity()
              .setId(RETRACTION_FACT_TYPE_ID)
              .setNamespaceID(GLOBAL_NAMESPACE)
              .setName(createRetractionFactTypeName())
              .setDefaultConfidence(1.0f)
              .setValidator("TrueValidator"));
    }

    return typeEntity;
  }

  private String createRetractionFactTypeName() {
    // This is needed to avoid an unlikely name collision (a FactType's name needs to be unique) when a user created a
    // "Retraction" FactType first, but still the authoritative FactType created here should be used for Retraction.
    // Should not be necessary any longer once a bootstrap method is implemented which creates the "Retraction" FactType on first start up.
    FactTypeEntity collision = factManager.getFactType(RETRACTION_FACT_TYPE_NAME);
    return collision == null ? RETRACTION_FACT_TYPE_NAME : RETRACTION_FACT_TYPE_NAME + "-" + UUID.randomUUID().toString().substring(0, 8);
  }
}
