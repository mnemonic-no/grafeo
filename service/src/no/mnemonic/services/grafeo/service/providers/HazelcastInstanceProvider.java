package no.mnemonic.services.grafeo.service.providers;

import com.hazelcast.config.Config;
import com.hazelcast.config.JavaSerializationFilterConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import no.mnemonic.commons.component.LifecycleAspect;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class HazelcastInstanceProvider implements LifecycleAspect, Provider<HazelcastInstance> {

  private final AtomicReference<HazelcastInstance> instance = new AtomicReference<>();

  private final String instanceName;
  private final String groupName;
  private final String multicastAddress;
  private final int multicastPort;
  private final boolean multicastEnabled;
  private final HazelcastServiceConfiguration serviceConfig;

  @Inject
  public HazelcastInstanceProvider(
          @Named("grafeo.hazelcast.instance.name") String instanceName,
          @Named("grafeo.hazelcast.group.name") String groupName,
          @Named("grafeo.hazelcast.multicast.address") String multicastAddress,
          @Named("grafeo.hazelcast.multicast.port") int multicastPort,
          @Named("grafeo.hazelcast.multicast.enabled") boolean multicastEnabled,
          HazelcastServiceConfiguration serviceConfig
  ) {
    this.instanceName = instanceName;
    this.groupName = groupName;
    this.multicastAddress = multicastAddress;
    this.multicastPort = multicastPort;
    this.multicastEnabled = multicastEnabled;
    this.serviceConfig = serviceConfig;
  }

  @Override
  public void startComponent() {
    get(); // Force initialization on startup.
  }

  @Override
  public void stopComponent() {
    instance.updateAndGet(existing -> {
      if (existing != null) existing.shutdown();
      return null;
    });
  }

  @Override
  public HazelcastInstance get() {
    return instance.updateAndGet(existing -> {
      if (existing != null) return existing;
      return createInstance();
    });
  }

  private HazelcastInstance createInstance() {
    Config cfg = new Config(instanceName);
    cfg.getGroupConfig().setName(groupName);

    // Specify log4j2 to collect the Hazelcast logs together with the service logs (instead of stdout).
    cfg.setProperty("hazelcast.logging.type", "log4j2");
    // Disable Hazelcast's own shutdown hook because termination must be handled by the LifecycleAspect.
    cfg.setProperty("hazelcast.shutdownhook.enabled", "false");

    // Specify network configuration for multicast.
    cfg.getNetworkConfig().getJoin().getMulticastConfig()
            .setEnabled(multicastEnabled)
            .setMulticastGroup(multicastAddress)
            .setMulticastPort(multicastPort);
    // Only allow well-known classes to be deserialized, especially it shouldn't be allowed to deserialize arbitrary
    // classes implementing Serializable to avoid deserialization vulnerabilities. This uses the default whitelist
    // provided in Hazelcast. It's not necessary to allow service specific classes because they are using custom
    // serializers based on JSON and don't implement Serializable.
    cfg.getSerializationConfig().setJavaSerializationFilterConfig(new JavaSerializationFilterConfig());

    // Apply configuration required by the service implementation (queues, maps, etc).
    serviceConfig.apply(cfg);

    return Hazelcast.getOrCreateHazelcastInstance(cfg);
  }
}
