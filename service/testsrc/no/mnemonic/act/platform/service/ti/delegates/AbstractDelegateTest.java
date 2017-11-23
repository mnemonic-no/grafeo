package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
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

import static org.mockito.MockitoAnnotations.initMocks;

abstract class AbstractDelegateTest {

  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private ObjectManager objectManager;
  @Mock
  private FactManager factManager;
  @Mock
  private FactSearchManager factSearchManager;
  @Mock
  private EntityHandlerFactory entityHandlerFactory;
  @Mock
  private ValidatorFactory validatorFactory;
  @Mock
  private ObjectTypeConverter objectTypeConverter;
  @Mock
  private ObjectConverter objectConverter;
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

    TiRequestContext requestContext = TiRequestContext.builder()
            .setObjectManager(objectManager)
            .setFactManager(factManager)
            .setFactSearchManager(factSearchManager)
            .setEntityHandlerFactory(entityHandlerFactory)
            .setValidatorFactory(validatorFactory)
            .setObjectTypeConverter(objectTypeConverter)
            .setObjectConverter(objectConverter)
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
    SecurityContext.clear();
    RequestContext.clear();
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

  FactSearchManager getFactSearchManager() {
    return factSearchManager;
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

  ObjectConverter getObjectConverter() {
    return objectConverter;
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
