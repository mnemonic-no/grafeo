package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.AccessMode;
import no.mnemonic.act.platform.api.request.v1.RetractFactRequest;
import no.mnemonic.act.platform.entity.cassandra.Direction;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.act.platform.entity.cassandra.FactTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.helpers.FactStorageHelper;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeResolver;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static no.mnemonic.commons.testtools.MockitoTools.match;
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

    verify(getFactManager()).saveFact(match(e -> organizationID.equals(e.getOrganizationID())));
    verify(getFactConverter()).apply(match(e -> organizationID.equals(e.getOrganizationID())));
  }

  @Test
  public void testRetractFactSetMissingSource() throws Exception {
    UUID sourceID = UUID.randomUUID();
    RetractFactRequest request = mockRetractingFact().setSource(null);

    when(getSecurityContext().getCurrentUserID()).thenReturn(sourceID);

    delegate.handle(request);

    verify(getFactManager()).saveFact(match(e -> sourceID.equals(e.getSourceID())));
    verify(getFactConverter()).apply(match(e -> sourceID.equals(e.getSourceID())));
  }

  @Test
  public void testRetractFactSetMissingAccessMode() throws Exception {
    RetractFactRequest request = mockRetractingFact().setAccessMode(null);

    delegate.handle(request);

    verify(getFactManager()).saveFact(match(e -> e.getAccessMode() == no.mnemonic.act.platform.entity.cassandra.AccessMode.Public));
    verify(getFactConverter()).apply(match(e -> e.getAccessMode() == no.mnemonic.act.platform.entity.cassandra.AccessMode.Public));
  }

  @Test
  public void testRetractFactSaveObjectFactBinding() throws Exception {
    RetractFactRequest request = mockRetractingFact();

    delegate.handle(request);

    verify(getObjectManager()).saveObjectFactBinding(match(binding -> {
      assertNotNull(binding.getFactID());
      assertNotNull(binding.getObjectID());
      assertEquals(Direction.None, binding.getDirection());
      return true;
    }));
  }

  private RetractFactRequest mockRetractingFact() throws Exception {
    RetractFactRequest request = crateRetractRequest();

    FactEntity factToRetract = new FactEntity()
            .setId(request.getFact())
            .setAccessMode(no.mnemonic.act.platform.entity.cassandra.AccessMode.Public)
            .setBindings(ListUtils.list(new FactEntity.FactObjectBinding().setObjectID(UUID.randomUUID())));

    when(factTypeResolver.resolveRetractionFactType()).thenReturn(new FactTypeEntity().setId(UUID.randomUUID()));
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
            .setAccessMode(AccessMode.RoleBased)
            .setAcl(ListUtils.list(UUID.randomUUID()));
  }

  private FactEntity matchFactEntity(RetractFactRequest request) {
    return match(entity -> {
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

}
