package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.request.v1.AccessMode;
import no.mnemonic.act.platform.api.request.v1.RetractFactRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiServiceEvent;
import no.mnemonic.act.platform.service.ti.helpers.FactCreateHelper;
import no.mnemonic.act.platform.service.ti.helpers.FactStorageHelper;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeResolver;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Objects;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FactRetractDelegateTest extends AbstractDelegateTest {

  @Mock
  private FactTypeResolver factTypeResolver;
  @Mock
  private FactCreateHelper factCreateHelper;
  @Mock
  private FactStorageHelper factStorageHelper;

  private FactRetractDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = new FactRetractDelegate(
            getSecurityContext(),
            getTriggerContext(),
            getFactManager(),
            factTypeResolver,
            factCreateHelper,
            factStorageHelper,
            getFactConverter()
    );
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testRetractFactNotExists() throws Exception {
    delegate.handle(createRetractRequest());
  }

  @Test(expected = AccessDeniedException.class)
  public void testRetractFactNoAccessToFact() throws Exception {
    RetractFactRequest request = mockRetractingFact();
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(isA(FactEntity.class));

    delegate.handle(request);
  }

  @Test(expected = AccessDeniedException.class)
  public void testRetractFactWithoutAddPermission() throws Exception {
    RetractFactRequest request = mockRetractingFact();
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.addFactObjects, request.getOrganization());

    delegate.handle(request);
  }

  @Test
  public void testRetractFactWithLessRestrictiveAccessMode() throws Exception {
    RetractFactRequest request = mockRetractingFact().setAccessMode(AccessMode.Public);

    try {
      delegate.handle(request);
      fail();
    } catch (InvalidArgumentException ignored) {
      verify(getFactManager()).getFact(request.getFact());
      verifyNoMoreInteractions(getFactManager()); // Nothing should be saved to Cassandra.
    }
  }

  @Test
  public void testRetractFact() throws Exception {
    RetractFactRequest request = mockRetractingFact();

    delegate.handle(request);

    verify(getFactManager()).saveFact(matchFactEntity(request));
    verify(factStorageHelper).saveInitialAclForNewFact(matchFactEntity(request), eq(request.getAcl()));
    verify(factStorageHelper).saveCommentForFact(matchFactEntity(request), eq(request.getComment()));
    verify(getFactConverter(), times(2)).apply(matchFactEntity(request));
  }

  @Test
  public void testRetractFactSetMissingOrganization() throws Exception {
    UUID organizationID = UUID.randomUUID();
    RetractFactRequest request = mockRetractingFact().setOrganization(null);

    when(factCreateHelper.resolveOrganization(isNull(), notNull()))
            .thenReturn(Organization.builder().setId(organizationID).build());

    Fact fact = delegate.handle(request);

    verify(getFactManager()).saveFact(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && organizationID.equals(e.getOrganizationID())));
    verify(getFactConverter()).apply(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && organizationID.equals(e.getOrganizationID())));
  }

  @Test
  public void testRetractFactSetMissingOrigin() throws Exception {
    UUID originID = UUID.randomUUID();
    RetractFactRequest request = mockRetractingFact().setOrigin(null);

    when(factCreateHelper.resolveOrigin(isNull())).thenReturn(new OriginEntity().setId(originID));
    when(factCreateHelper.resolveOrganization(notNull(), notNull()))
            .thenReturn(Organization.builder().setId(request.getOrganization()).build());

    Fact fact = delegate.handle(request);

    verify(getFactManager()).saveFact(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && originID.equals(e.getOriginID())));
    verify(getFactConverter()).apply(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && originID.equals(e.getOriginID())));
  }

  @Test
  public void testRetractFactSetMissingConfidence() throws Exception {
    RetractFactRequest request = mockRetractingFact().setConfidence(null);

    Fact fact = delegate.handle(request);

    verify(getFactManager()).saveFact(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && e.getConfidence() > 0.0));
    verify(getFactConverter()).apply(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && e.getConfidence() > 0.0));
  }

  @Test
  public void testRetractFactSetMissingAccessMode() throws Exception {
    RetractFactRequest request = mockRetractingFact().setAccessMode(null);

    Fact fact = delegate.handle(request);

    verify(getFactManager()).saveFact(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && e.getAccessMode() == no.mnemonic.act.platform.dao.cassandra.entity.AccessMode.RoleBased));
    verify(getFactConverter()).apply(argThat(e -> Objects.equals(e.getId(), fact.getId())
            && e.getAccessMode() == no.mnemonic.act.platform.dao.cassandra.entity.AccessMode.RoleBased));
  }

  @Test
  public void testRetractFactSaveMetaFactBinding() throws Exception {
    RetractFactRequest request = mockRetractingFact();

    delegate.handle(request);

    verify(getFactManager()).saveMetaFactBinding(argThat(binding -> {
      assertNotNull(binding.getFactID());
      assertNotNull(binding.getMetaFactID());
      return true;
    }));
  }

  @Test
  public void testRetractFactIndexesIntoElasticSearch() throws Exception {
    RetractFactRequest request = mockRetractingFact();

    delegate.handle(request);

    verify(getFactSearchManager(), times(2)).indexFact(matchFactDocument(request));
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
    ObjectEntity object = new ObjectEntity()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID());
    FactTypeEntity retractionFactType = new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setName("retractionFact")
            .setDefaultConfidence(0.2f);
    FactEntity factToRetract = new FactEntity()
            .setId(request.getFact())
            .setAccessMode(no.mnemonic.act.platform.dao.cassandra.entity.AccessMode.RoleBased)
            .setBindings(ListUtils.list(new FactEntity.FactObjectBinding().setObjectID(object.getId()).setDirection(Direction.BiDirectional)));

    when(getSecurityContext().getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(factCreateHelper.resolveOrigin(request.getOrigin())).thenReturn(origin);
    when(factCreateHelper.resolveOrganization(request.getOrganization(), origin)).thenReturn(organization);

    // Needed for indexing into ElasticSearch.
    when(getFactSearchManager().getFact(request.getFact())).thenReturn(new FactDocument().setId(request.getFact()));
    when(getObjectManager().getObject(object.getId())).thenReturn(object);
    when(getObjectManager().getObjectType(object.getTypeID())).thenReturn(new ObjectTypeEntity().setId(object.getTypeID()).setName("objectType"));

    when(factTypeResolver.resolveRetractionFactType()).thenReturn(retractionFactType);
    when(factStorageHelper.saveInitialAclForNewFact(any(), any())).thenAnswer(i -> i.getArgument(1));
    when(getFactManager().getFact(request.getFact())).thenReturn(factToRetract);
    when(getFactManager().saveFact(any())).thenAnswer(i -> i.getArgument(0));

    // Mock FactConverter needed for registering TriggerEvent.
    when(getFactConverter().apply(any())).then(i -> {
      FactEntity entity = i.getArgument(0);
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

  private FactEntity matchFactEntity(RetractFactRequest request) {
    return argThat(entity -> {
      // Needed for FactConverter as this will be called both for the retraction Fact and the retracted Fact.
      if (Objects.equals(request.getFact(), entity.getId())) {
        return true;
      }

      assertNotNull(entity.getId());
      assertNotNull(entity.getTypeID());
      assertEquals(request.getFact(), entity.getInReferenceToID());
      assertEquals(request.getOrganization(), entity.getOrganizationID());
      assertNotNull(entity.getAddedByID());
      assertEquals(request.getOrigin(), entity.getOriginID());
      assertTrue(entity.getTrust() > 0.0);
      assertEquals(request.getConfidence(), entity.getConfidence(), 0.0);
      assertEquals(request.getAccessMode().name(), entity.getAccessMode().name());
      assertTrue(entity.getTimestamp() > 0);
      assertTrue(entity.getLastSeenTimestamp() > 0);
      return true;
    });
  }

  private FactDocument matchFactDocument(RetractFactRequest request) {
    return argThat(document -> {
      // Verify that the retracted Fact is updated correctly.
      if (Objects.equals(request.getFact(), document.getId())) {
        assertTrue(document.isRetracted());
        return true;
      }

      // Verify that retraction Fact is index correctly.
      assertNotNull(document.getId());
      assertFalse(document.isRetracted());
      assertNotNull(document.getTypeID());
      assertEquals("retractionFact", document.getTypeName());
      assertEquals(request.getFact(), document.getInReferenceTo());
      assertEquals(request.getOrganization(), document.getOrganizationID());
      assertNotNull(document.getAddedByID());
      assertEquals(request.getOrigin(), document.getOriginID());
      assertTrue(document.getTrust() > 0.0);
      assertEquals(request.getConfidence(), document.getConfidence(), 0.0);
      assertEquals(request.getAccessMode().name(), document.getAccessMode().name());
      assertTrue(document.getTimestamp() > 0);
      assertTrue(document.getLastSeenTimestamp() > 0);
      assertTrue(document.getAcl().size() > 0);
      return true;
    });
  }
}
