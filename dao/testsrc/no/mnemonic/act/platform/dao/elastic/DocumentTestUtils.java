package no.mnemonic.act.platform.dao.elastic;

import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

class DocumentTestUtils {

  static ObjectDocument createObjectDocument() {
    return new ObjectDocument()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("objectValue")
            .setDirection(ObjectDocument.Direction.BiDirectional);
  }

  static FactDocument createFactDocument(long indexTimestamp) {
    return new FactDocument()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("factValue")
            .setInReferenceTo(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setAddedByID(UUID.randomUUID())
            .setLastSeenByID(UUID.randomUUID())
            .setAccessMode(FactDocument.AccessMode.Public)
            .setConfidence(0.1f)
            .setTrust(0.2f)
            .setTimestamp(indexTimestamp)
            .setLastSeenTimestamp(indexTimestamp)
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
    assertEquals(expected.getLastSeenByID(), actual.getLastSeenByID());
    assertEquals(expected.getAccessMode(), actual.getAccessMode());
    assertEquals(expected.getConfidence(), actual.getConfidence(), 0);
    assertEquals(expected.getTrust(), actual.getTrust(), 0);
    assertEquals(expected.getCertainty(), actual.getCertainty(), 0);
    assertEquals(expected.getTimestamp(), actual.getTimestamp());
    assertEquals(expected.getLastSeenTimestamp(), actual.getLastSeenTimestamp());
    assertEquals(expected.getAcl(), actual.getAcl());
    assertEquals(expected.getObjectCount(), actual.getObjectCount());

    expected.getObjects().forEach(o -> {
      Optional<ObjectDocument> actualMatch = actual.getObjects()
              .stream()
              .filter(x -> Objects.equals(x.getId(), o.getId()))
              .findFirst();
      assertTrue(actualMatch.isPresent());
      assertObjectDocument(o, actualMatch.get());
    });
  }
}
