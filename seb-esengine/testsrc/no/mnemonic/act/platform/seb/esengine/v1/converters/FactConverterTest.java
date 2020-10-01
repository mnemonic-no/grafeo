package no.mnemonic.act.platform.seb.esengine.v1.converters;

import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.seb.model.v1.*;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import java.util.Objects;
import java.util.UUID;

import static org.junit.Assert.*;

public class FactConverterTest {

  private final FactConverter converter = new FactConverter();

  @Test
  public void testConvertNull() {
    assertNull(converter.apply(null));
  }

  @Test
  public void testConvertEmpty() {
    assertNotNull(converter.apply(FactSEB.builder().build()));
  }

  @Test
  public void testConvertFull() {
    FactSEB seb = FactSEB.builder()
            .setId(UUID.randomUUID())
            .setType(FactTypeInfoSEB.builder().setId(UUID.randomUUID()).build())
            .setValue("value")
            .setInReferenceTo(FactInfoSEB.builder().setId(UUID.randomUUID()).build())
            .setOrganization(OrganizationInfoSEB.builder().setId(UUID.randomUUID()).build())
            .setOrigin(OriginInfoSEB.builder().setId(UUID.randomUUID()).build())
            .setAddedBy(SubjectInfoSEB.builder().setId(UUID.randomUUID()).build())
            .setAccessMode(FactSEB.AccessMode.Explicit)
            .setConfidence(0.1f)
            .setTrust(0.2f)
            .setTimestamp(123456789)
            .setLastSeenTimestamp(987654321)
            .addAclEntry(AclEntrySEB.builder().setSubject(SubjectInfoSEB.builder().setId(UUID.randomUUID()).build()).build())
            .build();

    FactDocument document = converter.apply(seb);
    assertNotNull(document);
    assertEquals(seb.getId(), document.getId());
    assertEquals(seb.getType().getId(), document.getTypeID());
    assertEquals(seb.getValue(), document.getValue());
    assertEquals(seb.getInReferenceTo().getId(), document.getInReferenceTo());
    assertEquals(seb.getOrganization().getId(), document.getOrganizationID());
    assertEquals(seb.getOrigin().getId(), document.getOriginID());
    assertEquals(seb.getAddedBy().getId(), document.getAddedByID());
    assertEquals(seb.getAccessMode().name(), document.getAccessMode().name());
    assertEquals(seb.getConfidence(), document.getConfidence(), 0.0);
    assertEquals(seb.getTrust(), document.getTrust(), 0.0);
    assertEquals(seb.getTimestamp(), document.getTimestamp());
    assertEquals(seb.getLastSeenTimestamp(), document.getLastSeenTimestamp());
    assertEquals(SetUtils.set(seb.getAcl(), entry -> entry.getSubject().getId()), document.getAcl());
  }

  @Test
  public void testConvertObjectsSourceAndDestination() {
    ObjectInfoSEB source = createObjectInfoSEB("source");
    ObjectInfoSEB destination = createObjectInfoSEB("destination");
    FactSEB seb = FactSEB.builder()
            .setSourceObject(source)
            .setDestinationObject(destination)
            .setBidirectionalBinding(false)
            .build();

    FactDocument document = converter.apply(seb);
    assertNotNull(document);
    assertEquals(2, document.getObjectCount());
    assertObject(source, getObjectDocument(document, source), ObjectDocument.Direction.FactIsDestination);
    assertObject(destination, getObjectDocument(document, destination), ObjectDocument.Direction.FactIsSource);
  }

  @Test
  public void testConvertObjectsBidirectional() {
    ObjectInfoSEB source = createObjectInfoSEB("source");
    ObjectInfoSEB destination = createObjectInfoSEB("destination");
    FactSEB seb = FactSEB.builder()
            .setSourceObject(source)
            .setDestinationObject(destination)
            .setBidirectionalBinding(true)
            .build();

    FactDocument document = converter.apply(seb);
    assertNotNull(document);
    assertEquals(2, document.getObjectCount());
    assertObject(source, getObjectDocument(document, source), ObjectDocument.Direction.BiDirectional);
    assertObject(destination, getObjectDocument(document, destination), ObjectDocument.Direction.BiDirectional);
  }

  private void assertObject(ObjectInfoSEB expected, ObjectDocument actual, ObjectDocument.Direction expectedDirection) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getType().getId(), actual.getTypeID());
    assertEquals(expected.getValue(), actual.getValue());
    assertEquals(expectedDirection, actual.getDirection());
  }

  private ObjectInfoSEB createObjectInfoSEB(String value) {
    return ObjectInfoSEB.builder()
            .setId(UUID.randomUUID())
            .setType(ObjectTypeInfoSEB.builder().setId(UUID.randomUUID()).build())
            .setValue(value)
            .build();
  }

  private ObjectDocument getObjectDocument(FactDocument fact, ObjectInfoSEB targetObject) {
    return fact.getObjects()
            .stream()
            .filter(o -> Objects.equals(o.getId(), targetObject.getId()))
            .findFirst()
            .orElseThrow(AssertionError::new);
  }
}
