package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.service.contexts.RequestContext;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.contexts.TriggerContext;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.*;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
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
  private FactSearchManager factSearchManager;
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

  FactSearchManager getFactSearchManager() {
    return factSearchManager;
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

  void expectInvalidArgumentException(InvalidArgumentExceptionTest test, String... messageTemplate) throws Exception {
    try {
      test.execute();
      fail();
    } catch (InvalidArgumentException ex) {
      assertEquals(SetUtils.set(messageTemplate), SetUtils.set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
    }
  }

  interface InvalidArgumentExceptionTest {
    void execute() throws Exception;
  }
}
