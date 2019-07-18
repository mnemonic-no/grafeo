package no.mnemonic.act.platform.service.ti;

import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.model.v1.*;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.service.contexts.RequestContext;
import no.mnemonic.act.platform.service.ti.handlers.FactRetractionHandler;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.function.Function;

/**
 * Specific RequestContext for the ThreatIntelligenceService.
 */
public class TiRequestContext extends RequestContext {

  private final ObjectManager objectManager;
  private final FactManager factManager;
  private final FactSearchManager factSearchManager;
  private final FactRetractionHandler retractionHandler;
  private final ValidatorFactory validatorFactory;
  private final Function<ObjectTypeEntity, ObjectType> objectTypeConverter;
  private final Function<FactTypeEntity, FactType> factTypeConverter;
  private final Function<ObjectEntity, Object> objectConverter;
  private final Function<FactEntity, Fact> factConverter;
  private final Function<FactAclEntity, AclEntry> aclEntryConverter;
  private final Function<FactCommentEntity, FactComment> factCommentConverter;

  private TiRequestContext(ObjectManager objectManager,
                           FactManager factManager,
                           FactSearchManager factSearchManager,
                           FactRetractionHandler retractionHandler,
                           ValidatorFactory validatorFactory,
                           Function<ObjectTypeEntity, ObjectType> objectTypeConverter,
                           Function<FactTypeEntity, FactType> factTypeConverter,
                           Function<ObjectEntity, Object> objectConverter,
                           Function<FactEntity, Fact> factConverter,
                           Function<FactAclEntity, AclEntry> aclEntryConverter,
                           Function<FactCommentEntity, FactComment> factCommentConverter) {
    this.objectManager = objectManager;
    this.factManager = factManager;
    this.factSearchManager = factSearchManager;
    this.retractionHandler = retractionHandler;
    this.validatorFactory = validatorFactory;
    this.objectTypeConverter = objectTypeConverter;
    this.factTypeConverter = factTypeConverter;
    this.objectConverter = objectConverter;
    this.factConverter = factConverter;
    this.aclEntryConverter = aclEntryConverter;
    this.factCommentConverter = factCommentConverter;
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

  public FactSearchManager getFactSearchManager() {
    return ObjectUtils.notNull(factSearchManager, "FactSearchManager not set in RequestContext.");
  }

  public FactRetractionHandler getRetractionHandler() {
    return ObjectUtils.notNull(retractionHandler, "FactRetractionHandler not set in RequestContext.");
  }

  public ValidatorFactory getValidatorFactory() {
    return ObjectUtils.notNull(validatorFactory, "ValidationFactory not set in RequestContext.");
  }

  public Function<ObjectTypeEntity, ObjectType> getObjectTypeConverter() {
    return ObjectUtils.notNull(objectTypeConverter, "ObjectTypeConverter not set in RequestContext.");
  }

  public Function<FactTypeEntity, FactType> getFactTypeConverter() {
    return ObjectUtils.notNull(factTypeConverter, "FactTypeConverter not set in RequestContext.");
  }

  public Function<ObjectEntity, Object> getObjectConverter() {
    return ObjectUtils.notNull(objectConverter, "ObjectConverter not set in RequestContext.");
  }

  public Function<FactEntity, Fact> getFactConverter() {
    return ObjectUtils.notNull(factConverter, "FactConverter not set in RequestContext.");
  }

  public Function<FactAclEntity, AclEntry> getAclEntryConverter() {
    return ObjectUtils.notNull(aclEntryConverter, "AclEntryConverter not set in RequestContext.");
  }

  public Function<FactCommentEntity, FactComment> getFactCommentConverter() {
    return ObjectUtils.notNull(factCommentConverter, "FactCommentConverter not set in RequestContext.");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private ObjectManager objectManager;
    private FactManager factManager;
    private FactSearchManager factSearchManager;
    private FactRetractionHandler retractionHandler;
    private ValidatorFactory validatorFactory;
    private Function<ObjectTypeEntity, ObjectType> objectTypeConverter;
    private Function<FactTypeEntity, FactType> factTypeConverter;
    private Function<ObjectEntity, Object> objectConverter;
    private Function<FactEntity, Fact> factConverter;
    private Function<FactAclEntity, AclEntry> aclEntryConverter;
    private Function<FactCommentEntity, FactComment> factCommentConverter;

    private Builder() {
    }

    public TiRequestContext build() {
      return new TiRequestContext(objectManager, factManager, factSearchManager, retractionHandler, validatorFactory,
              objectTypeConverter, factTypeConverter, objectConverter, factConverter, aclEntryConverter, factCommentConverter);
    }

    public Builder setObjectManager(ObjectManager objectManager) {
      this.objectManager = objectManager;
      return this;
    }

    public Builder setFactManager(FactManager factManager) {
      this.factManager = factManager;
      return this;
    }

    public Builder setFactSearchManager(FactSearchManager factSearchManager) {
      this.factSearchManager = factSearchManager;
      return this;
    }

    public Builder setRetractionHandler(FactRetractionHandler retractionHandler) {
      this.retractionHandler = retractionHandler;
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

    public Builder setFactTypeConverter(Function<FactTypeEntity, FactType> factTypeConverter) {
      this.factTypeConverter = factTypeConverter;
      return this;
    }

    public Builder setObjectConverter(Function<ObjectEntity, Object> objectConverter) {
      this.objectConverter = objectConverter;
      return this;
    }

    public Builder setFactConverter(Function<FactEntity, Fact> factConverter) {
      this.factConverter = factConverter;
      return this;
    }

    public Builder setAclEntryConverter(Function<FactAclEntity, AclEntry> aclEntryConverter) {
      this.aclEntryConverter = aclEntryConverter;
      return this;
    }

    public Builder setFactCommentConverter(Function<FactCommentEntity, FactComment> factCommentConverter) {
      this.factCommentConverter = factCommentConverter;
      return this;
    }
  }

}
