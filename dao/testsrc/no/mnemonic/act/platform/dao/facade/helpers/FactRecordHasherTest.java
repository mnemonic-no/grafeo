package no.mnemonic.act.platform.dao.facade.helpers;

import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class FactRecordHasherTest {

  @Test
  public void testToHashThrowsExceptionOnNull() {
    assertThrows(IllegalArgumentException.class, () -> FactRecordHasher.toHash(null));
  }

  @Test
  public void testToHashEmpty() {
    assertEquals("e7e32aa85b924cc7e8bf1b2dba4db25eecdcd14bfba1d5338b658da1845e4989", FactRecordHasher.toHash(new FactRecord()));
  }

  @Test
  public void testToHashFull() {
    assertEquals("0a05757c48d9923d0844c79e585dc5fb3ab998addf55ead515ecb97c769dfed3", FactRecordHasher.toHash(createFactRecord()));
  }

  @Test
  public void testToHashWithEqualFactRecord() {
    FactRecord record1 = createFactRecord();
    FactRecord record2 = createFactRecord();
    assertNotSame(record1, record2);
    assertEquals(FactRecordHasher.toHash(record1), FactRecordHasher.toHash(record2));
  }

  @Test
  public void testToHashWithDifferentTypeID() {
    String hash1 = FactRecordHasher.toHash(createFactRecord().setTypeID(UUID.randomUUID()));
    String hash2 = FactRecordHasher.toHash(createFactRecord().setTypeID(UUID.randomUUID()));
    assertNotEquals(hash1, hash2);
  }

  @Test
  public void testToHashWithDifferentOriginID() {
    String hash1 = FactRecordHasher.toHash(createFactRecord().setOriginID(UUID.randomUUID()));
    String hash2 = FactRecordHasher.toHash(createFactRecord().setOriginID(UUID.randomUUID()));
    assertNotEquals(hash1, hash2);
  }

  @Test
  public void testToHashWithDifferentOrganizationID() {
    String hash1 = FactRecordHasher.toHash(createFactRecord().setOrganizationID(UUID.randomUUID()));
    String hash2 = FactRecordHasher.toHash(createFactRecord().setOrganizationID(UUID.randomUUID()));
    assertNotEquals(hash1, hash2);
  }

  @Test
  public void testToHashWithDifferentAccessMode() {
    String hash1 = FactRecordHasher.toHash(createFactRecord().setAccessMode(FactRecord.AccessMode.Public));
    String hash2 = FactRecordHasher.toHash(createFactRecord().setAccessMode(FactRecord.AccessMode.RoleBased));
    assertNotEquals(hash1, hash2);
  }

  @Test
  public void testToHashWithDifferentConfidence() {
    String hash1 = FactRecordHasher.toHash(createFactRecord().setConfidence(0.22f));
    String hash2 = FactRecordHasher.toHash(createFactRecord().setConfidence(0.23f));
    assertNotEquals(hash1, hash2);
  }

  @Test
  public void testToHashWithConfidenceRoundedToSameValue() {
    String hash1 = FactRecordHasher.toHash(createFactRecord().setConfidence(0.202f));
    String hash2 = FactRecordHasher.toHash(createFactRecord().setConfidence(0.203f));
    assertEquals(hash1, hash2);
  }

  @Test
  public void testToHashWithDifferentInReferenceToID() {
    String hash1 = FactRecordHasher.toHash(createFactRecord().setInReferenceToID(UUID.randomUUID()));
    String hash2 = FactRecordHasher.toHash(createFactRecord().setInReferenceToID(UUID.randomUUID()));
    assertNotEquals(hash1, hash2);
  }

  @Test
  public void testToHashWithoutInReferenceToID() {
    String hash1 = FactRecordHasher.toHash(createFactRecord().setInReferenceToID(null));
    String hash2 = FactRecordHasher.toHash(createFactRecord().setInReferenceToID(null));
    assertEquals(hash1, hash2);
  }

  @Test
  public void testToHashWithDifferentSourceObjectID() {
    String hash1 = FactRecordHasher.toHash(createFactRecord().setSourceObject(new ObjectRecord().setId(UUID.randomUUID())));
    String hash2 = FactRecordHasher.toHash(createFactRecord().setSourceObject(new ObjectRecord().setId(UUID.randomUUID())));
    assertNotEquals(hash1, hash2);
  }

  @Test
  public void testToHashWithDifferentDestinationObjectID() {
    String hash1 = FactRecordHasher.toHash(createFactRecord().setDestinationObject(new ObjectRecord().setId(UUID.randomUUID())));
    String hash2 = FactRecordHasher.toHash(createFactRecord().setDestinationObject(new ObjectRecord().setId(UUID.randomUUID())));
    assertNotEquals(hash1, hash2);
  }

  @Test
  public void testToHashWithoutObjects() {
    String hash1 = FactRecordHasher.toHash(createFactRecord().setSourceObject(null).setDestinationObject(null));
    String hash2 = FactRecordHasher.toHash(createFactRecord().setSourceObject(null).setDestinationObject(null));
    assertEquals(hash1, hash2);
  }

  @Test
  public void testToHashWithDifferentBidirectionalBinding() {
    String hash1 = FactRecordHasher.toHash(createFactRecord().setBidirectionalBinding(true));
    String hash2 = FactRecordHasher.toHash(createFactRecord().setBidirectionalBinding(false));
    assertNotEquals(hash1, hash2);
  }

  @Test
  public void testToHashWithDifferentValue() {
    String hash1 = FactRecordHasher.toHash(createFactRecord().setValue("haha"));
    String hash2 = FactRecordHasher.toHash(createFactRecord().setValue("HAHA"));
    assertNotEquals(hash1, hash2);
  }

  @Test
  public void testToHashWithNullValue() {
    String hash1 = FactRecordHasher.toHash(createFactRecord().setValue(null));
    String hash2 = FactRecordHasher.toHash(createFactRecord().setValue("null"));
    String hash3 = FactRecordHasher.toHash(createFactRecord().setValue("NULL"));
    assertNotEquals(hash1, hash2);
    assertNotEquals(hash1, hash3);
    assertNotEquals(hash2, hash3);
  }

  @Test
  public void testToHashWithEmptyValue() {
    String hash1 = FactRecordHasher.toHash(createFactRecord().setValue(null));
    String hash2 = FactRecordHasher.toHash(createFactRecord().setValue(""));
    assertEquals(hash1, hash2);
  }

  @Test
  public void testFormatFactRecordEmpty() {
    String emptyFactRecordString = "typeID=NULL;" +
            "originID=NULL;" +
            "organizationID=NULL;" +
            "accessMode=NULL;" +
            "confidence=0.00;" +
            "inReferenceToID=NULL;" +
            "sourceObjectID=NULL;" +
            "destinationObjectID=NULL;" +
            "isBidirectionalBinding=false;" +
            "value=";
    assertEquals(emptyFactRecordString, FactRecordHasher.formatFactRecord(new FactRecord()));
  }

  @Test
  public void testFormatFactRecordFull() {
    String fullFactRecordString = "typeID=00000000-0000-0000-0000-000000000002;" +
            "originID=00000000-0000-0000-0000-000000000004;" +
            "organizationID=00000000-0000-0000-0000-000000000005;" +
            "accessMode=Explicit;" +
            "confidence=0.10;" +
            "inReferenceToID=00000000-0000-0000-0000-000000000003;" +
            "sourceObjectID=00000000-0000-0000-0000-000000000007;" +
            "destinationObjectID=00000000-0000-0000-0000-000000000008;" +
            "isBidirectionalBinding=true;" +
            "value=value";
    assertEquals(fullFactRecordString, FactRecordHasher.formatFactRecord(createFactRecord()));
  }

  @Test
  public void testFormatFloatingPoint() {
    assertEquals("0.00", FactRecordHasher.formatFloatingPoint(0));
    assertEquals("0.00", FactRecordHasher.formatFloatingPoint(0.001f));
    assertEquals("0.01", FactRecordHasher.formatFloatingPoint(0.01f));
    assertEquals("0.10", FactRecordHasher.formatFloatingPoint(0.1f));
    assertEquals("0.90", FactRecordHasher.formatFloatingPoint(0.9f));
    assertEquals("0.99", FactRecordHasher.formatFloatingPoint(0.991f));
    assertEquals("1.00", FactRecordHasher.formatFloatingPoint(0.999f));
    assertEquals("1.00", FactRecordHasher.formatFloatingPoint(1));
  }

  private FactRecord createFactRecord() {
    return new FactRecord()
            .setId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .setTypeID(UUID.fromString("00000000-0000-0000-0000-000000000002"))
            .setValue("value")
            .setInReferenceToID(UUID.fromString("00000000-0000-0000-0000-000000000003"))
            .setOriginID(UUID.fromString("00000000-0000-0000-0000-000000000004"))
            .setOrganizationID(UUID.fromString("00000000-0000-0000-0000-000000000005"))
            .setAddedByID(UUID.fromString("00000000-0000-0000-0000-000000000006"))
            .setAccessMode(FactRecord.AccessMode.Explicit)
            .setConfidence(0.1f)
            .setTrust(0.2f)
            .setTimestamp(123456789)
            .setLastSeenTimestamp(987654321)
            .setSourceObject(new ObjectRecord().setId(UUID.fromString("00000000-0000-0000-0000-000000000007")))
            .setDestinationObject(new ObjectRecord().setId(UUID.fromString("00000000-0000-0000-0000-000000000008")))
            .setBidirectionalBinding(true);
  }
}
