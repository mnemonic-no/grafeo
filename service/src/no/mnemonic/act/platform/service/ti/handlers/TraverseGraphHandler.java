package no.mnemonic.act.platform.service.ti.handlers;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.OperationTimeoutException;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.FactResponseConverter;
import no.mnemonic.act.platform.service.ti.converters.response.ObjectResponseConverter;
import no.mnemonic.act.platform.service.ti.helpers.GremlinSandboxExtension;
import no.mnemonic.act.platform.service.ti.tinkerpop.ActGraph;
import no.mnemonic.act.platform.service.ti.tinkerpop.FactEdge;
import no.mnemonic.act.platform.service.ti.tinkerpop.ObjectVertex;
import no.mnemonic.act.platform.service.ti.tinkerpop.TraverseParams;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.MapUtils;
import no.mnemonic.services.common.api.ResultSet;
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
import java.util.function.Consumer;

import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;

public class TraverseGraphHandler {

  private static final String SCRIPT_ENGINE = "gremlin-groovy";
  private static final long SCRIPT_EXECUTION_TIMEOUT = 120_000;

  private final TiSecurityContext securityContext;
  private final ObjectFactDao objectFactDao;
  private final ObjectFactTypeResolver objectFactTypeResolver;
  private final ObjectResponseConverter objectResponseConverter;
  private final FactResponseConverter factResponseConverter;

  private long scriptExecutionTimeout = SCRIPT_EXECUTION_TIMEOUT;

  @Inject
  public TraverseGraphHandler(TiSecurityContext securityContext,
                              ObjectFactDao objectFactDao,
                              ObjectFactTypeResolver objectFactTypeResolver,
                              ObjectResponseConverter objectResponseConverter,
                              FactResponseConverter factResponseConverter) {
    this.securityContext = securityContext;
    this.objectFactDao = objectFactDao;
    this.objectFactTypeResolver = objectFactTypeResolver;
    this.objectResponseConverter = objectResponseConverter;
    this.factResponseConverter = factResponseConverter;
  }

  /**
   * Traverse a graph by running the query starting at the provided startingObjects.
   * <p>
   * NB! This methods assumes that the caller has verified the following:
   * - that the objects exist
   * - that the user has access to all objects.
   *
   * @param startingObjects Start the query from these objects
   * @param query           The query to run
   * @param traverseParams  Configuration of the traversal
   * @return The result from running the query
   * @throws OperationTimeoutException Thrown if the traversale takes longer than the configured timeout
   * @throws InvalidArgumentException
   */
  public ResultSet<?> traverse(Collection<UUID> startingObjects, String query, TraverseParams traverseParams) throws OperationTimeoutException,
          InvalidArgumentException {

    if (CollectionUtils.isEmpty(startingObjects)) {
      // Search returned no results, just return empty traversal result as well.
      return StreamingResultSet.builder().build();
    }

    // Execute traversal and process results.
    Collection<Object> result = executeTraversal(startingObjects, query, traverseParams);

    return StreamingResultSet.builder()
            .setCount(result.size())
            .setValues(result)
            .build();
  }

  TraverseGraphHandler setScriptExecutionTimeout(long scriptExecutionTimeout) {
    this.scriptExecutionTimeout = scriptExecutionTimeout;
    return this;
  }

  private Collection<Object> executeTraversal(Collection<UUID> startingObjects, String query,
                                                        TraverseParams traverseParams)
          throws InvalidArgumentException, OperationTimeoutException {

    // The result will be written into this collection.
    Collection<Object> traversalResult = new ArrayList<>();

    try (Graph graph = createGraph(traverseParams); GremlinExecutor executor = createExecutor()) {
      // Create the first step of the graph traversal, i.e. starting the traversal at the Object(s) specified in the request.
      // This is injected into the script execution as variable 'g'. Every query has to start from 'g'.
      GraphTraversal<Vertex, Vertex> startingPoint = graph.traversal().V(startingObjects.toArray());
      Map<String, Object> bindings = MapUtils.map(T("g", startingPoint));
      // Start script execution and wait until result arrived or execution is aborted.
      // Use 'withResult' callback here because the graph will then be iterated inside the 'eval' thread, thus, every
      // exception caused by the traversal will be handled inside that thread as well which will result in an ExecutionException.
      executor.eval(query, SCRIPT_ENGINE, bindings, createResultConsumer(traversalResult)).get();
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

    return traversalResult;
  }

  /**
   * Create a function to process the graph traversal result. The result will be written into the
   * traversalResult collection provided as a parameter
   *
   * @param traversalResult Write the result set into the traversalResult collection
   * @return A function that will process the result from the graph traversal.
   */
  private Consumer<Object> createResultConsumer(Collection<Object> traversalResult) {
    return (Object result) -> {
      // The result of the graph traversal will be an iterator, thus, convert result to an iterator here.
      Iterator<?> resultIterator = IteratorUtils.asIterator(result);
      // Iterate result and convert values if necessary. This will perform the actual graph traversal.
      resultIterator.forEachRemaining(value -> {
        Object apiValue = tinkerpopToApi(value);
        if (apiValue != null) {
          traversalResult.add(apiValue);
        }
      });
    };
  }

  /**
   * Maps from Tinkerpop objects to the API model
   *
   * @param value A Tinkerpop object
   * @return API Facts, Objects or strings
   */
  private Object tinkerpopToApi(Object value) {
    if (value instanceof ObjectVertex) {
      // Fetch ObjectRecord and convert to Object model before adding to result. Avoid explicitly checking access to
      // Object and rely on access control implemented in graph traversal only. Checking this would be too expensive
      // because it requires fetching Facts for each Object. In addition, accidentally returning non-accessible
      // Objects will only leak the information that the Object exists and will not give further access to any Facts.
      return objectResponseConverter.apply(ObjectVertex.class.cast(value).getObjectRecord());
    } else if (value instanceof FactEdge) {
      // Convert to Fact model before adding to result.
      FactRecord fact = FactEdge.class.cast(value).getFactRecord();
      // But only add it if user has access to the Fact. Skip Fact otherwise.
      if (securityContext.hasReadPermission(fact)) {
        return factResponseConverter.apply(fact);
      }
    } else {
      // Don't know what this is, just add its string representation to result.
      // For example, it could be a query returning a list of properties.
      // This mimics the behaviour of gremlin-console and avoids returning arbitrary JSON objects.
      return value.toString();
    }
    return null;
  }

  private Graph createGraph(TraverseParams traverseParams) {
    return ActGraph.builder()
            .setObjectFactDao(objectFactDao)
            .setObjectTypeFactResolver(objectFactTypeResolver)
            .setSecurityContext(securityContext)
            .setTraverseParams(traverseParams)
            .build();
  }

  private GremlinExecutor createExecutor() {
    Map<String, Object> groovyCompilerConfig = MapUtils.map(
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
