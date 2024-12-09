package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.model.v1.Fact;
import no.mnemonic.services.grafeo.api.model.v1.Organization;
import no.mnemonic.services.grafeo.api.model.v1.Subject;
import no.mnemonic.services.grafeo.api.request.v1.AccessMode;
import no.mnemonic.services.grafeo.api.request.v1.CreateMetaFactRequest;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.service.contexts.TriggerContext;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.GrafeoServiceEvent;
import no.mnemonic.services.grafeo.service.implementation.handlers.FactCreateHandler;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactRequestResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactTypeRequestResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Objects;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FactCreateMetaDelegateTest {

  @Mock
  private FactTypeRequestResolver factTypeRequestResolver;
  @Mock
  private FactRequestResolver factRequestResolver;
  @Mock
  private FactCreateHandler factCreateHandler;
  @Mock
  private GrafeoSecurityContext securityContext;
  @Mock
  private TriggerContext triggerContext;
  @InjectMocks
  private FactCreateMetaDelegate delegate;

  private final OriginEntity origin = new OriginEntity()
          .setId(UUID.randomUUID())
          .setName("origin")
          .setTrust(0.1f);
  private final Organization organization = Organization.builder()
          .setId(UUID.randomUUID())
          .setName("organization")
          .build();
  private final Subject subject = Subject.builder()
          .setId(UUID.randomUUID())
          .setName("subject")
          .build();
  private final FactTypeEntity seenInFactType = new FactTypeEntity()
          .setId(UUID.randomUUID())
          .setName("seenIn");
  private final FactTypeEntity observationFactType = new FactTypeEntity()
          .setId(UUID.randomUUID())
          .setName("observation")
          .setValidator("validator")
          .setValidatorParameter("validatorParameter")
          .setDefaultConfidence(0.2f)
          .addRelevantFactBinding(new FactTypeEntity.MetaFactBindingDefinition().setFactTypeID(seenInFactType.getId()));
  private final FactRecord seenIn = new FactRecord()
          .setId(UUID.randomUUID())
          .setTypeID(seenInFactType.getId())
          .setAccessMode(FactRecord.AccessMode.RoleBased);

  @Test
  public void testCreateMetaFactNoAccessToReferencedFact() throws Exception {
    when(factRequestResolver.resolveFact(seenIn.getId())).thenReturn(seenIn);
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission(seenIn);

    assertThrows(AccessDeniedException.class, () -> delegate.handle(createRequest()));
  }

  @Test
  public void testCreateMetaFactWithoutAddPermission() throws Exception {
    when(factRequestResolver.resolveFact(seenIn.getId())).thenReturn(seenIn);
    mockFetchingOrganization();
    mockFetchingFactType();
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.addGrafeoFact, organization.getId());
    assertThrows(AccessDeniedException.class, () -> delegate.handle(createRequest()));
  }

  @Test
  public void testFactValueIsValidated() throws Exception {
    CreateMetaFactRequest request = createRequest();
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).assertValidFactValue(any(), eq(request.getValue()));
  }

  @Test
  public void testValidateBindingFailsWithoutBindingsOnFactType() throws Exception {
    CreateMetaFactRequest request = createRequest();
    observationFactType.setRelevantFactBindings(null);

    when(factRequestResolver.resolveFact(seenIn.getId())).thenReturn(seenIn);
    mockFetchingOrganization();
    mockFetchingFactType();

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
    assertEquals(set("invalid.meta.fact.binding"), set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testValidateBindingFailsOnType() throws Exception {
    FactRecord anotherFact = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID());
    CreateMetaFactRequest request = createRequest()
            .setFact(anotherFact.getId());

    when(factRequestResolver.resolveFact(anotherFact.getId())).thenReturn(anotherFact);
    mockFetchingOrganization();
    mockFetchingFactType();

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
    assertEquals(set("invalid.meta.fact.binding"), set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testCreateMetaFact() throws Exception {
    CreateMetaFactRequest request = createRequest();
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(matchFactRecord(request), notNull(), notNull());
  }

  @Test
  public void testCreateMetaFactWithTimeGlobal() throws Exception {
    seenIn.addFlag(FactRecord.Flag.TimeGlobalIndex);
    CreateMetaFactRequest request = createRequest();
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(argThat(record -> record.isSet(FactRecord.Flag.TimeGlobalIndex)), notNull(), notNull());
  }

  @Test
  public void testCreateMetaFactSetMissingOrganization() throws Exception {
    UUID organizationID = UUID.randomUUID();
    CreateMetaFactRequest request = createRequest()
            .setOrganization(null);
    mockCreateNewFact();

    when(factCreateHandler.resolveOrganization(isNull(), eq(origin)))
            .thenReturn(Organization.builder().setId(organizationID).build());

    delegate.handle(request);

    verify(factCreateHandler).saveFact(argThat(e -> Objects.equals(organizationID, e.getOrganizationID())), notNull(), notNull());
  }

  @Test
  public void testCreateMetaFactSetMissingOrigin() throws Exception {
    UUID originID = UUID.randomUUID();
    CreateMetaFactRequest request = createRequest()
            .setOrigin(null);
    mockCreateNewFact();

    when(factCreateHandler.resolveOrigin(isNull())).thenReturn(new OriginEntity().setId(originID));
    when(factCreateHandler.resolveOrganization(notNull(), notNull())).thenReturn(organization);

    delegate.handle(request);

    verify(factCreateHandler).saveFact(argThat(e -> Objects.equals(originID, e.getOriginID())), notNull(), notNull());
  }

  @Test
  public void testCreateMetaFactSetMissingConfidence() throws Exception {
    CreateMetaFactRequest request = createRequest()
            .setConfidence(null);
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(argThat(e -> Objects.equals(observationFactType.getDefaultConfidence(), e.getConfidence())), notNull(), notNull());
  }

  @Test
  public void testCreateMetaFactSetMissingAccessMode() throws Exception {
    CreateMetaFactRequest request = createRequest()
            .setAccessMode(null);
    mockCreateNewFact();

    when(factCreateHandler.resolveAccessMode(eq(seenIn), isNull())).thenReturn(seenIn.getAccessMode());

    delegate.handle(request);

    verify(factCreateHandler).saveFact(argThat(e -> Objects.equals(seenIn.getAccessMode(), e.getAccessMode())), notNull(), notNull());
  }

  @Test
  public void testCreateMetaFactSavesCommentAndAcl() throws Exception {
    CreateMetaFactRequest request = createRequest();
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(matchFactRecord(request), eq(request.getComment()), eq(list(subject.getId())));
  }

  @Test
  public void testCreateFactRegisterTriggerEvent() throws Exception {
    CreateMetaFactRequest request = createRequest();
    mockCreateNewFact();

    Fact addedFact = delegate.handle(request);

    verify(triggerContext).registerTriggerEvent(argThat(event -> {
      assertNotNull(event);
      assertEquals(GrafeoServiceEvent.EventName.FactAdded.name(), event.getEvent());
      assertEquals(addedFact.getOrganization().getId(), event.getOrganization());
      assertEquals("Private", event.getAccessMode().name());
      assertSame(addedFact, event.getContextParameters().get(GrafeoServiceEvent.ContextParameter.AddedFact.name()));
      return true;
    }));
  }

  private CreateMetaFactRequest createRequest() {
    return new CreateMetaFactRequest()
            .setFact(seenIn.getId())
            .setType(observationFactType.getName())
            .setValue("today")
            .setOrganization(organization.getName())
            .setOrigin(origin.getName())
            .setConfidence(0.3f)
            .setAccessMode(AccessMode.Explicit)
            .setComment("Hello World!")
            .addAcl(subject.getName());
  }

  private void mockCreateNewFact() throws Exception {
    mockFetchingFactType();
    mockFetchingOrganization();
    mockFetchingSubject();

    // Mock fetching of current user.
    when(securityContext.getCurrentUserID()).thenReturn(UUID.randomUUID());
    // Mock fetching of referenced Fact.
    when(factRequestResolver.resolveFact(seenIn.getId())).thenReturn(seenIn);
    when(factCreateHandler.resolveAccessMode(eq(seenIn), any())).thenReturn(FactRecord.AccessMode.Explicit);
    // Mocking needed for registering TriggerEvent.
    when(factCreateHandler.saveFact(any(), any(), any())).then(i -> {
      FactRecord record = i.getArgument(0);
      return Fact.builder()
              .setId(record.getId())
              .setAccessMode(no.mnemonic.services.grafeo.api.model.v1.AccessMode.valueOf(record.getAccessMode().name()))
              .setOrganization(Organization.builder().setId(record.getOrganizationID()).build().toInfo())
              .build();
    });
  }

  private void mockFetchingOrganization() throws Exception {
    when(factCreateHandler.resolveOrigin(origin.getName())).thenReturn(origin);
    when(factCreateHandler.resolveOrganization(organization.getName(), origin)).thenReturn(organization);
  }

  private void mockFetchingSubject() throws Exception {
    when(factCreateHandler.resolveSubjects(notNull())).thenReturn(list(subject));
  }

  private void mockFetchingFactType() throws Exception {
    when(factTypeRequestResolver.resolveFactType(observationFactType.getName())).thenReturn(observationFactType);
  }

  private FactRecord matchFactRecord(CreateMetaFactRequest request) {
    return argThat(record -> {
      assertNotNull(record.getId());
      assertEquals(observationFactType.getId(), record.getTypeID());
      assertEquals(request.getValue(), record.getValue());
      assertEquals(request.getFact(), record.getInReferenceToID());
      assertEquals(organization.getId(), record.getOrganizationID());
      assertNotNull(record.getAddedByID());
      assertNotNull(record.getLastSeenByID());
      assertEquals(origin.getId(), record.getOriginID());
      assertEquals(origin.getTrust(), record.getTrust(), 0.0);
      assertEquals(request.getConfidence(), record.getConfidence(), 0.0);
      assertEquals(request.getAccessMode().name(), record.getAccessMode().name());
      assertTrue(record.getTimestamp() > 0);
      assertTrue(record.getLastSeenTimestamp() > 0);
      assertEquals(record.getTimestamp(), record.getLastSeenTimestamp());

      return true;
    });
  }
}
