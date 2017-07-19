package no.mnemonic.act.platform.dao.tinkerpop;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.entity.cassandra.*;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.mockito.Mock;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

abstract class AbstractGraphTest {

  @Mock
  private ObjectManager objectManager;
  @Mock
  private FactManager factManager;

  private ActGraph actGraph;

  @Before
  public void setup() {
    initMocks(this);

    actGraph = ActGraph.builder()
            .setObjectManager(objectManager)
            .setFactManager(factManager)
            .setHasFactAccess(f -> true)
            .build();
  }

  ObjectManager getObjectManager() {
    return objectManager;
  }

  FactManager getFactManager() {
    return factManager;
  }

  ActGraph getActGraph() {
    return actGraph;
  }

  UUID mockObject() {
    UUID objectID = UUID.randomUUID();
    UUID typeID = UUID.randomUUID();

    when(objectManager.getObject(objectID)).thenReturn(new ObjectEntity()
            .setId(objectID)
            .setTypeID(typeID)
            .setValue("value")
    );

    when(objectManager.getObjectType(typeID)).thenReturn(new ObjectTypeEntity()
            .setId(typeID)
            .setName("type")
    );

    return objectID;
  }

  UUID mockFact(FactEntity.FactObjectBinding binding) {
    UUID factID = UUID.randomUUID();
    UUID typeID = UUID.randomUUID();

    when(factManager.getFact(factID)).thenReturn(new FactEntity()
            .setId(factID)
            .setTypeID(typeID)
            .setValue("value")
            .setInReferenceToID(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .setOrganizationID(UUID.fromString("00000000-0000-0000-0000-000000000002"))
            .setSourceID(UUID.fromString("00000000-0000-0000-0000-000000000003"))
            .setAccessMode(AccessMode.Public)
            .setTimestamp(123456789)
            .setLastSeenTimestamp(987654321)
            .setBindings(ObjectUtils.ifNotNull(binding, ListUtils::list, ListUtils.list()))
    );

    when(factManager.getFactType(typeID)).thenReturn(new FactTypeEntity()
            .setId(typeID)
            .setName("type")
    );

    return factID;
  }

}
