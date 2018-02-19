package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.AccessMode;
import no.mnemonic.act.platform.api.request.v1.RetractFactRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
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
  private FactStorageHelper factStorageHelper;

  private FactRetractDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = FactRetractDelegate.builder()
            .setFactTypeResolver(factTypeResolver)
            .setFactStorageHelper(factStorageHelper)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateDelegateWithoutFactTypeResolver() {
    FactRetractDelegate.builder().setFactStorageHelper(factStorageHelper).build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateDelegateWithoutFactStorageHelper() {
    FactRetractDelegate.builder().setFactTypeResolver(factTypeResolver).build();
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testRetractFactNotExists() throws Exception {
    delegate.handle(crateRetractRequest());
  }

  @Test(expected = AccessDeniedException.class)
  public void testRetractFactNoAccessToFact() throws Exception {
    RetractFactRequest request = mockRetractingFact();
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(any());

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
    verify(getFactConverter()).apply(matchFactEntity(request));
  }

  @Test
  public void testRetractFactSetMissingOrganization() throws Exception {
    UUID organizationID = UUID.randomUUID();
    RetractFactRequest request = mockRetractingFact().setOrganization(null);

    when(getSecurityContext().getCurrentUserOrganizationID()).thenReturn(organizationID);

    delegate.handle(request);

    verify(getFactManager()).saveFact(argThat(e -> organizationID.equals(e.getOrganizationID())));
    verify(getFactConverter()).apply(argThat(e -> organizationID.equals(e.getOrganizationID())));
  }

  @Test
  public void testRetractFactSetMissingSource() throws Exception {
    UUID sourceID = UUID.randomUUID();
    RetractFactRequest request = mockRetractingFact().setSource(null);

    when(getSecurityContext().getCurrentUserID()).thenReturn(sourceID);

    delegate.handle(request);

    verify(getFactManager()).saveFact(argThat(e -> sourceID.equals(e.getSourceID())));
    verify(getFactConverter()).apply(argThat(e -> sourceID.equals(e.getSourceID())));
  }

  @Test
  public void testRetractFactSetMissingAccessMode() throws Exception {
    RetractFactRequest request = mockRetractingFact().setAccessMode(null);

    delegate.handle(request);

    verify(getFactManager()).saveFact(argThat(e -> e.getAccessMode() == no.mnemonic.act.platform.dao.cassandra.entity.AccessMode.RoleBased));
    verify(getFactConverter()).apply(argThat(e -> e.getAccessMode() == no.mnemonic.act.platform.dao.cassandra.entity.AccessMode.RoleBased));
  }

  @Test
  public void testRetractFactSaveObjectFactBinding() throws Exception {
    RetractFactRequest request = mockRetractingFact();

    delegate.handle(request);

    verify(getObjectManager()).saveObjectFactBinding(argThat(binding -> {
      assertNotNull(binding.getFactID());
      assertNotNull(binding.getObjectID());
      assertEquals(Direction.None, binding.getDirection());
      return true;
    }));
  }

  @Test
  public void testRetractFactIndexesIntoElasticSearch() throws Exception {
    RetractFactRequest request = mockRetractingFact();

    delegate.handle(request);

    verify(getFactSearchManager(), times(2)).indexFact(matchFactDocument(request));
  }

  private RetractFactRequest mockRetractingFact() {
    RetractFactRequest request = crateRetractRequest();

    ObjectEntity object = new ObjectEntity()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID());
    FactEntity factToRetract = new FactEntity()
            .setId(request.getFact())
            .setAccessMode(no.mnemonic.act.platform.dao.cassandra.entity.AccessMode.RoleBased)
            .setBindings(ListUtils.list(new FactEntity.FactObjectBinding().setObjectID(object.getId()).setDirection(Direction.None)));

    // Needed for indexing into ElasticSearch.
    when(getFactSearchManager().getFact(request.getFact())).thenReturn(new FactDocument().setId(request.getFact()));
    when(getObjectManager().getObject(object.getId())).thenReturn(object);
    when(getObjectManager().getObjectType(object.getTypeID())).thenReturn(new ObjectTypeEntity().setId(object.getTypeID()).setName("objectType"));

    when(factTypeResolver.resolveRetractionFactType()).thenReturn(new FactTypeEntity().setId(UUID.randomUUID()).setName("retractionFact"));
    when(factStorageHelper.saveInitialAclForNewFact(any(), any())).thenAnswer(i -> i.getArgument(1));
    when(getFactManager().getFact(request.getFact())).thenReturn(factToRetract);
    when(getFactManager().saveFact(any())).thenAnswer(i -> i.getArgument(0));

    return request;
  }

  private RetractFactRequest crateRetractRequest() {
    return new RetractFactRequest()
            .setFact(UUID.randomUUID())
            .setOrganization(UUID.randomUUID())
            .setSource(UUID.randomUUID())
            .setComment("Hello World!")
            .setAccessMode(AccessMode.Explicit)
            .setAcl(ListUtils.list(UUID.randomUUID()));
  }

  private FactEntity matchFactEntity(RetractFactRequest request) {
    return argThat(entity -> {
      assertNotNull(entity.getId());
      assertNotNull(entity.getTypeID());
      assertNotNull(entity.getValue());
      assertEquals(request.getFact(), entity.getInReferenceToID());
      assertEquals(request.getOrganization(), entity.getOrganizationID());
      assertEquals(request.getSource(), entity.getSourceID());
      assertEquals(request.getAccessMode().name(), entity.getAccessMode().name());
      assertTrue(entity.getBindings().size() > 0);
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
      assertNotNull(document.getValue());
      assertEquals(request.getFact(), document.getInReferenceTo());
      assertEquals(request.getOrganization(), document.getOrganizationID());
      assertEquals(request.getSource(), document.getSourceID());
      assertEquals(request.getAccessMode().name(), document.getAccessMode().name());
      assertTrue(document.getTimestamp() > 0);
      assertTrue(document.getLastSeenTimestamp() > 0);
      assertTrue(document.getAcl().size() > 0);
      assertTrue(document.getObjects().size() > 0);
      return true;
    });
  }

}
