package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.api.request.v1.AccessMode;
import no.mnemonic.act.platform.api.request.v1.RetractFactRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.contexts.TriggerContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.TiServiceEvent;
import no.mnemonic.act.platform.service.ti.converters.response.FactResponseConverter;
import no.mnemonic.act.platform.service.ti.handlers.FactCreateHandler;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactRequestResolver;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactTypeRequestResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactRetractDelegateTest {

  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private FactTypeRequestResolver factTypeRequestResolver;
  @Mock
  private FactRequestResolver factRequestResolver;
  @Mock
  private FactCreateHandler factCreateHandler;
  @Mock
  private FactResponseConverter factResponseConverter;
  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private TriggerContext triggerContext;
  @Mock
  private Clock clock;

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

  private FactRetractDelegate delegate;

  @Before
  public void setup() {
    initMocks(this);
    delegate = new FactRetractDelegate(
            securityContext,
            triggerContext,
            objectFactDao,
            factTypeRequestResolver,
            factRequestResolver,
            factCreateHandler,
            factResponseConverter
    ).withClock(clock);

    when(clock.millis()).thenReturn(1000L, 2000L, 3000L);
  }

  @Test(expected = AccessDeniedException.class)
  public void testRetractFactNoAccessToFact() throws Exception {
    RetractFactRequest request = mockRetractingFact();
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission(isA(FactRecord.class));

    delegate.handle(request);
  }

  @Test(expected = AccessDeniedException.class)
  public void testRetractFactWithoutAddPermission() throws Exception {
    RetractFactRequest request = mockRetractingFact();
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.addThreatIntelFact, organization.getId());

    delegate.handle(request);
  }

  @Test
  public void testRetractFact() throws Exception {
    RetractFactRequest request = mockRetractingFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(matchFactRecord(request), any(), any());
    verify(objectFactDao).retractFact(argThat(e -> Objects.equals(e.getId(), request.getFact())));
  }

  @Test
  public void testRetractFactSetMissingOrganization() throws Exception {
    RetractFactRequest request = mockRetractingFact().setOrganization(null);

    when(factCreateHandler.resolveOrganization(isNull(), notNull())).thenReturn(organization);

    Fact fact = delegate.handle(request);

    verify(factCreateHandler).saveFact(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && Objects.equals(organization.getId(), e.getOrganizationID())), any(), any());
  }

  @Test
  public void testRetractFactSetMissingOrigin() throws Exception {
    RetractFactRequest request = mockRetractingFact().setOrigin(null);

    when(factCreateHandler.resolveOrigin(isNull())).thenReturn(origin);
    when(factCreateHandler.resolveOrganization(notNull(), notNull())).thenReturn(organization);

    Fact fact = delegate.handle(request);

    verify(factCreateHandler).saveFact(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && Objects.equals(origin.getId(), e.getOriginID())), any(), any());
  }

  @Test
  public void testRetractFactSetMissingConfidence() throws Exception {
    RetractFactRequest request = mockRetractingFact().setConfidence(null);

    Fact fact = delegate.handle(request);

    verify(factCreateHandler).saveFact(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && e.getConfidence() > 0.0), any(), any());
  }

  @Test
  public void testRetractFactSetMissingAccessMode() throws Exception {
    RetractFactRequest request = mockRetractingFact().setAccessMode(null);

    when(factCreateHandler.resolveAccessMode(notNull(), isNull())).thenReturn(FactRecord.AccessMode.RoleBased);

    Fact fact = delegate.handle(request);

    verify(factCreateHandler).saveFact(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && e.getAccessMode() == FactRecord.AccessMode.RoleBased), any(), any());
  }

  @Test
  public void testRetractFactStoresAclAndComment() throws Exception {
    RetractFactRequest request = mockRetractingFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(matchFactRecord(request), eq(request.getComment()), eq(list(subject.getId())));
  }

  @Test
  public void testRetractFactRegistersTriggerEvent() throws Exception {
    RetractFactRequest request = mockRetractingFact();

    Fact retractionFact = delegate.handle(request);

    verify(triggerContext).registerTriggerEvent(argThat(event -> {
      assertNotNull(event);
      assertEquals(TiServiceEvent.EventName.FactRetracted.name(), event.getEvent());
      assertEquals(retractionFact.getOrganization().getId(), event.getOrganization());
      assertEquals("Private", event.getAccessMode().name());
      assertEquals(request.getFact(), Fact.class.cast(event.getContextParameters().get(TiServiceEvent.ContextParameter.RetractedFact.name())).getId());
      assertSame(retractionFact, event.getContextParameters().get(TiServiceEvent.ContextParameter.RetractionFact.name()));
      return true;
    }));
  }

  private RetractFactRequest mockRetractingFact() throws Exception {
    RetractFactRequest request = createRetractRequest();

    FactTypeEntity retractionFactType = new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setName("retractionFact")
            .setDefaultConfidence(0.2f);
    FactRecord factToRetract = new FactRecord()
            .setId(request.getFact())
            .setAccessMode(FactRecord.AccessMode.RoleBased);

    when(securityContext.getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(factCreateHandler.resolveOrigin(origin.getName())).thenReturn(origin);
    when(factCreateHandler.resolveOrganization(organization.getName(), origin)).thenReturn(organization);
    when(factCreateHandler.resolveSubjects(notNull())).thenReturn(list(subject));

    when(factTypeRequestResolver.resolveRetractionFactType()).thenReturn(retractionFactType);
    // Mock fetching of Fact to retract.
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(factToRetract);
    when(factCreateHandler.resolveAccessMode(eq(factToRetract), any())).thenReturn(FactRecord.AccessMode.Explicit);
    // Mock stuff needed for saving Facts.
    when(objectFactDao.retractFact(any())).thenAnswer(i -> i.getArgument(0));

    // Mocking needed for registering TriggerEvent.
    when(factResponseConverter.apply(any())).then(i -> {
      FactRecord entity = i.getArgument(0);
      return Fact.builder()
              .setId(entity.getId())
              .setAccessMode(no.mnemonic.act.platform.api.model.v1.AccessMode.valueOf(entity.getAccessMode().name()))
              .setOrganization(Organization.builder().setId(entity.getOrganizationID()).build().toInfo())
              .build();
    });
    when(factCreateHandler.saveFact(any(), any(), any())).then(i -> {
      FactRecord entity = i.getArgument(0);
      return Fact.builder()
              .setId(entity.getId())
              .setAccessMode(no.mnemonic.act.platform.api.model.v1.AccessMode.valueOf(entity.getAccessMode().name()))
              .setOrganization(Organization.builder().setId(entity.getOrganizationID()).build().toInfo())
              .build();
    });

    return request;
  }

  private RetractFactRequest createRetractRequest() {
    return new RetractFactRequest()
            .setFact(UUID.randomUUID())
            .setOrganization(organization.getName())
            .setOrigin(origin.getName())
            .setConfidence(0.3f)
            .setComment("Hello World!")
            .setAccessMode(AccessMode.Explicit)
            .addAcl(subject.getName());
  }

  private FactRecord matchFactRecord(RetractFactRequest request) {
    return argThat(record -> {
      assertNotNull(record.getId());
      assertNotNull(record.getTypeID());
      assertEquals(request.getFact(), record.getInReferenceToID());
      assertEquals(organization.getId(), record.getOrganizationID());
      assertNotNull(record.getAddedByID());
      assertNotNull(record.getLastSeenByID());
      assertEquals(origin.getId(), record.getOriginID());
      assertTrue(record.getTrust() > 0.0);
      assertEquals(request.getConfidence(), record.getConfidence(), 0.0);
      assertEquals(request.getAccessMode().name(), record.getAccessMode().name());
      assertTrue(record.getTimestamp() > 0);
      assertTrue(record.getLastSeenTimestamp() > 0);
      assertEquals(record.getTimestamp(), record.getLastSeenTimestamp());
      assertTrue(record.isSet(FactRecord.Flag.TimeGlobalIndex));
      return true;
    });
  }
}
