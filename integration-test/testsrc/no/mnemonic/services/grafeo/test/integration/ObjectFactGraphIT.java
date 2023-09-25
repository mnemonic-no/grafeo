package no.mnemonic.services.grafeo.test.integration;

import com.google.inject.*;
import com.google.inject.name.Names;
import no.mnemonic.commons.container.ComponentContainer;
import no.mnemonic.commons.container.providers.GuiceBeanProvider;
import no.mnemonic.commons.junit.docker.CassandraDockerResource;
import no.mnemonic.commons.junit.docker.ElasticSearchDockerResource;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.bindings.DaoCache;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.dao.elastic.FactSearchManager;
import no.mnemonic.services.grafeo.dao.modules.DaoModule;
import no.mnemonic.services.grafeo.service.contexts.SecurityContext;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.handlers.FactRetractionHandler;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactTypeRequestResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.OrganizationByIdResponseResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.OriginByIdResponseResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.SubjectByIdResponseResolver;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.ObjectFactGraph;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.TraverseParams;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.utils.ObjectFactTypeResolver;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.utils.PropertyHelper;
import no.mnemonic.services.grafeo.service.scopes.ServiceRequestScope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.junit.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ObjectFactGraphIT {

  private static final long NOW = Instant.parse("2022-01-01T12:00:00.000Z").toEpochMilli();

  private final FactSearchCriteria factSearchCriteria = FactSearchCriteria.builder()
          .setAccessControlCriteria(AccessControlCriteria.builder()
                  .addCurrentUserIdentity(UUID.randomUUID())
                  .addAvailableOrganizationID(UUID.randomUUID())
                  .build())
          .setIndexSelectCriteria(IndexSelectCriteria.builder()
                  .setIndexStartTimestamp(NOW)
                  .setIndexEndTimestamp(NOW)
                  .build())
          .build();

  private static GuiceBeanProvider daoBeanProvider;
  private static ComponentContainer daoContainer;
  private static ObjectFactDao objectFactDao;
  private static GrafeoSecurityContext mockSecurityContext;
  private static ObjectTypeEntity ipType;
  private static ObjectTypeEntity domainType;
  private static ObjectTypeEntity attackType;
  private static FactTypeEntity resolveType;
  private static ObjectRecord ip;
  private static ObjectRecord domain;
  private static ObjectRecord attack;

  @ClassRule
  public static CassandraDockerResource cassandra = CassandraDockerResource.builder()
          .setImageName("cassandra")
          .setSkipPullDockerImage(true)
          .setExposedPortsRange("15000-25000")
          .addApplicationPort(9042)
          .skipReachabilityCheck()
          .build();

  @ClassRule
  public static ElasticSearchDockerResource elastic = ElasticSearchDockerResource.builder()
          // Need to specify the exact version here because Elastic doesn't publish images with the 'latest' tag.
          // Usually this should be the same version as the ElasticSearch client used.
          .setImageName("elasticsearch/elasticsearch:7.17.13")
          .setSkipPullDockerImage(true)
          .setExposedPortsRange("15000-25000")
          .addApplicationPort(9200)
          .skipReachabilityCheck()
          .addEnvironmentVariable("discovery.type", "single-node")
          .build();

  @BeforeClass
  public static void initialize() {
    // Employ ComponentContainer + Guice to instantiate and manage components.
    daoBeanProvider = new GuiceBeanProvider(new GraphModuleIT());
    daoContainer = ComponentContainer.create(daoBeanProvider);
    daoContainer.initialize();

    // Configure FactSearchManager with test environment in order to make indexed documents available for search immediately.
    daoBeanProvider.getBean(FactSearchManager.class)
            .orElseThrow(IllegalStateException::new)
            .setTestEnvironment(true);

    // GuiceBeanProvider only returns singleton objects, thus, an ObjectFactDao instance must be fetched directly from Guice.
    objectFactDao = daoBeanProvider.getInjector().getInstance(ObjectFactDao.class);
    ObjectManager objectManager = daoBeanProvider.getBean(ObjectManager.class).orElseThrow(IllegalStateException::new);
    FactManager factManager = daoBeanProvider.getBean(FactManager.class).orElseThrow(IllegalStateException::new);

    // Create Objects and Facts in order to create Graph to traverse.
    // Example data
    // ip(1.1.1.1)         - resolve("to")   -> domain(example.org)
    // attack(apt)         - used("by")      -> ip(1.1.1.1)
    // domain(example.org) <- seen("today")
    ipType = createObjectType("ip");
    domainType = createObjectType("domain");
    attackType = createObjectType("attack");
    ip = createObject(ipType.getId(), "1.1.1.1");
    domain = createObject(domainType.getId(), "example.org");
    attack = createObject(attackType.getId(), "apt");

    resolveType = createFactType("resolve");
    FactTypeEntity seenType = createFactType("seen");
    FactTypeEntity usedType = createFactType("used");

    FactRecord resolve = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(resolveType.getId())
            .setAccessMode(FactRecord.AccessMode.Public)
            .setValue("to")
            .setSourceObject(ip)
            .setDestinationObject(domain)
            .setLastSeenTimestamp(NOW);

    FactRecord seen = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(seenType.getId())
            .setAccessMode(FactRecord.AccessMode.Public)
            .setValue("today")
            .setDestinationObject(domain)
            .setLastSeenTimestamp(NOW);

    FactRecord used = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(usedType.getId())
            .setAccessMode(FactRecord.AccessMode.Public)
            .setValue("by")
            .setSourceObject(attack)
            .setDestinationObject(ip)
            .setLastSeenTimestamp(NOW);

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

  @Before
  public void setup() {
    // Create a new SecurityContext instance with default mocking for every test.
    mockSecurityContext = mock(GrafeoSecurityContext.class);
    when(mockSecurityContext.hasReadPermission(isA(FactRecord.class))).thenReturn(true);
  }

  @AfterClass
  public static void teardown() {
    daoContainer.destroy();
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
    when(mockSecurityContext.hasReadPermission(isA(FactRecord.class))).thenReturn(false);
    assertFalse(createGraph().V(ip.getId()).out("resolve").hasNext());
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

  @Test
  public void testFilterByTime() {
    ObjectRecord someIp = createObject(ipType.getId(), "2.2.2.2");
    ObjectRecord someDomain = createObject(domainType.getId(), "test.org");

    objectFactDao.storeObject(someIp);
    objectFactDao.storeObject(someDomain);

    Long t0 = NOW;
    Long beforeT0 = t0 - 10;
    Long afterT0 = t0 + 10;

    FactRecord someFact = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(resolveType.getId())
            .setAccessMode(FactRecord.AccessMode.Public)
            .setValue("to")
            .setTimestamp(t0)
            .setLastSeenTimestamp(t0)
            .setSourceObject(someIp)
            .setDestinationObject(someDomain);

    objectFactDao.storeFact(someFact);

    // No time filter
    assertEquals(1, IteratorUtils.count(createGraph(traverseParamsBuilder()
            .build()).V(someIp.getId()).outE("resolve")));

    // Just before
    assertEquals(0, IteratorUtils.count(createGraph(traverseParamsBuilder()
            .setBaseSearchCriteria(factSearchCriteria.toBuilder()
                    .setEndTimestamp(beforeT0)
                    .build())
            .build()).V(someIp.getId()).outE("resolve")));

    // Just after
    assertEquals(0, IteratorUtils.count(createGraph(traverseParamsBuilder()
            .setBaseSearchCriteria(factSearchCriteria.toBuilder()
                    .setStartTimestamp(afterT0)
                    .build())
            .build()).V(someIp.getId()).outE("resolve")));

    // In between
    assertEquals(1, IteratorUtils.count(createGraph(traverseParamsBuilder()
            .setBaseSearchCriteria(factSearchCriteria.toBuilder()
                    .setStartTimestamp(beforeT0)
                    .setEndTimestamp(afterT0)
                    .build())
            .build()).V(someIp.getId()).outE("resolve")));
  }

  @Test
  public void testFilterByRetractionStatus() {
    ObjectRecord someIp = createObject(ipType.getId(), "3.3.3.3");
    ObjectRecord someDomain = createObject(domainType.getId(), "testing.org");

    objectFactDao.storeObject(someIp);
    objectFactDao.storeObject(someDomain);

    // Create and retract a fact
    FactRecord retractedFact = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(resolveType.getId())
            .setAccessMode(FactRecord.AccessMode.Public)
            .setValue("to")
            .setSourceObject(someIp)
            .setDestinationObject(someDomain)
            .setLastSeenTimestamp(NOW);
    objectFactDao.storeFact(retractedFact);
    retractFact(retractedFact);

    // Include retracted
    assertEquals(1, IteratorUtils.count(createGraph(traverseParamsBuilder()
            .setIncludeRetracted(true)
            .build()).V(someIp.getId()).outE("resolve")));

    // Don't include retracted
    assertEquals(0, IteratorUtils.count(createGraph(traverseParamsBuilder()
            .setIncludeRetracted(false)
            .build()).V(someIp.getId()).outE("resolve")));
  }

  private TraverseParams.Builder traverseParamsBuilder() {
    return TraverseParams.builder()
            .setBaseSearchCriteria(factSearchCriteria);
  }

  private GraphTraversalSource createGraph() {
    return createGraph(traverseParamsBuilder().build());
  }

  private GraphTraversalSource createGraph(TraverseParams traverseParams) {
    Injector injector = daoBeanProvider.getInjector();
    return ObjectFactGraph.builder()
            .setObjectFactDao(objectFactDao)
            .setSecurityContext(mockSecurityContext)
            .setObjectTypeFactResolver(injector.getInstance(ObjectFactTypeResolver.class))
            .setFactRetractionHandler(injector.getInstance(FactRetractionHandler.class))
            .setPropertyHelper(injector.getInstance(PropertyHelper.class))
            .setTraverseParams(traverseParams)
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

  private void retractFact(FactRecord factToRetract) {
    FactTypeEntity retractFactType = daoBeanProvider.getInjector()
            .getInstance(FactTypeRequestResolver.class)
            .resolveRetractionFactType();
    FactRecord retractionFact = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(retractFactType.getId())
            .setInReferenceToID(factToRetract.getId())
            .setAccessMode(FactRecord.AccessMode.Public)
            .setLastSeenTimestamp(NOW);

    objectFactDao.storeFact(retractionFact);
    objectFactDao.retractFact(factToRetract);
  }

  private static class GraphModuleIT extends AbstractModule {
    @Override
    protected void configure() {
      install(new DaoModule());

      // Provide a noop implementation for the DC replication component (not needed here).
      bind(new TypeLiteral<Consumer<FactRecord>>() {}).toInstance(o -> {});
      // Simply use HashMap for the DAO caches.
      bind(new TypeLiteral<Map<UUID, ObjectRecord>>() {})
              .annotatedWith(DaoCache.class)
              .toInstance(new HashMap<>());
      bind(new TypeLiteral<Map<String, ObjectRecord>>() {})
              .annotatedWith(DaoCache.class)
              .toInstance(new HashMap<>());
      bind(new TypeLiteral<Map<UUID, FactRecord>>() {})
              .annotatedWith(DaoCache.class)
              .toInstance(new HashMap<>());
      bind(new TypeLiteral<Map<String, UUID>>() {})
              .annotatedWith(DaoCache.class)
              .toInstance(new HashMap<>());

      // Bind SecurityContext to a mock implementation.
      bind(SecurityContext.class).toProvider(() -> mockSecurityContext);
      bind(GrafeoSecurityContext.class).toProvider(() -> mockSecurityContext);
      // Mock a few resolvers which aren't actually needed during tests.
      bind(SubjectByIdResponseResolver.class).toInstance(mock(SubjectByIdResponseResolver.class));
      bind(OrganizationByIdResponseResolver.class).toInstance(mock(OrganizationByIdResponseResolver.class));
      bind(OriginByIdResponseResolver.class).toInstance(mock(OriginByIdResponseResolver.class));

      // FactRetractionHandler is configured to use the ServiceRequestScope. Just bind a dummy implementation to satisfy Guice.
      bindScope(ServiceRequestScope.class, new Scope() {
        @Override
        public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
          return unscoped;
        }
      });

      // Configuration required for Cassandra + ElasticSearch
      bind(String.class).annotatedWith(Names.named("grafeo.cassandra.data.center")).toInstance("datacenter1");
      bind(String.class).annotatedWith(Names.named("grafeo.cassandra.contact.points")).toInstance(cassandra.getExposedHost());
      bind(String.class).annotatedWith(Names.named("grafeo.cassandra.port")).toInstance(String.valueOf(cassandra.getExposedHostPort(9042)));
      bind(String.class).annotatedWith(Names.named("grafeo.elasticsearch.contact.points")).toInstance(elastic.getExposedHost());
      bind(String.class).annotatedWith(Names.named("grafeo.elasticsearch.port")).toInstance(String.valueOf(elastic.getExposedHostPort(9200)));
    }
  }
}
