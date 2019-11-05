package no.mnemonic.act.platform.dao.elastic;

import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

class DocumentTestUtils {

  static ObjectDocument createObjectDocument() {
    return new ObjectDocument()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("objectValue")
            .setDirection(ObjectDocument.Direction.BiDirectional);
  }

  static FactDocument createFactDocument() {
    return new FactDocument()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("factValue")
            .setInReferenceTo(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setAddedByID(UUID.randomUUID())
            .setAccessMode(FactDocument.AccessMode.Public)
            .setConfidence(0.1f)
            .setTrust(0.2f)
            .setTimestamp(123456789)
            .setLastSeenTimestamp(987654321)
            .addAclEntry(UUID.randomUUID())
            .addObject(createObjectDocument());
  }

  static void assertObjectDocument(ObjectDocument expected, ObjectDocument actual) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getTypeID(), actual.getTypeID());
    assertEquals(expected.getValue(), actual.getValue());
    assertEquals(expected.getDirection(), actual.getDirection());
  }

  static void assertFactDocument(FactDocument expected, FactDocument actual) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getTypeID(), actual.getTypeID());
    assertEquals(expected.getValue(), actual.getValue());
    assertEquals(expected.getInReferenceTo(), actual.getInReferenceTo());
    assertEquals(expected.getOrganizationID(), actual.getOrganizationID());
    assertEquals(expected.getOriginID(), actual.getOriginID());
    assertEquals(expected.getAddedByID(), actual.getAddedByID());
    assertEquals(expected.getAccessMode(), actual.getAccessMode());
    assertEquals(expected.getConfidence(), actual.getConfidence(), 0);
    assertEquals(expected.getTrust(), actual.getTrust(), 0);
    assertEquals(expected.getCertainty(), actual.getCertainty(), 0);
    assertEquals(expected.getTimestamp(), actual.getTimestamp());
    assertEquals(expected.getLastSeenTimestamp(), actual.getLastSeenTimestamp());
    assertEquals(expected.getAcl(), actual.getAcl());
    assertEquals(expected.getObjectCount(), actual.getObjectCount());

    assertObjectDocument(expected.getObjects().iterator().next(), actual.getObjects().iterator().next());
  }

}
