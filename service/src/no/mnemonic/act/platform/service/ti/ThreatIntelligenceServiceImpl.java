package no.mnemonic.act.platform.service.ti;

import no.mnemonic.act.platform.api.exceptions.*;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.model.v1.*;
import no.mnemonic.act.platform.api.request.v1.*;
import no.mnemonic.act.platform.api.service.v1.RequestHeader;
import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.act.platform.auth.IdentityResolver;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectFactBindingEntity;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.service.Service;
import no.mnemonic.act.platform.service.contexts.RequestContext;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.delegates.*;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.model.Credentials;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ThreatIntelligenceServiceImpl implements Service, ThreatIntelligenceService {

  public static final UUID GLOBAL_NAMESPACE = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private final AccessController accessController;
  private final IdentityResolver identityResolver;
  private final OrganizationResolver organizationResolver;
  private final SubjectResolver subjectResolver;
  private final FactManager factManager;
  private final ObjectManager objectManager;
  private final FactSearchManager factSearchManager;
  private final ValidatorFactory validatorFactory;
  private final DelegateProvider delegateProvider;

  @Inject
  public ThreatIntelligenceServiceImpl(AccessController accessController,
                                       IdentityResolver identityResolver,
                                       OrganizationResolver organizationResolver,
                                       SubjectResolver subjectResolver,
                                       FactManager factManager,
                                       ObjectManager objectManager,
                                       FactSearchManager factSearchManager,
                                       ValidatorFactory validatorFactory,
                                       DelegateProvider delegateProvider) {
    this.accessController = accessController;
    this.identityResolver = identityResolver;
    this.organizationResolver = organizationResolver;
    this.subjectResolver = subjectResolver;
    this.factManager = factManager;
    this.objectManager = objectManager;
    this.factSearchManager = factSearchManager;
    this.validatorFactory = validatorFactory;
    this.delegateProvider = delegateProvider;
  }

  @Override
  public SecurityContext createSecurityContext(Credentials credentials) {
    return TiSecurityContext.builder()
            .setAccessController(accessController)
            .setIdentityResolver(identityResolver)
            .setOrganizationResolver(organizationResolver)
            .setSubjectResolver(subjectResolver)
            .setCredentials(credentials)
            .setAclResolver(factManager::fetchFactAcl)
            .setFactsBoundToObjectResolver(createFactsBoundToObjectResolver())
            .build();
  }

  @Override
  public RequestContext createRequestContext() {
    return TiRequestContext.builder()
            .setFactManager(factManager)
            .setObjectManager(objectManager)
            .setFactSearchManager(factSearchManager)
            .setValidatorFactory(validatorFactory)
            .build();
  }

  @Override
  public ObjectType getObjectType(RequestHeader rh, GetObjectTypeByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return delegateProvider.get(ObjectTypeGetByIdDelegate.class).handle(request);
  }

  @Override
  public ResultSet<ObjectType> searchObjectTypes(RequestHeader rh, SearchObjectTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return delegateProvider.get(ObjectTypeSearchDelegate.class).handle(request);
  }

  @Override
  public ObjectType createObjectType(RequestHeader rh, CreateObjectTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return delegateProvider.get(ObjectTypeCreateDelegate.class).handle(request);
  }

  @Override
  public ObjectType updateObjectType(RequestHeader rh, UpdateObjectTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return delegateProvider.get(ObjectTypeUpdateDelegate.class).handle(request);
  }

  @Override
  public FactType getFactType(RequestHeader rh, GetFactTypeByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return delegateProvider.get(FactTypeGetByIdDelegate.class).handle(request);
  }

  @Override
  public ResultSet<FactType> searchFactTypes(RequestHeader rh, SearchFactTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return delegateProvider.get(FactTypeSearchDelegate.class).handle(request);
  }

  @Override
  public FactType createFactType(RequestHeader rh, CreateFactTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return delegateProvider.get(FactTypeCreateDelegate.class).handle(request);
  }

  @Override
  public FactType updateFactType(RequestHeader rh, UpdateFactTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return delegateProvider.get(FactTypeUpdateDelegate.class).handle(request);
  }

  @Override
  public Object getObject(RequestHeader rh, GetObjectByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return delegateProvider.get(ObjectGetDelegate.class).handle(request);
  }

  @Override
  public Object getObject(RequestHeader rh, GetObjectByTypeValueRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return delegateProvider.get(ObjectGetDelegate.class).handle(request);
  }

  @Override
  public ResultSet<Fact> searchObjectFacts(RequestHeader rh, SearchObjectFactsRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return delegateProvider.get(ObjectSearchFactsDelegate.class).handle(request);
  }

  @Override
  public ResultSet<Object> searchObjects(RequestHeader rh, SearchObjectRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return delegateProvider.get(ObjectSearchDelegate.class).handle(request);
  }

  @Override
  public Fact getFact(RequestHeader rh, GetFactByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return delegateProvider.get(FactGetByIdDelegate.class).handle(request);
  }

  @Override
  public ResultSet<Fact> searchFacts(RequestHeader rh, SearchFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return delegateProvider.get(FactSearchDelegate.class).handle(request);
  }

  @Override
  public Fact createFact(RequestHeader rh, CreateFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return delegateProvider.get(FactCreateDelegate.class).handle(request);
  }

  @Override
  public ResultSet<Fact> searchMetaFacts(RequestHeader rh, SearchMetaFactsRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return delegateProvider.get(FactSearchMetaDelegate.class).handle(request);
  }

  @Override
  public Fact createMetaFact(RequestHeader rh, CreateMetaFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return delegateProvider.get(FactCreateMetaDelegate.class).handle(request);
  }

  @Override
  public Fact retractFact(RequestHeader rh, RetractFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return delegateProvider.get(FactRetractDelegate.class).handle(request);
  }

  @Override
  public ResultSet<FactComment> getFactComments(RequestHeader rh, GetFactCommentsRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return delegateProvider.get(FactGetCommentsDelegate.class).handle(request);
  }

  @Override
  public FactComment createFactComment(RequestHeader rh, CreateFactCommentRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return delegateProvider.get(FactCreateCommentDelegate.class).handle(request);
  }

  @Override
  public ResultSet<AclEntry> getFactAcl(RequestHeader rh, GetFactAclRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return delegateProvider.get(FactGetAclDelegate.class).handle(request);
  }

  @Override
  public AclEntry grantFactAccess(RequestHeader rh, GrantFactAccessRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return delegateProvider.get(FactGrantAccessDelegate.class).handle(request);
  }

  @Override
  public ResultSet<?> traverseGraph(RequestHeader rh, TraverseByObjectIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    return delegateProvider.get(TraverseGraphDelegate.class).handle(request);
  }

  @Override
  public ResultSet<?> traverseGraph(RequestHeader rh, TraverseByObjectTypeValueRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    return delegateProvider.get(TraverseGraphDelegate.class).handle(request);
  }

  @Override
  public ResultSet<?> traverseGraph(RequestHeader rh, TraverseByObjectSearchRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    return delegateProvider.get(TraverseGraphDelegate.class).handle(request);
  }

  @Override
  public Origin getOrigin(RequestHeader rh, GetOriginByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return delegateProvider.get(OriginGetByIdDelegate.class).handle(request);
  }

  @Override
  public ResultSet<Origin> searchOrigins(RequestHeader rh, SearchOriginRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return delegateProvider.get(OriginSearchDelegate.class).handle(request);
  }

  @Override
  public Origin createOrigin(RequestHeader rh, CreateOriginRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return delegateProvider.get(OriginCreateDelegate.class).handle(request);
  }

  @Override
  public Origin updateOrigin(RequestHeader rh, UpdateOriginRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return delegateProvider.get(OriginUpdateDelegate.class).handle(request);
  }

  private Function<UUID, Iterator<FactEntity>> createFactsBoundToObjectResolver() {
    return objectID -> {
      // Look up bindings for the given Object ID ...
      List<UUID> factID = objectManager.fetchObjectFactBindings(objectID)
              .stream()
              .map(ObjectFactBindingEntity::getFactID)
              .collect(Collectors.toList());
      // ... and use those to fetch the bound Facts.
      return factManager.getFacts(factID);
    };
  }
}
