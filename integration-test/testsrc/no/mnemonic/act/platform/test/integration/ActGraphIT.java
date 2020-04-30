package no.mnemonic.act.platform.test.integration;

import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.cassandra.ClusterManager;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.dao.elastic.ClientFactory;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.facade.ObjectFactDaoFacade;
import no.mnemonic.act.platform.dao.facade.converters.FactAclEntryRecordConverter;
import no.mnemonic.act.platform.dao.facade.converters.FactCommentRecordConverter;
import no.mnemonic.act.platform.dao.facade.converters.FactRecordConverter;
import no.mnemonic.act.platform.dao.facade.converters.ObjectRecordConverter;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.tinkerpop.ActGraph;
import no.mnemonic.act.platform.service.ti.tinkerpop.TraverseParams;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver;
import no.mnemonic.commons.junit.docker.CassandraDockerResource;
import no.mnemonic.commons.junit.docker.DockerTestUtils;
import no.mnemonic.commons.junit.docker.ElasticSearchDockerResource;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActGraphIT {

  private static ClusterManager clusterManager;
  private static ClientFactory clientFactory;
  private static FactManager factManager;
  private static ObjectManager objectManager;
  private static ObjectFactDao objectFactDao;
  private static FactSearchManager factSearchManager;
  private static ObjectFactTypeResolver objectFactTypeResolver;
  private static TiSecurityContext mockSecurityContext;
  private static ObjectRecord ip;
  private static ObjectRecord domain;
  private static ObjectRecord attack;

  public static CassandraDockerResource cassandra = CassandraDockerResource.builder()
          .setImageName("cassandra")
          .setExposedPortsRange("15000-25000")
          .addApplicationPort(9042)
          .setSetupScript("setup.cql")
          .build();

  private static ElasticSearchDockerResource elastic = ElasticSearchDockerResource.builder()
          // Need to specify the exact version here because Elastic doesn't publish images with the 'latest' tag.
          // Usually this should be the same version as the ElasticSearch client used.
          .setImageName("elasticsearch/elasticsearch:6.8.8")
          .setExposedPortsRange("15000-25000")
          .addApplicationPort(9200)
          .addEnvironmentVariable("xpack.security.enabled", "false")
          .addEnvironmentVariable("xpack.monitoring.enabled", "false")
          .addEnvironmentVariable("xpack.ml.enabled", "false")
          .addEnvironmentVariable("xpack.graph.enabled", "false")
          .addEnvironmentVariable("xpack.watcher.enabled", "false")
          .build();

  @ClassRule
  public static TestRule chain = RuleChain.outerRule(cassandra).around(elastic);

  @BeforeClass
  public static void initialize() {
    // Create managers and start them up.
    clusterManager = ClusterManager.builder()
            .setDataCenter("datacenter1")
            .setPort(cassandra.getExposedHostPort(9042))
            .addContactPoint(DockerTestUtils.getDockerHost())
            .build();

    clientFactory = ClientFactory.builder()
            .setPort(elastic.getExposedHostPort(9200))
            .addContactPoint(DockerTestUtils.getDockerHost())
            .build();

    mockSecurityContext = mock(TiSecurityContext.class);
    when(mockSecurityContext.hasReadPermission(isA(FactRecord.class))).thenReturn(true);
    when(mockSecurityContext.getCurrentUserID()).thenReturn(new UUID(0, 1));
    when(mockSecurityContext.getAvailableOrganizationID()).thenReturn(SetUtils.set(new UUID(0, 1)));

    objectManager = new ObjectManager(clusterManager);
    factManager = new FactManager(clusterManager);
    factSearchManager = new FactSearchManager(clientFactory)
            .setTestEnvironment(true)
            .setSearchScrollExpiration("5s")
            .setSearchScrollSize(1);
    objectFactDao = new ObjectFactDaoFacade(
            objectManager,
            factManager,
            factSearchManager,
            new ObjectRecordConverter(),
            new FactRecordConverter(
                    factManager,
                    objectManager,
                    new ObjectRecordConverter(),
                    new FactAclEntryRecordConverter(), new FactCommentRecordConverter()),
            new FactAclEntryRecordConverter(),
            new FactCommentRecordConverter());
    objectFactTypeResolver = new ObjectFactTypeResolver(factManager, objectManager);

    clusterManager.startComponent();
    clientFactory.startComponent();
    objectManager.startComponent();
    factManager.startComponent();
    factSearchManager.startComponent();

    // Create Objects and Facts in order to create Graph to traverse.
    // Example data
    // ip(1.1.1.1)         - resolve("to")   -> domain(example.org)
    // attack(apt)         - used("by")      -> ip(1.1.1.1)
    // domain(example.org) <- seen("today")
    ObjectTypeEntity ipType = createObjectType("ip");
    ObjectTypeEntity domainType = createObjectType("domain");
    ObjectTypeEntity attackType = createObjectType("attack");
    ip = createObject(ipType.getId(), "1.1.1.1");
    domain = createObject(domainType.getId(), "example.org");
    attack = createObject(attackType.getId(), "apt");

    FactTypeEntity resolveType = createFactType("resolve");
    FactTypeEntity seenType = createFactType("seen");
    FactTypeEntity usedType = createFactType("used");

    FactRecord resolve = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(resolveType.getId())
            .setAccessMode(FactRecord.AccessMode.Public)
            .setValue("to")
            .setSourceObject(ip)
            .setDestinationObject(domain);

    FactRecord seen = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(seenType.getId())
            .setAccessMode(FactRecord.AccessMode.Public)
            .setValue("today")
            .setDestinationObject(domain);

    FactRecord used = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(usedType.getId())
            .setAccessMode(FactRecord.AccessMode.Public)
            .setValue("by")
            .setSourceObject(attack)
            .setDestinationObject(ip);

    // Save everything to Cassandra and ElasticSearch.
    objectManager.saveObjectType(ipType);
    objectManager.saveObjectType(domainType);
    objectManager.saveObjectType(attackType);

    factManager.saveFactType(resolveType);
    factManager.saveFactType(seenType);
    factManager.saveFactType(usedType);

    objectFactDao.storeObject(ip);
    objectFactDao.storeObject(domain);
    objectFactDao.storeObject(attack);

    objectFactDao.storeFact(resolve);
    objectFactDao.storeFact(used);
    objectFactDao.storeFact(seen);
  }

  @AfterClass
  public static void teardown() {
    ObjectUtils.ifNotNullDo(factManager, FactManager::stopComponent);
    ObjectUtils.ifNotNullDo(objectManager, ObjectManager::stopComponent);
    ObjectUtils.ifNotNullDo(factSearchManager, FactSearchManager::stopComponent);
    ObjectUtils.ifNotNullDo(clusterManager, ClusterManager::stopComponent);
    ObjectUtils.ifNotNullDo(clientFactory, ClientFactory::stopComponent);
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
  public void testFollowEdgeWithoutFactAccess() {
    TiSecurityContext securityContextNoAccess = mock(TiSecurityContext.class);
    when(securityContextNoAccess.getCurrentUserID()).thenReturn(new UUID(0, 1));
    when(securityContextNoAccess.getAvailableOrganizationID()).thenReturn(SetUtils.set(new UUID(0, 1)));
    when(securityContextNoAccess.hasReadPermission(any(FactRecord.class))).thenReturn(false);

    GraphTraversalSource g = ActGraph.builder()
            .setObjectFactDao(objectFactDao)
            .setObjectTypeFactResolver(objectFactTypeResolver)
            .setSecurityContext(securityContextNoAccess)
            .setTraverseParams(TraverseParams.builder().build())
            .build()
            .traversal();

    assertFalse(g.V(ip.getId()).out("resolve").hasNext());
  }

  @Test
  public void testCountOutgoingEdges() {
    assertEquals(1, IteratorUtils.count(createGraph().V(ip.getId()).out()));
    assertEquals(0, IteratorUtils.count(createGraph().V(domain.getId()).out()));
    assertEquals(1, IteratorUtils.count(createGraph().V(attack.getId()).out()));
  }

  @Test
  public void testCountIncomingEdges() {
    assertEquals(1, IteratorUtils.count(createGraph().V(ip.getId()).in()));
    assertEquals(1, IteratorUtils.count(createGraph().V(domain.getId()).in()));
    assertEquals(0, IteratorUtils.count(createGraph().V(attack.getId()).in()));
  }

  @Test
  public void testCountEdgesFilterWithLabel() {
    assertEquals(2, IteratorUtils.count(createGraph().V(domain.getId()).both("resolve")));
    assertEquals(1, IteratorUtils.count(createGraph().V(domain.getId()).in("resolve")));
    assertEquals(0, IteratorUtils.count(createGraph().V(domain.getId()).out("resolve")));
  }

  @Test
  public void testCountEdgesFilterWithMultiLabel() {
    assertEquals(2, IteratorUtils.count(createGraph().V(domain.getId()).both("seen", "resolve")));
    assertEquals(1, IteratorUtils.count(createGraph().V(domain.getId()).in("seen", "resolve")));
    assertEquals(0, IteratorUtils.count(createGraph().V(domain.getId()).out("seen", "resolve")));
  }

  private GraphTraversalSource createGraph() {
    return ActGraph.builder()
            .setObjectFactDao(objectFactDao)
            .setObjectTypeFactResolver(objectFactTypeResolver)
            .setSecurityContext(mockSecurityContext)
            .setTraverseParams(TraverseParams.builder().build())
            .build()
            .traversal();
  }

  private static ObjectTypeEntity createObjectType(String name) {
    return new ObjectTypeEntity()
            .setId(UUID.randomUUID())
            .setNamespaceID(UUID.randomUUID())
            .setName(name);
  }

  private static FactTypeEntity createFactType(String name) {
    return new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setNamespaceID(UUID.randomUUID())
            .setName(name);
  }

  private static ObjectRecord createObject(UUID typeID, String value) {
    return new ObjectRecord()
            .setId(UUID.randomUUID())
            .setTypeID(typeID)
            .setValue(value);
  }
}
