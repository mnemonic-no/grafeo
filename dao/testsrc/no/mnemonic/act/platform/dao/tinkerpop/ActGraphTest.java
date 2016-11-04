package no.mnemonic.act.platform.dao.tinkerpop;

import no.mnemonic.act.platform.dao.cassandra.ClusterManager;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.entity.cassandra.*;
import no.mnemonic.act.platform.entity.handlers.DefaultEntityHandlerFactory;
import no.mnemonic.act.platform.entity.handlers.EntityHandlerFactory;
import no.mnemonic.commons.testtools.cassandra.CassandraTestResource;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.UUID;

import static no.mnemonic.act.platform.entity.cassandra.CassandraEntity.KEY_SPACE;
import static org.junit.Assert.assertEquals;

public class ActGraphTest {

  private static ClusterManager clusterManager;
  private static FactManager factManager;
  private static ObjectManager objectManager;
  private static ObjectEntity ip;
  private static ObjectEntity domain;

  @ClassRule
  public static CassandraTestResource cassandra = CassandraTestResource.builder()
          .setClusterName("ACT Cluster")
          .setKeyspaceName(KEY_SPACE)
          .setStartupScript("resources/setup.cql")
          .build();

  @BeforeClass
  public static void initialize() throws Exception {
    EntityHandlerFactory factory = new DefaultEntityHandlerFactory();

    // Create managers and start them up.
    clusterManager = ClusterManager.builder()
            .setClusterName(cassandra.getClusterName())
            .setPort(cassandra.getPort())
            .addContactPoint("127.0.0.1")
            .build();
    objectManager = new ObjectManager(clusterManager, factory);
    factManager = new FactManager(clusterManager, factory);
    clusterManager.startComponent();
    objectManager.startComponent();
    factManager.startComponent();

    // Create Objects and Facts in order to create Graph to traverse.
    ObjectTypeEntity ipType = createObjectType("ip");
    ObjectTypeEntity domainType = createObjectType("domain");
    ip = createObject(ipType.getId(), "1.1.1.1");
    domain = createObject(domainType.getId(), "example.org");

    FactTypeEntity resolveType = createFactType("resolve");
    FactEntity resolve = createFact(resolveType.getId(), "to")
            .setBindings(ListUtils.list(
                    createFactObjectBinding(ip.getId(), Direction.FactIsDestination),
                    createFactObjectBinding(domain.getId(), Direction.FactIsSource)
            ));

    // Save everything to Cassandra.
    objectManager.saveObjectType(ipType);
    objectManager.saveObjectType(domainType);
    objectManager.saveObject(ip);
    objectManager.saveObject(domain);
    objectManager.saveObjectFactBinding(createObjectFactBinding(ip.getId(), resolve.getId(), Direction.FactIsDestination));
    objectManager.saveObjectFactBinding(createObjectFactBinding(domain.getId(), resolve.getId(), Direction.FactIsSource));
    factManager.saveFactType(resolveType);
    factManager.saveFact(resolve);
  }

  @AfterClass
  public static void teardown() {
    ObjectUtils.ifNotNullDo(factManager, FactManager::stopComponent);
    ObjectUtils.ifNotNullDo(objectManager, ObjectManager::stopComponent);
    ObjectUtils.ifNotNullDo(clusterManager, ClusterManager::stopComponent);
  }

  @Test
  public void testResolveDomain() {
    GraphTraversalSource g = ActGraph.builder()
            .setObjectManager(objectManager)
            .setFactManager(factManager)
            .build()
            .traversal();
    assertEquals(domain.getValue(), g.V(ip.getId()).out("resolve").values("value").next());
  }

  private static ObjectTypeEntity createObjectType(String name) {
    return new ObjectTypeEntity()
            .setId(UUID.randomUUID())
            .setNamespaceID(UUID.randomUUID())
            .setName(name)
            .setEntityHandler("IdentityHandler");
  }

  private static FactTypeEntity createFactType(String name) {
    return new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setNamespaceID(UUID.randomUUID())
            .setName(name)
            .setEntityHandler("IdentityHandler");
  }

  private static ObjectEntity createObject(UUID typeID, String value) {
    return new ObjectEntity()
            .setId(UUID.randomUUID())
            .setTypeID(typeID)
            .setValue(value);
  }

  private static FactEntity createFact(UUID typeID, String value) {
    return new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(typeID)
            .setValue(value);
  }

  private static FactEntity.FactObjectBinding createFactObjectBinding(UUID objectID, Direction direction) {
    return new FactEntity.FactObjectBinding()
            .setObjectID(objectID)
            .setDirection(direction);
  }

  private static ObjectFactBindingEntity createObjectFactBinding(UUID objectID, UUID factID, Direction direction) {
    return new ObjectFactBindingEntity()
            .setObjectID(objectID)
            .setFactID(factID)
            .setDirection(direction);
  }

}
