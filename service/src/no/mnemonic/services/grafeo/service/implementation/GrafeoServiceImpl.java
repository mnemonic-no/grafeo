package no.mnemonic.services.grafeo.service.implementation;

import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.grafeo.api.exceptions.*;
import no.mnemonic.services.grafeo.api.model.v1.Object;
import no.mnemonic.services.grafeo.api.model.v1.*;
import no.mnemonic.services.grafeo.api.request.v1.*;
import no.mnemonic.services.grafeo.api.service.v1.GrafeoService;
import no.mnemonic.services.grafeo.api.service.v1.RequestHeader;
import no.mnemonic.services.grafeo.auth.IdentitySPI;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.service.Service;
import no.mnemonic.services.grafeo.service.contexts.SecurityContext;
import no.mnemonic.services.grafeo.service.implementation.delegates.*;

import javax.inject.Inject;
import java.util.UUID;

public class GrafeoServiceImpl implements GrafeoService, Service {

  public static final UUID GLOBAL_NAMESPACE = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private final AccessController accessController;
  private final IdentitySPI identityResolver;
  private final ObjectFactDao objectFactDao;
  private final DelegateProvider delegateProvider;

  @Inject
  public GrafeoServiceImpl(AccessController accessController,
                           IdentitySPI identityResolver,
                           ObjectFactDao objectFactDao,
                           DelegateProvider delegateProvider) {
    this.accessController = accessController;
    this.identityResolver = identityResolver;
    this.objectFactDao = objectFactDao;
    this.delegateProvider = delegateProvider;
  }

  @Override
  public SecurityContext createSecurityContext(Credentials credentials) {
    return GrafeoSecurityContext.builder()
            .setAccessController(accessController)
            .setIdentityResolver(identityResolver)
            .setCredentials(credentials)
            .setObjectFactDao(objectFactDao)
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
  public ResultSet<?> traverse(RequestHeader rh, TraverseGraphByObjectsRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    return delegateProvider.get(TraverseByObjectsDelegate.class).handle(request);
  }

  @Override
  public ResultSet<?> traverse(RequestHeader rh, TraverseGraphByObjectSearchRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    return delegateProvider.get(TraverseByObjectSearchDelegate.class).handle(request);
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

  @Override
  public Origin deleteOrigin(RequestHeader rh, DeleteOriginRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return delegateProvider.get(OriginDeleteDelegate.class).handle(request);
  }
}
