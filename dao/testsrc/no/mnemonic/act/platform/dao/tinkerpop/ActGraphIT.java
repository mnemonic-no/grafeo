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
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static no.mnemonic.act.platform.entity.cassandra.CassandraEntity.KEY_SPACE;
import static org.junit.Assert.*;

public class ActGraphIT {

  private static ClusterManager clusterManager;
  private static FactManager factManager;
  private static ObjectManager objectManager;
  private static ObjectEntity ip;
  private static ObjectEntity domain;
  private static ObjectEntity attack;

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
    ObjectTypeEntity attackType = createObjectType("attack");
    ip = createObject(ipType.getId(), "1.1.1.1");
    domain = createObject(domainType.getId(), "example.org");
    attack = createObject(attackType.getId(), "apt");

    FactTypeEntity resolveType = createFactType("resolve");
    FactTypeEntity seenType = createFactType("seen");
    FactTypeEntity usedType = createFactType("used");
    FactEntity resolve = createFact(resolveType.getId(), "to")
            .setBindings(ListUtils.list(
                    createFactObjectBinding(ip.getId(), Direction.FactIsDestination),
                    createFactObjectBinding(domain.getId(), Direction.FactIsSource)
            ));
    FactEntity seen = createFact(seenType.getId(), "today")
            .setBindings(ListUtils.list(
                    createFactObjectBinding(domain.getId(), Direction.None)
            ));
    FactEntity used = createFact(usedType.getId(), "by")
            .setBindings(ListUtils.list(
                    createFactObjectBinding(ip.getId(), Direction.BiDirectional),
                    createFactObjectBinding(domain.getId(), Direction.BiDirectional),
                    createFactObjectBinding(attack.getId(), Direction.BiDirectional)
            ));

    // Save everything to Cassandra.
    objectManager.saveObjectType(ipType);
    objectManager.saveObjectType(domainType);
    objectManager.saveObjectType(attackType);
    objectManager.saveObject(ip);
    objectManager.saveObject(domain);
    objectManager.saveObject(attack);
    objectManager.saveObjectFactBinding(createObjectFactBinding(ip.getId(), resolve.getId(), Direction.FactIsDestination));
    objectManager.saveObjectFactBinding(createObjectFactBinding(ip.getId(), used.getId(), Direction.BiDirectional));
    objectManager.saveObjectFactBinding(createObjectFactBinding(domain.getId(), resolve.getId(), Direction.FactIsSource));
    objectManager.saveObjectFactBinding(createObjectFactBinding(domain.getId(), seen.getId(), Direction.None));
    objectManager.saveObjectFactBinding(createObjectFactBinding(domain.getId(), used.getId(), Direction.BiDirectional));
    objectManager.saveObjectFactBinding(createObjectFactBinding(attack.getId(), used.getId(), Direction.BiDirectional));
    factManager.saveFactType(resolveType);
    factManager.saveFactType(seenType);
    factManager.saveFactType(usedType);
    factManager.saveFact(resolve);
    factManager.saveFact(seen);
    factManager.saveFact(used);
  }

  @AfterClass
  public static void teardown() {
    ObjectUtils.ifNotNullDo(factManager, FactManager::stopComponent);
    ObjectUtils.ifNotNullDo(objectManager, ObjectManager::stopComponent);
    ObjectUtils.ifNotNullDo(clusterManager, ClusterManager::stopComponent);
  }

  @Test
  public void testFollowSingleEdge() {
    assertEquals(domain.getValue(), createGraph().V(ip.getId()).out("resolve").values("value").next());
  }

  @Test
  public void testFollowSingleEdgeReversed() {
    assertEquals(ip.getValue(), createGraph().V(domain.getId()).in("resolve").values("value").next());
  }

  @Test
  public void testFollowLoop() {
    assertEquals(domain.getId(), createGraph().V(domain.getId()).out("seen").next().id());
  }

  @Test
  public void testFollowMultiEdge() {
    List<Vertex> vertices = createGraph().V(attack.getId()).out("used").toList();
    assertEquals(2, vertices.size());
    assertTrue(vertices.stream().anyMatch(v -> Objects.equals(v.id(), ip.getId())));
    assertTrue(vertices.stream().anyMatch(v -> Objects.equals(v.id(), domain.getId())));
  }

  @Test
  public void testFollowEdgeWithoutFactAccess() {
    GraphTraversalSource g = ActGraph.builder()
            .setObjectManager(objectManager)
            .setFactManager(factManager)
            .setHasFactAccess(f -> false)
            .build()
            .traversal();
    assertFalse(g.V(ip.getId()).out("resolve").hasNext());
  }

  private GraphTraversalSource createGraph() {
    return ActGraph.builder()
            .setObjectManager(objectManager)
            .setFactManager(factManager)
            .setHasFactAccess(f -> true)
            .build()
            .traversal();
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
