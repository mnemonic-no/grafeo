package no.mnemonic.services.grafeo.service.implementation;

import no.mnemonic.services.triggers.pipeline.api.AccessMode;
import no.mnemonic.services.triggers.pipeline.api.TriggerEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static no.mnemonic.services.grafeo.service.implementation.GrafeoServiceEvent.EventName.FactAdded;
import static org.junit.jupiter.api.Assertions.*;

public class GrafeoServiceEventTest {

  @Test
  public void testCreateServiceEventWithAccessModePublic() {
    UUID organizationID = UUID.randomUUID();
    TriggerEvent event = GrafeoServiceEvent.forEvent(FactAdded)
            .setOrganization(organizationID)
            .setAccessMode(no.mnemonic.services.grafeo.api.model.v1.AccessMode.Public)
            .build();
    assertNotNull(event.getId());
    assertTrue(event.getTimestamp() > 0);
    assertEquals("GrafeoService", event.getService());
    assertEquals(FactAdded.name(), event.getEvent());
    assertEquals(organizationID, event.getOrganization());
    assertEquals(AccessMode.Public, event.getAccessMode());
  }

  @Test
  public void testCreateServiceEventWithAccessModeRoleBased() {
    UUID organizationID = UUID.randomUUID();
    TriggerEvent event = GrafeoServiceEvent.forEvent(FactAdded)
            .setOrganization(organizationID)
            .setAccessMode(no.mnemonic.services.grafeo.api.model.v1.AccessMode.RoleBased)
            .build();
    assertNotNull(event.getId());
    assertTrue(event.getTimestamp() > 0);
    assertEquals("GrafeoService", event.getService());
    assertEquals(FactAdded.name(), event.getEvent());
    assertEquals(organizationID, event.getOrganization());
    assertEquals(AccessMode.RoleBased, event.getAccessMode());
  }

  @Test
  public void testCreateServiceEventWithAccessModeExplicit() {
    UUID organizationID = UUID.randomUUID();
    TriggerEvent event = GrafeoServiceEvent.forEvent(FactAdded)
            .setOrganization(organizationID)
            .setAccessMode(no.mnemonic.services.grafeo.api.model.v1.AccessMode.Explicit)
            .build();
    assertNotNull(event.getId());
    assertTrue(event.getTimestamp() > 0);
    assertEquals("GrafeoService", event.getService());
    assertEquals(FactAdded.name(), event.getEvent());
    assertEquals(organizationID, event.getOrganization());
    assertEquals(AccessMode.Private, event.getAccessMode());
  }

  @Test
  public void testCreateServiceEventWithContextParameters() {
    TriggerEvent event = GrafeoServiceEvent.forEvent(FactAdded)
            .setOrganization(UUID.randomUUID())
            .setAccessMode(no.mnemonic.services.grafeo.api.model.v1.AccessMode.Public)
            .addContextParameter("parameter1", "value")
            .addContextParameter("parameter2", 42)
            .build();
    assertEquals("value", event.getContextParameters().get("parameter1"));
    assertEquals(42, event.getContextParameters().get("parameter2"));
  }

  @Test
  public void testCreateServiceEventWithoutEvent() {
    assertThrows(RuntimeException.class, () -> GrafeoServiceEvent.forEvent(null)
            .setOrganization(UUID.randomUUID())
            .setAccessMode(no.mnemonic.services.grafeo.api.model.v1.AccessMode.Public)
            .build());
  }

  @Test
  public void testCreateServiceEventWithoutOrganization() {
    assertThrows(RuntimeException.class, () -> GrafeoServiceEvent.forEvent(FactAdded)
            .setAccessMode(no.mnemonic.services.grafeo.api.model.v1.AccessMode.Public)
            .build());
  }

  @Test
  public void testCreateServiceEventWithoutAccessMode() {
    assertThrows(RuntimeException.class, () -> GrafeoServiceEvent.forEvent(FactAdded)
            .setOrganization(UUID.randomUUID())
            .build());
  }

}
