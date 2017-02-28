package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.entity.handlers.EntityHandlerFactory;
import no.mnemonic.act.platform.service.contexts.RequestContext;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.*;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;

import static org.mockito.Mockito.spy;
import static org.mockito.MockitoAnnotations.initMocks;

abstract class AbstractDelegateTest {

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
  @Mock
  private FactConverter factConverter;
  @Mock
  private AclEntryConverter aclEntryConverter;
  @Mock
  private FactCommentConverter factCommentConverter;

  @Before
  public void initialize() {
    initMocks(this);

    // Need to spy on context in order to be able to stub methods.
    TiSecurityContext securityContext = spy(TiSecurityContext.builder()
            .setAclResolver(id -> null)
            .build());
    TiRequestContext requestContext = TiRequestContext.builder()
            .setObjectManager(objectManager)
            .setFactManager(factManager)
            .setEntityHandlerFactory(entityHandlerFactory)
            .setValidatorFactory(validatorFactory)
            .setObjectTypeConverter(objectTypeConverter)
            .setFactTypeConverter(factTypeConverter)
            .setFactConverter(factConverter)
            .setAclEntryConverter(aclEntryConverter)
            .setFactCommentConverter(factCommentConverter)
            .build();

    SecurityContext.set(securityContext);
    RequestContext.set(requestContext);
  }

  @After
  public void cleanup() throws Exception {
    SecurityContext.get().close();
    RequestContext.get().close();
  }

  TiSecurityContext getSecurityContext() {
    return TiSecurityContext.get();
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

  FactConverter getFactConverter() {
    return factConverter;
  }

  AclEntryConverter getAclEntryConverter() {
    return aclEntryConverter;
  }

  FactCommentConverter getFactCommentConverter() {
    return factCommentConverter;
  }
}
