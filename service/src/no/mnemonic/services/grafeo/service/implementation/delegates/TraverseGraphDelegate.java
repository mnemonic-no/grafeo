package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.MapUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.OperationTimeoutException;
import no.mnemonic.services.grafeo.api.model.v1.Object;
import no.mnemonic.services.grafeo.api.request.v1.TraverseByObjectIdRequest;
import no.mnemonic.services.grafeo.api.request.v1.TraverseByObjectSearchRequest;
import no.mnemonic.services.grafeo.api.request.v1.TraverseByObjectTypeValueRequest;
import no.mnemonic.services.grafeo.api.service.v1.StreamingResultSet;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.tinkerpop.FactEdge;
import no.mnemonic.services.grafeo.dao.tinkerpop.ObjectFactGraph;
import no.mnemonic.services.grafeo.dao.tinkerpop.ObjectVertex;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.FactResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.converters.response.ObjectResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.handlers.ObjectTypeHandler;
import no.mnemonic.services.grafeo.service.implementation.helpers.GremlinSandboxExtension;
import org.apache.tinkerpop.gremlin.groovy.engine.GremlinExecutor;
import org.apache.tinkerpop.gremlin.groovy.jsr223.GroovyCompilerGremlinPlugin;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;

/**
 * @deprecated see {@link TraverseByObjectsDelegate} and {@link TraverseByObjectSearchDelegate}
 */
@Deprecated
public class TraverseGraphDelegate implements Delegate {

  private static final String SCRIPT_ENGINE = "gremlin-groovy";
  private static final long SCRIPT_EXECUTION_TIMEOUT = 120_000;

  private final GrafeoSecurityContext securityContext;
  private final ObjectFactDao objectFactDao;
  private final ObjectManager objectManager;
  private final FactManager factManager;
  private final ObjectSearchDelegate objectSearch;
  private final ObjectResponseConverter objectResponseConverter;
  private final FactResponseConverter factResponseConverter;
  private final ObjectTypeHandler objectTypeHandler;

  private final Collection<java.lang.Object> traversalResult = new ArrayList<>();

  private long scriptExecutionTimeout = SCRIPT_EXECUTION_TIMEOUT;

  @Inject
  public TraverseGraphDelegate(GrafeoSecurityContext securityContext,
                               ObjectFactDao objectFactDao,
                               ObjectManager objectManager,
                               FactManager factManager,
                               ObjectSearchDelegate objectSearch,
                               ObjectResponseConverter objectResponseConverter,
                               FactResponseConverter factResponseConverter,
                               ObjectTypeHandler objectTypeHandler) {
    this.securityContext = securityContext;
    this.objectFactDao = objectFactDao;
    this.objectManager = objectManager;
    this.factManager = factManager;
    this.objectSearch = objectSearch;
    this.objectResponseConverter = objectResponseConverter;
    this.factResponseConverter = factResponseConverter;
    this.objectTypeHandler = objectTypeHandler;
  }

  public ResultSet<?> handle(TraverseByObjectIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    securityContext.checkPermission(FunctionConstants.traverseThreatIntelFact);

    return handle(objectFactDao.getObject(request.getId()), request.getQuery());
  }

  public ResultSet<?> handle(TraverseByObjectTypeValueRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    securityContext.checkPermission(FunctionConstants.traverseThreatIntelFact);
    objectTypeHandler.assertObjectTypeExists(request.getType(), "type");

    return handle(objectFactDao.getObject(request.getType(), request.getValue()), request.getQuery());
  }

  public ResultSet<?> handle(TraverseByObjectSearchRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    securityContext.checkPermission(FunctionConstants.traverseThreatIntelFact);

    // Search for Objects and use the result as starting points for the graph traversal.
    // The search will only return Objects the current user has access to, thus, there is no need to check
    // Object access here (in contrast to the traversal with a single starting Object).
    Collection<UUID> startingObjects = SetUtils.set(objectSearch.handle(request).iterator(), Object::getId);
    if (startingObjects.isEmpty()) {
      // Search returned no results, just return empty traversal result as well.
      return StreamingResultSet.builder().build();
    }

    // Execute traversal and process results.
    executeTraversal(startingObjects, request.getQuery());

    return StreamingResultSet.builder()
            .setCount(traversalResult.size())
            .setValues(traversalResult)
            .build();
  }

  TraverseGraphDelegate setScriptExecutionTimeout(long scriptExecutionTimeout) {
    this.scriptExecutionTimeout = scriptExecutionTimeout;
    return this;
  }

  private ResultSet<?> handle(ObjectRecord startingObject, String query)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    // Verify that user has access to starting point of graph traversal.
    securityContext.checkReadPermission(startingObject);
    // Execute traversal and process results.
    executeTraversal(Collections.singleton(startingObject.getId()), query);

