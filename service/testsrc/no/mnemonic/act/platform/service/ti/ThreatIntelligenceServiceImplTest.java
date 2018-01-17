package no.mnemonic.act.platform.service.ti;

import no.mnemonic.act.platform.auth.IdentityResolver;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.handlers.EntityHandlerFactory;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.model.Credentials;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.MockitoAnnotations.initMocks;

public class ThreatIntelligenceServiceImplTest {

  @Mock
  private Credentials credentials;
  @Mock
  private AccessController accessController;
  @Mock
  private IdentityResolver identityResolver;
  @Mock
  private OrganizationResolver organizationResolver;
  @Mock
  private SubjectResolver subjectResolver;
  @Mock
  private FactManager factManager;
  @Mock
  private ObjectManager objectManager;
  @Mock
  private FactSearchManager factSearchManager;
  @Mock
  private EntityHandlerFactory entityHandlerFactory;
  @Mock
  private ValidatorFactory validatorFactory;

  private ThreatIntelligenceServiceImpl service;

  @Before
  public void initialize() {
    initMocks(this);
    service = new ThreatIntelligenceServiceImpl(accessController, identityResolver, organizationResolver, subjectResolver, factManager, objectManager, factSearchManager, entityHandlerFactory, validatorFactory);
  }

  @Test
  public void testCreateSecurityContext() {
    assertNotNull(service.createSecurityContext(credentials));
  }

  @Test
  public void testCreateRequestContext() {
    TiRequestContext context = (TiRequestContext) service.createRequestContext();
    assertNotNull(context);
    assertNotNull(context.getObjectTypeConverter());
    assertNotNull(context.getFactTypeConverter());
    assertNotNull(context.getObjectConverter());
    assertNotNull(context.getFactConverter());
    assertNotNull(context.getAclEntryConverter());
    assertNotNull(context.getFactCommentConverter());
    assertSame(factManager, context.getFactManager());
    assertSame(objectManager, context.getObjectManager());
    assertSame(factSearchManager, context.getFactSearchManager());
    assertSame(entityHandlerFactory, context.getEntityHandlerFactory());
    assertSame(validatorFactory, context.getValidatorFactory());
  }

}
