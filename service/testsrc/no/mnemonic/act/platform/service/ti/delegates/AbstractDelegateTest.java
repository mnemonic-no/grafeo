package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.service.contexts.RequestContext;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.contexts.TriggerContext;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.FactTypeConverter;
import no.mnemonic.act.platform.service.ti.converters.ObjectTypeConverter;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;

import static org.mockito.MockitoAnnotations.initMocks;

abstract class AbstractDelegateTest {

  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private TriggerContext triggerContext;
  @Mock
  private ObjectManager objectManager;
  @Mock
  private FactManager factManager;
  @Mock
  private ValidatorFactory validatorFactory;
  @Mock
  private ObjectTypeConverter objectTypeConverter;
  @Mock
  private FactTypeConverter factTypeConverter;

  @Before
  public void initialize() {
    initMocks(this);

    TiRequestContext requestContext = TiRequestContext.builder()
            .setObjectManager(objectManager)
            .setFactManager(factManager)
            .setValidatorFactory(validatorFactory)
            .build();

    SecurityContext.set(securityContext);
    TriggerContext.set(triggerContext);
    RequestContext.set(requestContext);
  }

  @After
  public void cleanup() {
    SecurityContext.clear();
    TriggerContext.clear();
    RequestContext.clear();
  }

  TiSecurityContext getSecurityContext() {
    return TiSecurityContext.get();
  }

  TriggerContext getTriggerContext() {
    return TriggerContext.get();
  }

  ObjectManager getObjectManager() {
    return objectManager;
  }

  FactManager getFactManager() {
    return factManager;
  }

  ValidatorFactory getValidatorFactory() {
    return validatorFactory;
  }

  ObjectTypeConverter getObjectTypeConverter() {
    return objectTypeConverter;
  }

  FactTypeConverter getFactTypeConverter() {
    return factTypeConverter;
  }
}
