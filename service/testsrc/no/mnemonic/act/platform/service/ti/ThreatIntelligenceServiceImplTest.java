package no.mnemonic.act.platform.service.ti;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.entity.handlers.EntityHandlerFactory;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.MockitoAnnotations.initMocks;

public class ThreatIntelligenceServiceImplTest {

  @Mock
  private FactManager factManager;
  @Mock
  private ObjectManager objectManager;
  @Mock
  private EntityHandlerFactory entityHandlerFactory;
  @Mock
  private ValidatorFactory validatorFactory;

  private ThreatIntelligenceServiceImpl service;

  @Before
  public void initialize() {
    initMocks(this);
    service = new ThreatIntelligenceServiceImpl(factManager, objectManager, entityHandlerFactory, validatorFactory);
  }

  @Test
  public void testCreateSecurityContext() {
    assertNotNull(service.createSecurityContext());
  }

  @Test
  public void testCreateRequestContext() {
    TiRequestContext context = (TiRequestContext) service.createRequestContext();
    assertNotNull(context);
    assertNotNull(context.getObjectTypeConverter());
    assertNotNull(context.getFactTypeConverter());
    assertNotNull(context.getObjectConverter());
    assertNotNull(context.getFactConverter());
    assertSame(factManager, context.getFactManager());
    assertSame(objectManager, context.getObjectManager());
    assertSame(entityHandlerFactory, context.getEntityHandlerFactory());
    assertSame(validatorFactory, context.getValidatorFactory());
  }

}
