package no.mnemonic.act.platform.service.ti.helpers;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;

import java.util.UUID;

import static no.mnemonic.act.platform.service.ti.ThreatIntelligenceServiceImpl.GLOBAL_NAMESPACE;

public class FactTypeResolver {

  public static final UUID RETRACTION_FACT_TYPE_ID = UUID.nameUUIDFromBytes("SystemRetractionFactType".getBytes());
  static final String RETRACTION_FACT_TYPE_NAME = "Retraction";

  private final FactManager factManager;

  public FactTypeResolver(FactManager factManager) {
    this.factManager = factManager;
  }

  /**
   * Tries to resolve a FactTypeEntity.
   * <p>
   * If the provided 'type' parameter is a String representing a UUID the FactType will be fetched by UUID, otherwise
   * it will be fetched by name. An InvalidArgumentException is thrown if the FactType cannot be resolved.
   *
   * @param type Type UUID or type name
   * @return Resolved FactTypeEntity
   * @throws InvalidArgumentException If FactType cannot be resolved
   */
  public FactTypeEntity resolveFactType(String type) throws InvalidArgumentException {
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
              .setEntityHandler("IdentityHandler")
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
