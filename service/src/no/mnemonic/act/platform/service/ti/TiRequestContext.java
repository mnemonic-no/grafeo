package no.mnemonic.act.platform.service.ti;

import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import no.mnemonic.act.platform.entity.handlers.EntityHandlerFactory;
import no.mnemonic.act.platform.service.contexts.RequestContext;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.function.Function;

/**
 * Specific RequestContext for the ThreatIntelligenceService.
 */
public class TiRequestContext extends RequestContext {

  private final ObjectManager objectManager;
  private final FactManager factManager;
  private final EntityHandlerFactory entityHandlerFactory;
  private final ValidatorFactory validatorFactory;
  private final Function<ObjectTypeEntity, ObjectType> objectTypeConverter;

  private TiRequestContext(ObjectManager objectManager, FactManager factManager,
                           EntityHandlerFactory entityHandlerFactory, ValidatorFactory validatorFactory,
                           Function<ObjectTypeEntity, ObjectType> objectTypeConverter) {
    this.objectManager = objectManager;
    this.factManager = factManager;
    this.entityHandlerFactory = entityHandlerFactory;
    this.validatorFactory = validatorFactory;
    this.objectTypeConverter = objectTypeConverter;
  }

  public static TiRequestContext get() {
    return (TiRequestContext) RequestContext.get();
  }

  public ObjectManager getObjectManager() {
    return ObjectUtils.notNull(objectManager, "ObjectManager not set in RequestContext.");
  }

  public FactManager getFactManager() {
    return ObjectUtils.notNull(factManager, "FactManager not set in RequestContext.");
  }

  public EntityHandlerFactory getEntityHandlerFactory() {
    return ObjectUtils.notNull(entityHandlerFactory, "EntityHandlerFactory not set in RequestContext.");
  }

  public ValidatorFactory getValidatorFactory() {
    return ObjectUtils.notNull(validatorFactory, "ValidationFactory not set in RequestContext.");
  }

  public Function<ObjectTypeEntity, ObjectType> getObjectTypeConverter() {
    return ObjectUtils.notNull(objectTypeConverter, "ObjectTypeConverter not set in RequestContext.");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private ObjectManager objectManager;
    private FactManager factManager;
    private EntityHandlerFactory entityHandlerFactory;
    private ValidatorFactory validatorFactory;
    private Function<ObjectTypeEntity, ObjectType> objectTypeConverter;

    private Builder() {
    }

    public TiRequestContext build() {
      return new TiRequestContext(objectManager, factManager, entityHandlerFactory, validatorFactory, objectTypeConverter);
    }

    public Builder setObjectManager(ObjectManager objectManager) {
      this.objectManager = objectManager;
      return this;
    }

    public Builder setFactManager(FactManager factManager) {
      this.factManager = factManager;
      return this;
    }

    public Builder setEntityHandlerFactory(EntityHandlerFactory entityHandlerFactory) {
      this.entityHandlerFactory = entityHandlerFactory;
      return this;
    }

    public Builder setValidatorFactory(ValidatorFactory validatorFactory) {
      this.validatorFactory = validatorFactory;
      return this;
    }

    public Builder setObjectTypeConverter(Function<ObjectTypeEntity, ObjectType> objectTypeConverter) {
      this.objectTypeConverter = objectTypeConverter;
      return this;
    }
  }

}
