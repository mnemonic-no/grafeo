package no.mnemonic.act.platform.service.ti;

import no.mnemonic.services.triggers.pipeline.api.AccessMode;
import no.mnemonic.services.triggers.pipeline.api.TriggerEvent;
import org.junit.Test;

import java.util.UUID;

import static no.mnemonic.act.platform.service.ti.TiServiceEvent.EventName.FactAdded;
import static org.junit.Assert.*;

public class TiServiceEventTest {

  @Test
  public void testCreateTiServiceEventWithAccessModePublic() {
    UUID organizationID = UUID.randomUUID();
    TriggerEvent event = TiServiceEvent.forEvent(FactAdded)
            .setOrganization(organizationID)
            .setAccessMode(no.mnemonic.act.platform.api.model.v1.AccessMode.Public)
            .build();
    assertNotNull(event.getId());
    assertTrue(event.getTimestamp() > 0);
    assertEquals("ThreatIntelligenceService", event.getService());
    assertEquals(FactAdded.name(), event.getEvent());
    assertEquals(organizationID, event.getOrganization());
    assertEquals(AccessMode.Public, event.getAccessMode());
  }

  @Test
  public void testCreateTiServiceEventWithAccessModeRoleBased() {
    UUID organizationID = UUID.randomUUID();
    TriggerEvent event = TiServiceEvent.forEvent(FactAdded)
            .setOrganization(organizationID)
            .setAccessMode(no.mnemonic.act.platform.api.model.v1.AccessMode.RoleBased)
            .build();
    assertNotNull(event.getId());
    assertTrue(event.getTimestamp() > 0);
    assertEquals("ThreatIntelligenceService", event.getService());
    assertEquals(FactAdded.name(), event.getEvent());
    assertEquals(organizationID, event.getOrganization());
    assertEquals(AccessMode.RoleBased, event.getAccessMode());
  }

  @Test
  public void testCreateTiServiceEventWithAccessModeExplicit() {
    UUID organizationID = UUID.randomUUID();
    TriggerEvent event = TiServiceEvent.forEvent(FactAdded)
            .setOrganization(organizationID)
            .setAccessMode(no.mnemonic.act.platform.api.model.v1.AccessMode.Explicit)
            .build();
    assertNotNull(event.getId());
    assertTrue(event.getTimestamp() > 0);
    assertEquals("ThreatIntelligenceService", event.getService());
    assertEquals(FactAdded.name(), event.getEvent());
    assertEquals(organizationID, event.getOrganization());
    assertEquals(AccessMode.Private, event.getAccessMode());
  }

  @Test
  public void testCreateTiServiceEventWithContextParameters() {
    TriggerEvent event = TiServiceEvent.forEvent(FactAdded)
            .setOrganization(UUID.randomUUID())
            .setAccessMode(no.mnemonic.act.platform.api.model.v1.AccessMode.Public)
            .addContextParameter("parameter1", "value")
            .addContextParameter("parameter2", 42)
            .build();
    assertEquals("value", event.getContextParameters().get("parameter1"));
    assertEquals(42, event.getContextParameters().get("parameter2"));
  }

  @Test(expected = RuntimeException.class)
  public void testCreateTiServiceEventWithoutEvent() {
    TiServiceEvent.forEvent(null)
            .setOrganization(UUID.randomUUID())
            .setAccessMode(no.mnemonic.act.platform.api.model.v1.AccessMode.Public)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateTiServiceEventWithoutOrganization() {
    TiServiceEvent.forEvent(FactAdded)
            .setAccessMode(no.mnemonic.act.platform.api.model.v1.AccessMode.Public)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateTiServiceEventWithoutAccessMode() {
    TiServiceEvent.forEvent(FactAdded)
            .setOrganization(UUID.randomUUID())
            .build();
  }

}