    return StreamingResultSet.builder()
            .setCount(traversalResult.size())
            .setValues(traversalResult)
            .build();
  }

  private void executeTraversal(Collection<UUID> startingObjects, String query)
          throws InvalidArgumentException, OperationTimeoutException {
    try (Graph graph = createGraph(); GremlinExecutor executor = createExecutor()) {
      // Create the first step of the graph traversal, i.e. starting the traversal at the Object(s) specified in the request.
      // This is injected into the script execution as variable 'g'. Every query has to start from 'g'.
      GraphTraversal<Vertex, Vertex> startingPoint = graph.traversal().V(startingObjects.toArray());
      Map<String, java.lang.Object> bindings = MapUtils.map(T("g", startingPoint));
      // Start script execution and wait until result arrived or execution is aborted.
      // Use 'withResult' callback here because the graph will then be iterated inside the 'eval' thread, thus, every
      // exception caused by the traversal will be handled inside that thread as well which will result in an ExecutionException.
      executor.eval(query, SCRIPT_ENGINE, bindings, this::produceTraversalResult).get();
    } catch (ExecutionException ex) {
      // Exceptions causing the script execution to fail are wrapped inside an ExecutionException. Need to unwrap them.
      Throwable cause = ObjectUtils.ifNull(ex.getCause(), ex);
      // A TimeoutException will be thrown when either the GremlinExecutor or the Groovy sandbox abort the script execution.
      // In both cases throw an own OperationTimeoutException in order to signal the timeout to the user.
      if (cause instanceof TimeoutException) {
        throw new OperationTimeoutException("The performed graph traversal query timed out.", "graph.traversal.timeout");
      }
      // In all other cases throw an InvalidArgumentException because the failure is most likely caused by a wrong query,
      // e.g. invalid syntax, an unsupported operation such as 'addE()', or an operation not allowed by the sandbox.
      throw new InvalidArgumentException()
              .addValidationError(cause.getMessage(), "graph.traversal.failure", "query", query);
    } catch (Exception ex) {
      // Something bad happened, abort method.
      throw new IllegalStateException("Could not perform graph traversal.", ex);
    }
  }

  private void produceTraversalResult(java.lang.Object result) {
    // The result of the graph traversal will be an iterator, thus, convert result to an iterator here.
    Iterator<?> resultIterator = IteratorUtils.asIterator(result);
    // Iterate result and convert values if necessary. This will perform the actual graph traversal.
    resultIterator.forEachRemaining(value -> {
      if (value instanceof ObjectVertex) {
        // Fetch ObjectRecord and convert to Object model before adding to result. Avoid explicitly checking access to
        // Object and rely on access control implemented in graph traversal only. Checking this would be too expensive
        // because it requires fetching Facts for each Object. In addition, accidentally returning non-accessible
        // Objects will only leak the information that the Object exists and will not give further access to any Facts.
        ObjectRecord object = objectFactDao.getObject(ObjectVertex.class.cast(value).getObject().getId());
        traversalResult.add(objectResponseConverter.apply(object));
      } else if (value instanceof FactEdge) {
        // Fetch FactRecord and convert to Fact model before adding to result.
        FactRecord fact = objectFactDao.getFact(FactEdge.class.cast(value).getFact().getId());
        // But only add it if user has access to the Fact. Skip Fact otherwise.
        if (securityContext.hasReadPermission(fact)) {
          traversalResult.add(factResponseConverter.apply(fact));
        }
      } else {
        // Don't know what this is, just add its string representation to result.
        // For example, it could be a query returning a list of properties.
        // This mimics the behaviour of gremlin-console and avoids returning arbitrary JSON objects.
        traversalResult.add(value.toString());
      }
    });
  }

  private Graph createGraph() {
    return ObjectFactGraph.builder()
            .setObjectManager(objectManager)
            .setFactManager(factManager)
            .setHasFactAccess(securityContext::hasReadPermission)
            .build();
  }

  private GremlinExecutor createExecutor() {
    Map<String, java.lang.Object> groovyCompilerConfig = MapUtils.map(
            // Protect against scripts going haywire (endless loops, etc.).
            T("timedInterrupt", scriptExecutionTimeout),
            // Statically compile scripts before execution (needed for sandbox).
            T("compilation", GroovyCompilerGremlinPlugin.Compilation.COMPILE_STATIC),
            // Execute scripts inside a sandbox (i.e. only allow whitelisted methods).
            T("extensions", GremlinSandboxExtension.class.getName())
    );

    return GremlinExecutor.build()
            .evaluationTimeout(scriptExecutionTimeout)
            .addPlugins(SCRIPT_ENGINE, MapUtils.map(T(GroovyCompilerGremlinPlugin.class.getName(), groovyCompilerConfig)))
            .create();
  }
}
