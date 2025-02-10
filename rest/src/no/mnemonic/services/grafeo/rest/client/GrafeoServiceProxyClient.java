package no.mnemonic.services.grafeo.rest.client;

import com.google.inject.Inject;
import no.mnemonic.commons.metrics.MetricAspect;
import no.mnemonic.commons.metrics.MetricException;
import no.mnemonic.commons.metrics.Metrics;
import no.mnemonic.commons.metrics.MetricsGroup;
import no.mnemonic.services.common.api.ServiceTimeOutException;
import no.mnemonic.services.common.api.proxy.client.ServiceClient;
import no.mnemonic.services.common.api.proxy.client.ServiceV1HttpClient;
import no.mnemonic.services.common.api.proxy.serializer.Serializer;
import no.mnemonic.services.common.api.proxy.serializer.XStreamSerializer;
import no.mnemonic.services.grafeo.api.service.v1.GrafeoService;

import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client component accessing the {@link GrafeoService} via HTTP.
 */
@Singleton
public class GrafeoServiceProxyClient implements Provider<GrafeoService>, MetricAspect {

  private final AtomicReference<Instance> instance = new AtomicReference<>();

  private final String baseURI;
  private final int standardPort;
  private final int bulkPort;
  private final int expeditePort;

  private int maxConcurrentRequests = 20;

  @Inject
  public GrafeoServiceProxyClient(
          @Named("grafeo.service.client.base.uri") String baseURI,
          @Named("grafeo.service.client.standard.port") int standardPort,
          @Named("grafeo.service.client.bulk.port") int bulkPort,
          @Named("grafeo.service.client.expedite.port") int expeditePort) {
    this.baseURI = baseURI;
    this.standardPort = standardPort;
    this.bulkPort = bulkPort;
    this.expeditePort = expeditePort;
  }

  @Override
  public GrafeoService get() {
    return instance.updateAndGet(i -> {
      if (i != null) return i;
      return setupInstance();
    }).serviceClient().getInstance();
  }

  @Override
  public Metrics getMetrics() throws MetricException {
    MetricsGroup metrics = new MetricsGroup();
    if (instance.get() != null) {
      metrics.addSubMetrics("serviceClient", instance.get().serviceClient().getMetrics());
      metrics.addSubMetrics("httpClient", instance.get().httpClient().getMetrics());
    }

    return metrics;
  }

  private Instance setupInstance() {
    ServiceV1HttpClient httpClient = ServiceV1HttpClient.builder()
            .setBaseURI(baseURI)
            .setStandardPort(standardPort)
            .setBulkPort(bulkPort)
            .setExpeditePort(expeditePort)
            .setMaxConnections(maxConcurrentRequests)
            .build();
    ServiceClient<GrafeoService> serviceClient = ServiceClient.<GrafeoService>builder()
            .setProxyInterface(GrafeoService.class)
            .setV1HttpClient(httpClient)
            .setSerializer(createXStreamSerializer())
            .build();
    return new Instance(serviceClient, httpClient);
  }

  private Serializer createXStreamSerializer() {
    // XStreamSerializer is the only serializer supported by the server.
    return XStreamSerializer.builder()
            // Common Java classes used in responses. Need to explicitly define Set/List because
            // XStream doesn't provide default converters for UnmodifiableSet/UnmodifiableList.
            .setAllowedClass(String.class)
            .setAllowedClass(UUID.class)
            .setAllowedClass(Set.class)
            .setAllowedClass(List.class)
            .setAllowedClassesRegex("java.util.Collections\\$EmptySet")
            .setAllowedClassesRegex("java.util.Collections\\$EmptyList")
            .setAllowedClassesRegex("java.util.Collections\\$UnmodifiableSet")
            .setAllowedClassesRegex("java.util.Collections\\$UnmodifiableList")
            // Allow all response classes defined in the API (including exceptions).
            .setAllowedClass(ServiceTimeOutException.class)
            .setAllowedClassesRegex("no.mnemonic.services.grafeo.api.model.*")
            .setAllowedClassesRegex("no.mnemonic.services.grafeo.api.exceptions.*")
            .build();
  }

  private record Instance(ServiceClient<GrafeoService> serviceClient, ServiceV1HttpClient httpClient) {
  }

  @Inject(optional = true)
  public GrafeoServiceProxyClient setMaxConcurrentRequests(
          @Named("grafeo.service.client.max.concurrent.requests") int maxConcurrentRequests) {
    this.maxConcurrentRequests = maxConcurrentRequests;
    return this;
  }
}
