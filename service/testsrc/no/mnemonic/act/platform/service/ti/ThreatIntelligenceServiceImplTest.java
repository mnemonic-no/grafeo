package no.mnemonic.act.platform.service.ti;

import no.mnemonic.act.platform.api.request.v1.*;
import no.mnemonic.act.platform.api.service.v1.RequestHeader;
import no.mnemonic.act.platform.auth.IdentityResolver;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.service.ti.delegates.*;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.model.Credentials;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;
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
  private ValidatorFactory validatorFactory;
  @Mock
  private DelegateProvider delegateProvider;

  private ThreatIntelligenceServiceImpl service;

  @Before
  public void initialize() {
    initMocks(this);
    service = new ThreatIntelligenceServiceImpl(
            accessController,
            identityResolver,
            organizationResolver,
            subjectResolver,
            factManager,
            objectManager,
            factSearchManager,
            validatorFactory,
            delegateProvider
    );
  }

  @Test
  public void testCreateSecurityContext() {
    assertNotNull(service.createSecurityContext(credentials));
  }

  @Test
  public void testCreateRequestContext() {
    TiRequestContext context = (TiRequestContext) service.createRequestContext();
    assertNotNull(context);
    assertSame(factManager, context.getFactManager());
    assertSame(objectManager, context.getObjectManager());
    assertSame(factSearchManager, context.getFactSearchManager());
    assertSame(validatorFactory, context.getValidatorFactory());
  }

  @Test
  public void testGetObjectTypeCallsDelegate() throws Exception {
    ObjectTypeGetByIdDelegate delegate = mock(ObjectTypeGetByIdDelegate.class);
    when(delegateProvider.get(ObjectTypeGetByIdDelegate.class)).thenReturn(delegate);

    GetObjectTypeByIdRequest request = new GetObjectTypeByIdRequest();
    service.getObjectType(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testSearchObjectTypesCallsDelegate() throws Exception {
    ObjectTypeSearchDelegate delegate = mock(ObjectTypeSearchDelegate.class);
    when(delegateProvider.get(ObjectTypeSearchDelegate.class)).thenReturn(delegate);

    SearchObjectTypeRequest request = new SearchObjectTypeRequest();
    service.searchObjectTypes(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testCreateObjectTypeCallsDelegate() throws Exception {
    ObjectTypeCreateDelegate delegate = mock(ObjectTypeCreateDelegate.class);
    when(delegateProvider.get(ObjectTypeCreateDelegate.class)).thenReturn(delegate);

    CreateObjectTypeRequest request = new CreateObjectTypeRequest();
    service.createObjectType(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testUpdateObjectTypeCallsDelegate() throws Exception {
    ObjectTypeUpdateDelegate delegate = mock(ObjectTypeUpdateDelegate.class);
    when(delegateProvider.get(ObjectTypeUpdateDelegate.class)).thenReturn(delegate);

    UpdateObjectTypeRequest request = new UpdateObjectTypeRequest();
    service.updateObjectType(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testGetFactTypeCallsDelegate() throws Exception {
    FactTypeGetByIdDelegate delegate = mock(FactTypeGetByIdDelegate.class);
    when(delegateProvider.get(FactTypeGetByIdDelegate.class)).thenReturn(delegate);

    GetFactTypeByIdRequest request = new GetFactTypeByIdRequest();
    service.getFactType(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testSearchFactTypesCallsDelegate() throws Exception {
    FactTypeSearchDelegate delegate = mock(FactTypeSearchDelegate.class);
    when(delegateProvider.get(FactTypeSearchDelegate.class)).thenReturn(delegate);

    SearchFactTypeRequest request = new SearchFactTypeRequest();
    service.searchFactTypes(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testCreateFactTypeCallsDelegate() throws Exception {
    FactTypeCreateDelegate delegate = mock(FactTypeCreateDelegate.class);
    when(delegateProvider.get(FactTypeCreateDelegate.class)).thenReturn(delegate);

    CreateFactTypeRequest request = new CreateFactTypeRequest();
    service.createFactType(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testUpdateFactTypeCallsDelegate() throws Exception {
    FactTypeUpdateDelegate delegate = mock(FactTypeUpdateDelegate.class);
    when(delegateProvider.get(FactTypeUpdateDelegate.class)).thenReturn(delegate);

    UpdateFactTypeRequest request = new UpdateFactTypeRequest();
    service.updateFactType(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testGetObjectByIdCallsDelegate() throws Exception {
    ObjectGetDelegate delegate = mock(ObjectGetDelegate.class);
    when(delegateProvider.get(ObjectGetDelegate.class)).thenReturn(delegate);

    GetObjectByIdRequest request = new GetObjectByIdRequest();
    service.getObject(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testGetObjectByTypeValueCallsDelegate() throws Exception {
    ObjectGetDelegate delegate = mock(ObjectGetDelegate.class);
    when(delegateProvider.get(ObjectGetDelegate.class)).thenReturn(delegate);

    GetObjectByTypeValueRequest request = new GetObjectByTypeValueRequest();
    service.getObject(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testSearchObjectFactsCallsDelegate() throws Exception {
    ObjectSearchFactsDelegate delegate = mock(ObjectSearchFactsDelegate.class);
    when(delegateProvider.get(ObjectSearchFactsDelegate.class)).thenReturn(delegate);

    SearchObjectFactsRequest request = new SearchObjectFactsRequest();
    service.searchObjectFacts(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testSearchObjectsCallsDelegate() throws Exception {
    ObjectSearchDelegate delegate = mock(ObjectSearchDelegate.class);
    when(delegateProvider.get(ObjectSearchDelegate.class)).thenReturn(delegate);

    SearchObjectRequest request = new SearchObjectRequest();
    service.searchObjects(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testGetFactCallsDelegate() throws Exception {
    FactGetByIdDelegate delegate = mock(FactGetByIdDelegate.class);
    when(delegateProvider.get(FactGetByIdDelegate.class)).thenReturn(delegate);

    GetFactByIdRequest request = new GetFactByIdRequest();
    service.getFact(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testSearchFactsCallsDelegate() throws Exception {
    FactSearchDelegate delegate = mock(FactSearchDelegate.class);
    when(delegateProvider.get(FactSearchDelegate.class)).thenReturn(delegate);

    SearchFactRequest request = new SearchFactRequest();
    service.searchFacts(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testCreateFactCallsDelegate() throws Exception {
    FactCreateDelegate delegate = mock(FactCreateDelegate.class);
    when(delegateProvider.get(FactCreateDelegate.class)).thenReturn(delegate);

    CreateFactRequest request = new CreateFactRequest();
    service.createFact(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testSearchMetaFactsCallsDelegate() throws Exception {
    FactSearchMetaDelegate delegate = mock(FactSearchMetaDelegate.class);
    when(delegateProvider.get(FactSearchMetaDelegate.class)).thenReturn(delegate);

    SearchMetaFactsRequest request = new SearchMetaFactsRequest();
    service.searchMetaFacts(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testCreateMetaFactCallsDelegate() throws Exception {
    FactCreateMetaDelegate delegate = mock(FactCreateMetaDelegate.class);
    when(delegateProvider.get(FactCreateMetaDelegate.class)).thenReturn(delegate);

    CreateMetaFactRequest request = new CreateMetaFactRequest();
    service.createMetaFact(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testRetractFactCallsDelegate() throws Exception {
    FactRetractDelegate delegate = mock(FactRetractDelegate.class);
    when(delegateProvider.get(FactRetractDelegate.class)).thenReturn(delegate);

    RetractFactRequest request = new RetractFactRequest();
    service.retractFact(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testGetFactCommentsCallsDelegate() throws Exception {
    FactGetCommentsDelegate delegate = mock(FactGetCommentsDelegate.class);
    when(delegateProvider.get(FactGetCommentsDelegate.class)).thenReturn(delegate);

    GetFactCommentsRequest request = new GetFactCommentsRequest();
    service.getFactComments(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testCreateFactCommentCallsDelegate() throws Exception {
    FactCreateCommentDelegate delegate = mock(FactCreateCommentDelegate.class);
    when(delegateProvider.get(FactCreateCommentDelegate.class)).thenReturn(delegate);

    CreateFactCommentRequest request = new CreateFactCommentRequest();
    service.createFactComment(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testGetFactAclCallsDelegate() throws Exception {
    FactGetAclDelegate delegate = mock(FactGetAclDelegate.class);
    when(delegateProvider.get(FactGetAclDelegate.class)).thenReturn(delegate);

    GetFactAclRequest request = new GetFactAclRequest();
    service.getFactAcl(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testGrantFactAccessCallsDelegate() throws Exception {
    FactGrantAccessDelegate delegate = mock(FactGrantAccessDelegate.class);
    when(delegateProvider.get(FactGrantAccessDelegate.class)).thenReturn(delegate);

    GrantFactAccessRequest request = new GrantFactAccessRequest();
    service.grantFactAccess(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testTraverseGraphByObjectIdCallsDelegate() throws Exception {
    TraverseGraphDelegate delegate = mock(TraverseGraphDelegate.class);
    when(delegateProvider.get(TraverseGraphDelegate.class)).thenReturn(delegate);

    TraverseByObjectIdRequest request = new TraverseByObjectIdRequest();
    service.traverseGraph(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testTraverseGraphByObjectTypeValueCallsDelegate() throws Exception {
    TraverseGraphDelegate delegate = mock(TraverseGraphDelegate.class);
    when(delegateProvider.get(TraverseGraphDelegate.class)).thenReturn(delegate);

    TraverseByObjectTypeValueRequest request = new TraverseByObjectTypeValueRequest();
    service.traverseGraph(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }

  @Test
  public void testTraverseGraphByObjectSearchCallsDelegate() throws Exception {
    TraverseGraphDelegate delegate = mock(TraverseGraphDelegate.class);
    when(delegateProvider.get(TraverseGraphDelegate.class)).thenReturn(delegate);

    TraverseByObjectSearchRequest request = new TraverseByObjectSearchRequest();
    service.traverseGraph(RequestHeader.builder().build(), request);
    verify(delegate).handle(request);
  }
}
