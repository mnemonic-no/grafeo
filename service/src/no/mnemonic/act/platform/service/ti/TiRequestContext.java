package no.mnemonic.act.platform.service.ti;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.service.contexts.RequestContext;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import no.mnemonic.commons.utilities.ObjectUtils;

/**
 * Specific RequestContext for the ThreatIntelligenceService.
 */
public class TiRequestContext extends RequestContext {

  private final ObjectManager objectManager;
  private final FactManager factManager;
  private final ValidatorFactory validatorFactory;

  private TiRequestContext(ObjectManager objectManager,
                           FactManager factManager,
                           ValidatorFactory validatorFactory) {
    this.objectManager = objectManager;
    this.factManager = factManager;
    this.validatorFactory = validatorFactory;
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

  public ValidatorFactory getValidatorFactory() {
    return ObjectUtils.notNull(validatorFactory, "ValidationFactory not set in RequestContext.");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private ObjectManager objectManager;
    private FactManager factManager;
    private ValidatorFactory validatorFactory;

    private Builder() {
    }

    public TiRequestContext build() {
      return new TiRequestContext(objectManager, factManager, validatorFactory);
    }

    public Builder setObjectManager(ObjectManager objectManager) {
      this.objectManager = objectManager;
      return this;
    }

    public Builder setFactManager(FactManager factManager) {
      this.factManager = factManager;
      return this;
    }

    public Builder setValidatorFactory(ValidatorFactory validatorFactory) {
      this.validatorFactory = validatorFactory;
      return this;
    }
  }

}
