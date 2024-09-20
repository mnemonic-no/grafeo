package no.mnemonic.services.grafeo.service.container;

import com.google.inject.Inject;
import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.metrics.MetricAspect;
import no.mnemonic.commons.metrics.MetricException;
import no.mnemonic.commons.metrics.Metrics;
import no.mnemonic.commons.metrics.MetricsGroup;
import no.mnemonic.commons.utilities.lambda.LambdaUtils;
import no.mnemonic.services.common.api.ServiceSessionFactory;
import no.mnemonic.services.common.api.proxy.serializer.Serializer;
import no.mnemonic.services.common.api.proxy.server.ServiceInvocationHandler;
import no.mnemonic.services.common.api.proxy.server.ServiceProxy;
import no.mnemonic.services.grafeo.api.service.v1.GrafeoService;

import javax.inject.Named;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server component exposing the {@link GrafeoService} via HTTP.
 */
public class GrafeoServiceProxyServer implements LifecycleAspect, MetricAspect {

  private static final Logger LOGGER = Logging.getLogger(GrafeoServiceProxyServer.class);

  @Dependency
  private final GrafeoService service;
  @Dependency
  private final ServiceSessionFactory sessionFactory;
  private final Serializer messageSerializer;

  private final int standardPort;
  private final int bulkPort;
  private final int expeditePort;

  private int maxConcurrentRequestsStandard = 20;
  private int maxConcurrentRequestsBulk = 5;
  private int maxConcurrentRequestsExpedite = 5;
  private int circuitBreakerLimit = 1;

  private ExecutorService executor;
  private ServiceInvocationHandler<GrafeoService> invocationHandler;
  private ServiceProxy serviceProxy;

  @Inject
  public GrafeoServiceProxyServer(
          GrafeoService service,
          ServiceSessionFactory sessionFactory,
          Serializer messageSerializer,
          @Named("grafeo.service.proxy.standard.port") int standardPort,
          @Named("grafeo.service.proxy.bulk.port") int bulkPort,
          @Named("grafeo.service.proxy.expedite.port") int expeditePort) {
    this.service = service;
    this.sessionFactory = sessionFactory;
    this.messageSerializer = messageSerializer;
    this.standardPort = standardPort;
    this.bulkPort = bulkPort;
    this.expeditePort = expeditePort;
  }

  @Override
  public void startComponent() {
    executor = Executors.newFixedThreadPool(maxConcurrentRequestsStandard + maxConcurrentRequestsBulk + maxConcurrentRequestsExpedite);
    invocationHandler = ServiceInvocationHandler.<GrafeoService>builder()
            .setExecutorService(executor)
            .setProxiedService(service)
            .setSessionFactory(sessionFactory)
            .addSerializer(messageSerializer)
            .build();
    serviceProxy = ServiceProxy.builder()
            .addInvocationHandler(GrafeoService.class, invocationHandler)
            .setStandardPort(standardPort)
            .setBulkPort(bulkPort)
            .setExpeditePort(expeditePort)
            .setStandardThreads(maxConcurrentRequestsStandard)
            .setBulkThreads(maxConcurrentRequestsBulk)
            .setExpediteThreads(maxConcurrentRequestsExpedite)
            .setCircuitBreakerLimit(circuitBreakerLimit)
            .build();
    serviceProxy.startComponent();
  }

  @Override
  public void stopComponent() {
    LambdaUtils.tryTo(serviceProxy::stopComponent, ex -> LOGGER.error(ex, "Failed to cleanly shutdown service proxy."));
    LambdaUtils.tryTo(executor::shutdown, ex -> LOGGER.error(ex, "Failed to cleanly shutdown executor service."));
  }

  @Override
  public Metrics getMetrics() throws MetricException {
    MetricsGroup metrics = new MetricsGroup();
    if (serviceProxy != null) metrics.addSubMetrics("serviceProxy", serviceProxy.getMetrics());
    if (invocationHandler != null) metrics.addSubMetrics("invocationHandler", invocationHandler.getMetrics());
    if (messageSerializer != null) metrics.addSubMetrics("xstreamMessageSerializer", messageSerializer.getMetrics());

    return metrics;
  }

  @Inject(optional = true)
  public GrafeoServiceProxyServer setMaxConcurrentRequestsStandard(
          @Named("grafeo.service.proxy.max.concurrent.requests.standard") int maxConcurrentRequestsStandard) {
    this.maxConcurrentRequestsStandard = maxConcurrentRequestsStandard;
    return this;
  }

  @Inject(optional = true)
  public GrafeoServiceProxyServer setMaxConcurrentRequestsBulk(
          @Named("grafeo.service.proxy.max.concurrent.requests.bulk") int maxConcurrentRequestsBulk) {
    this.maxConcurrentRequestsBulk = maxConcurrentRequestsBulk;
    return this;
  }

  @Inject(optional = true)
  public GrafeoServiceProxyServer setMaxConcurrentRequestsExpedite(
          @Named("grafeo.service.proxy.max.concurrent.requests.expedite") int maxConcurrentRequestsExpedite) {
    this.maxConcurrentRequestsExpedite = maxConcurrentRequestsExpedite;
    return this;
  }

  @Inject(optional = true)
  public GrafeoServiceProxyServer setCircuitBreakerLimit(
          @Named("grafeo.service.proxy.circuit.breaker.limit") int circuitBreakerLimit) {
    this.circuitBreakerLimit = circuitBreakerLimit;
    return this;
  }
}
