package no.mnemonic.act.platform.cli.tools.converters;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FactEntityToDocumentConverterTest {

  @Mock
  private FactManager factManager;
  @Mock
  private ObjectManager objectManager;
  @InjectMocks
  private FactEntityToDocumentConverter converter;

  @Test
  public void testConvertNull() {
    assertNull(converter.apply(null));
  }

  @Test
  public void testConvertEmpty() {
    assertNotNull(converter.apply(new FactEntity()));
  }

  @Test
  public void testConvertBasic() {
    FactEntity entity = new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value")
            .setInReferenceToID(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setAddedByID(UUID.randomUUID())
            .setLastSeenByID(UUID.randomUUID())
            .setAccessMode(AccessMode.Explicit)
            .setConfidence(0.1f)
            .setTrust(0.2f)
            .setTimestamp(123456789)
            .setLastSeenTimestamp(987654321);

    FactDocument document = converter.apply(entity);
    assertNotNull(document);
    assertEquals(entity.getId(), document.getId());
    assertEquals(entity.getTypeID(), document.getTypeID());
    assertEquals(entity.getValue(), document.getValue());
    assertEquals(entity.getInReferenceToID(), document.getInReferenceTo());
    assertEquals(entity.getOrganizationID(), document.getOrganizationID());
    assertEquals(entity.getOriginID(), document.getOriginID());
    assertEquals(entity.getAddedByID(), document.getAddedByID());
    assertEquals(entity.getLastSeenByID(), document.getLastSeenByID());
    assertEquals(entity.getAccessMode().name(), document.getAccessMode().name());
    assertEquals(entity.getConfidence(), document.getConfidence(), 0.0f);
    assertEquals(entity.getTrust(), document.getTrust(), 0.0f);
    assertEquals(entity.getTimestamp(), document.getTimestamp());
    assertEquals(entity.getLastSeenTimestamp(), document.getLastSeenTimestamp());
  }

  @Test
  public void testConvertWithAcl() {
    FactEntity factEntity = new FactEntity().setId(UUID.randomUUID());
    FactAclEntity aclEntity = new FactAclEntity().setSubjectID(UUID.randomUUID());

    when(factManager.fetchFactAcl(notNull())).thenReturn(ListUtils.list(aclEntity));

    FactDocument factDocument = converter.apply(factEntity);
    assertNotNull(factDocument);
    assertEquals(SetUtils.set(aclEntity.getSubjectID()), factDocument.getAcl());

    verify(factManager).fetchFactAcl(factEntity.getId());
  }

  @Test
  public void testConvertWithObject() {
    ObjectEntity objectEntity = new ObjectEntity()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value");
    FactEntity.FactObjectBinding binding = new FactEntity.FactObjectBinding()
            .setObjectID(objectEntity.getId())
            .setDirection(Direction.BiDirectional);
    FactEntity factEntity = new FactEntity().addBinding(binding);

    when(objectManager.getObject(notNull())).thenReturn(objectEntity);

    FactDocument factDocument = converter.apply(factEntity);
    assertNotNull(factDocument);
    assertEquals(1, factDocument.getObjectCount());

    ObjectDocument objectDocument = factDocument.getObjects().iterator().next();
    assertEquals(objectEntity.getId(), objectDocument.getId());
    assertEquals(objectEntity.getTypeID(), objectDocument.getTypeID());
    assertEquals(objectEntity.getValue(), objectDocument.getValue());
    assertEquals(binding.getDirection().name(), objectDocument.getDirection().name());

    verify(objectManager).getObject(objectEntity.getId());
  }

  @Test
  public void testConvertWithUnresolvedObject() {
    FactEntity.FactObjectBinding binding = new FactEntity.FactObjectBinding().setObjectID(UUID.randomUUID());
    FactEntity factEntity = new FactEntity().addBinding(binding);

    FactDocument factDocument = converter.apply(factEntity);
    assertNotNull(factDocument);
    assertEquals(0, factDocument.getObjectCount());

    verify(objectManager).getObject(binding.getObjectID());
  }
}
