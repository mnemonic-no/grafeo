package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.entity.handlers.EntityHandlerFactory;
import no.mnemonic.act.platform.service.contexts.RequestContext;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.converters.FactTypeConverter;
import no.mnemonic.act.platform.service.ti.converters.ObjectTypeConverter;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.mockito.MockitoAnnotations.initMocks;

abstract class AbstractDelegateTest {

  @Spy // Needs to be a spy, otherwise the SecurityContext won't close correctly.
  private SecurityContext securityContext;
  @Mock
  private ObjectManager objectManager;
  @Mock
  private FactManager factManager;
  @Mock
  private EntityHandlerFactory entityHandlerFactory;
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
            .setEntityHandlerFactory(entityHandlerFactory)
            .setValidatorFactory(validatorFactory)
            .setObjectTypeConverter(objectTypeConverter)
            .setFactTypeConverter(factTypeConverter)
            .build();

    SecurityContext.set(securityContext);
    RequestContext.set(requestContext);
  }

  @After
  public void cleanup() throws Exception {
    SecurityContext.get().close();
    RequestContext.get().close();
  }

  SecurityContext getSecurityContext() {
    return securityContext;
  }

  ObjectManager getObjectManager() {
    return objectManager;
  }

  FactManager getFactManager() {
    return factManager;
  }

  EntityHandlerFactory getEntityHandlerFactory() {
    return entityHandlerFactory;
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
