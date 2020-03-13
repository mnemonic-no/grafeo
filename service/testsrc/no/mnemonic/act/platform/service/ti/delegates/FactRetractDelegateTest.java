package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.request.v1.AccessMode;
import no.mnemonic.act.platform.api.request.v1.RetractFactRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.api.record.FactCommentRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiServiceEvent;
import no.mnemonic.act.platform.service.ti.converters.FactConverter;
import no.mnemonic.act.platform.service.ti.handlers.FactCreateHandler;
import no.mnemonic.act.platform.service.ti.resolvers.FactResolver;
import no.mnemonic.act.platform.service.ti.resolvers.FactTypeResolver;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Objects;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FactRetractDelegateTest extends AbstractDelegateTest {

  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private FactTypeResolver factTypeResolver;
  @Mock
  private FactResolver factResolver;
  @Mock
  private FactCreateHandler factCreateHandler;
  @Mock
  private FactConverter factConverter;

  private FactRetractDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = new FactRetractDelegate(
            getSecurityContext(),
            getTriggerContext(),
            objectFactDao,
            factTypeResolver,
            factResolver,
      factCreateHandler,
            factConverter
    );
  }

  @Test(expected = AccessDeniedException.class)
  public void testRetractFactNoAccessToFact() throws Exception {
    RetractFactRequest request = mockRetractingFact();
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(isA(FactRecord.class));

    delegate.handle(request);
  }

  @Test(expected = AccessDeniedException.class)
  public void testRetractFactWithoutAddPermission() throws Exception {
    RetractFactRequest request = mockRetractingFact();
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.addFactObjects, request.getOrganization());

    delegate.handle(request);
  }

  @Test
  public void testRetractFact() throws Exception {
    RetractFactRequest request = mockRetractingFact();

    delegate.handle(request);

    verify(objectFactDao).storeFact(matchFactRecord(request));
    verify(objectFactDao).retractFact(argThat(e -> Objects.equals(e.getId(), request.getFact())));
    verify(factConverter, times(2)).apply(matchFactRecord(request));
  }

  @Test
  public void testRetractFactSetMissingOrganization() throws Exception {
    UUID organizationID = UUID.randomUUID();
    RetractFactRequest request = mockRetractingFact().setOrganization(null);

    when(factCreateHandler.resolveOrganization(isNull(), notNull()))
            .thenReturn(Organization.builder().setId(organizationID).build());

    Fact fact = delegate.handle(request);

    verify(objectFactDao).storeFact(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && organizationID.equals(e.getOrganizationID())));
    verify(factConverter).apply(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && organizationID.equals(e.getOrganizationID())));
  }

  @Test
  public void testRetractFactSetMissingOrigin() throws Exception {
    UUID originID = UUID.randomUUID();
    RetractFactRequest request = mockRetractingFact().setOrigin(null);

    when(factCreateHandler.resolveOrigin(isNull())).thenReturn(new OriginEntity().setId(originID));
    when(factCreateHandler.resolveOrganization(notNull(), notNull()))
            .thenReturn(Organization.builder().setId(request.getOrganization()).build());

    Fact fact = delegate.handle(request);

    verify(objectFactDao).storeFact(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && originID.equals(e.getOriginID())));
    verify(factConverter).apply(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && originID.equals(e.getOriginID())));
  }

  @Test
  public void testRetractFactSetMissingConfidence() throws Exception {
    RetractFactRequest request = mockRetractingFact().setConfidence(null);

    Fact fact = delegate.handle(request);

    verify(objectFactDao).storeFact(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && e.getConfidence() > 0.0));
    verify(factConverter).apply(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && e.getConfidence() > 0.0));
  }

  @Test
  public void testRetractFactSetMissingAccessMode() throws Exception {
    RetractFactRequest request = mockRetractingFact().setAccessMode(null);

    when(factCreateHandler.resolveAccessMode(notNull(), isNull())).thenReturn(FactRecord.AccessMode.RoleBased);

    Fact fact = delegate.handle(request);

    verify(objectFactDao).storeFact(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && e.getAccessMode() == FactRecord.AccessMode.RoleBased));
    verify(factConverter).apply(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && e.getAccessMode() == FactRecord.AccessMode.RoleBased));
  }

  @Test
  public void testRetractFactStoresAclAndComment() throws Exception {
    RetractFactRequest request = mockRetractingFact();

    delegate.handle(request);

    verify(objectFactDao).storeFact(argThat(record -> {
      assertEquals(set(record.getComments(), FactCommentRecord::getComment), set(request.getComment()));
      assertTrue(list(record.getAcl(), FactAclEntryRecord::getSubjectID).containsAll(request.getAcl()));
      return true;
    }));
  }

  @Test
  public void testRetractFactRegistersTriggerEvent() throws Exception {
    RetractFactRequest request = mockRetractingFact();

    Fact retractionFact = delegate.handle(request);

    verify(getTriggerContext()).registerTriggerEvent(argThat(event -> {
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

    OriginEntity origin = new OriginEntity()
            .setId(request.getOrigin())
            .setName("origin")
            .setTrust(0.1f);
    Organization organization = Organization.builder()
            .setId(request.getOrganization())
            .setName("organization")
            .build();
    FactTypeEntity retractionFactType = new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setName("retractionFact")
            .setDefaultConfidence(0.2f);
    FactRecord factToRetract = new FactRecord()
            .setId(request.getFact())
            .setAccessMode(FactRecord.AccessMode.RoleBased);

    when(getSecurityContext().getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(factCreateHandler.resolveOrigin(request.getOrigin())).thenReturn(origin);
    when(factCreateHandler.resolveOrganization(request.getOrganization(), origin)).thenReturn(organization);

    when(factTypeResolver.resolveRetractionFactType()).thenReturn(retractionFactType);
    // Mock fetching of Fact to retract.
    when(factResolver.resolveFact(request.getFact())).thenReturn(factToRetract);
    when(factCreateHandler.resolveAccessMode(eq(factToRetract), any())).thenReturn(FactRecord.AccessMode.Explicit);
    // Mock stuff needed for saving Facts.
    when(objectFactDao.storeFact(any())).thenAnswer(i -> i.getArgument(0));
    when(objectFactDao.retractFact(any())).thenAnswer(i -> i.getArgument(0));

    // Mock FactConverter needed for registering TriggerEvent.
    when(factConverter.apply(any())).then(i -> {
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
            .setOrganization(UUID.randomUUID())
            .setOrigin(UUID.randomUUID())
            .setConfidence(0.3f)
            .setComment("Hello World!")
            .setAccessMode(AccessMode.Explicit)
            .setAcl(ListUtils.list(UUID.randomUUID()));
  }

  private FactRecord matchFactRecord(RetractFactRequest request) {
    return argThat(record -> {
      // Needed for FactConverter as this will be called both for the retraction Fact and the retracted Fact.
      if (Objects.equals(request.getFact(), record.getId())) {
        return true;
      }

      assertNotNull(record.getId());
      assertNotNull(record.getTypeID());
      assertEquals(request.getFact(), record.getInReferenceToID());
      assertEquals(request.getOrganization(), record.getOrganizationID());
      assertNotNull(record.getAddedByID());
      assertEquals(request.getOrigin(), record.getOriginID());
      assertTrue(record.getTrust() > 0.0);
      assertEquals(request.getConfidence(), record.getConfidence(), 0.0);
      assertEquals(request.getAccessMode().name(), record.getAccessMode().name());
      assertTrue(record.getTimestamp() > 0);
      assertTrue(record.getLastSeenTimestamp() > 0);
      return true;
    });
  }
}
