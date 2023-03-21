package no.mnemonic.services.grafeo.cli.tools.handlers;

import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static no.mnemonic.services.grafeo.cli.tools.handlers.CassandraMigrateTimeGlobalFlagHandler.RETRACTION_FACT_TYPE_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CassandraMigrateTimeGlobalFlagHandlerTest {

  @Mock
  private FactManager factManager;
  @Mock
  private ObjectManager objectManager;
  @Mock
  private CassandraFactProcessor factProcessor;
  @InjectMocks
  private CassandraMigrateTimeGlobalFlagHandler handler;

  @Test
  public void testMigrateInvokesFactProcessor() {
    Instant start = Instant.parse("2021-01-01T12:00:00.000Z");
    Instant stop = Instant.parse("2021-01-01T17:30:00.000Z");

    assertDoesNotThrow(() -> handler.migrate(start, stop));
    verify(factProcessor).process(notNull(), eq(start), eq(stop), eq(false));
  }

  @Test
  public void testMigrateSkipsAlreadyMigratedFact() {
    FactEntity fact = new FactEntity().addFlag(FactEntity.Flag.TimeGlobalIndex);

    assertDoesNotThrow(() -> handler.migrateSingleFact(fact));
    verifyNoInteractions(factManager, objectManager);
  }

  @Test
  public void testMigrateSkipsNoTimeGlobalFact() {
    FactEntity fact = new FactEntity();

    assertDoesNotThrow(() -> handler.migrateSingleFact(fact));
    verify(factManager, never()).saveFact(any());
  }

  @Test
  public void testMigrateTimeGlobalRetractionFact() {
    FactEntity fact = new FactEntity().setTypeID(RETRACTION_FACT_TYPE_ID);

    assertDoesNotThrow(() -> handler.migrateSingleFact(fact));
    verify(factManager).saveFact(argThat(entity -> entity.isSet(FactEntity.Flag.TimeGlobalIndex)));
  }

  @Test
  public void testMigrateTimeGlobalMetaFact() {
    FactEntity referencedFact = new FactEntity()
            .setId(UUID.randomUUID())
            .addFlag(FactEntity.Flag.TimeGlobalIndex);
    FactEntity metaFact = new FactEntity()
            .setId(UUID.randomUUID())
            .setInReferenceToID(referencedFact.getId());
    when(factManager.getFact(referencedFact.getId())).thenReturn(referencedFact);

    assertDoesNotThrow(() -> handler.migrateSingleFact(metaFact));
    verify(factManager).getFact(referencedFact.getId());
    verify(factManager).saveFact(argThat(entity -> Objects.equals(entity.getId(), metaFact.getId()) && entity.isSet(FactEntity.Flag.TimeGlobalIndex)));
  }

  @Test
  public void testMigrateNoTimeGlobalMetaFact() {
    FactEntity referencedFact = new FactEntity()
            .setId(UUID.randomUUID());
    FactEntity metaFact = new FactEntity()
            .setId(UUID.randomUUID())
            .setInReferenceToID(referencedFact.getId());
    when(factManager.getFact(referencedFact.getId())).thenReturn(referencedFact);

    assertDoesNotThrow(() -> handler.migrateSingleFact(metaFact));
    verify(factManager).getFact(referencedFact.getId());
    verify(factManager, never()).saveFact(any());
  }

  @Test
  public void testMigrateTimeGlobalObjectFactWithTwoLeggedFact() {
    UUID sourceObjectID = UUID.randomUUID();
    UUID destinationObjectID = UUID.randomUUID();
    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())
            .addBinding(new FactEntity.FactObjectBinding().setObjectID(sourceObjectID))
            .addBinding(new FactEntity.FactObjectBinding().setObjectID(destinationObjectID));
    when(objectManager.getObject(any())).thenReturn(new ObjectEntity().setTypeID(UUID.randomUUID()));
    when(objectManager.getObjectType(isA(UUID.class))).thenReturn(new ObjectTypeEntity().addFlag(ObjectTypeEntity.Flag.TimeGlobalIndex));

    assertDoesNotThrow(() -> handler.migrateSingleFact(fact));
    verify(objectManager).getObject(sourceObjectID);
    verify(objectManager).getObject(destinationObjectID);
    verify(objectManager, times(2)).getObjectType(isA(UUID.class));
    verify(factManager).saveFact(argThat(entity -> Objects.equals(entity.getId(), fact.getId()) && entity.isSet(FactEntity.Flag.TimeGlobalIndex)));
  }

  @Test
  public void testMigrateNoTimeGlobalObjectFactWithTwoLeggedFact() {
    UUID sourceObjectID = UUID.randomUUID();
    UUID destinationObjectID = UUID.randomUUID();
    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())
            .addBinding(new FactEntity.FactObjectBinding().setObjectID(sourceObjectID))
            .addBinding(new FactEntity.FactObjectBinding().setObjectID(destinationObjectID));
    when(objectManager.getObject(any())).thenReturn(new ObjectEntity().setTypeID(UUID.randomUUID()));
    when(objectManager.getObjectType(isA(UUID.class)))
            .thenReturn(new ObjectTypeEntity().addFlag(ObjectTypeEntity.Flag.TimeGlobalIndex))
            .thenReturn(new ObjectTypeEntity());

    assertDoesNotThrow(() -> handler.migrateSingleFact(fact));
    verify(objectManager).getObject(sourceObjectID);
    verify(objectManager).getObject(destinationObjectID);
    verify(objectManager, times(2)).getObjectType(isA(UUID.class));
    verify(factManager, never()).saveFact(any());
  }

  @Test
  public void testMigrateTimeGlobalObjectFactWithOneLeggedFact() {
    UUID objectID = UUID.randomUUID();
    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())
            .addBinding(new FactEntity.FactObjectBinding().setObjectID(objectID));
    when(objectManager.getObject(any())).thenReturn(new ObjectEntity().setTypeID(UUID.randomUUID()));
    when(objectManager.getObjectType(isA(UUID.class))).thenReturn(new ObjectTypeEntity().addFlag(ObjectTypeEntity.Flag.TimeGlobalIndex));

    assertDoesNotThrow(() -> handler.migrateSingleFact(fact));
    verify(objectManager).getObject(objectID);
    verify(objectManager).getObjectType(isA(UUID.class));
    verify(factManager).saveFact(argThat(entity -> Objects.equals(entity.getId(), fact.getId()) && entity.isSet(FactEntity.Flag.TimeGlobalIndex)));
  }

  @Test
  public void testMigrateNoTimeGlobalObjectFactWithOneLeggedFact() {
    UUID objectID = UUID.randomUUID();
    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())
            .addBinding(new FactEntity.FactObjectBinding().setObjectID(objectID));
    when(objectManager.getObject(any())).thenReturn(new ObjectEntity().setTypeID(UUID.randomUUID()));
    when(objectManager.getObjectType(isA(UUID.class))).thenReturn(new ObjectTypeEntity());

    assertDoesNotThrow(() -> handler.migrateSingleFact(fact));
    verify(objectManager).getObject(objectID);
    verify(objectManager).getObjectType(isA(UUID.class));
    verify(factManager, never()).saveFact(any());
  }

  @Test
  public void testMigrateTimeGlobalObjectFactCachesFlag() {
    UUID objectID = UUID.randomUUID();
    FactEntity fact1 = new FactEntity()
            .setId(UUID.randomUUID())
            .addBinding(new FactEntity.FactObjectBinding().setObjectID(objectID));
    FactEntity fact2 = new FactEntity()
            .setId(UUID.randomUUID())
            .addBinding(new FactEntity.FactObjectBinding().setObjectID(objectID));
    when(objectManager.getObject(any())).thenReturn(new ObjectEntity().setTypeID(UUID.randomUUID()));
    when(objectManager.getObjectType(isA(UUID.class))).thenReturn(new ObjectTypeEntity().addFlag(ObjectTypeEntity.Flag.TimeGlobalIndex));

    assertDoesNotThrow(() -> handler.migrateSingleFact(fact1));
    verify(objectManager).getObject(objectID);
    verify(objectManager).getObjectType(isA(UUID.class));
    verify(factManager).saveFact(argThat(entity -> Objects.equals(entity.getId(), fact1.getId()) && entity.isSet(FactEntity.Flag.TimeGlobalIndex)));

    assertDoesNotThrow(() -> handler.migrateSingleFact(fact2));
    verifyNoMoreInteractions(objectManager);
    verify(factManager).saveFact(argThat(entity -> Objects.equals(entity.getId(), fact2.getId()) && entity.isSet(FactEntity.Flag.TimeGlobalIndex)));
  }
}
