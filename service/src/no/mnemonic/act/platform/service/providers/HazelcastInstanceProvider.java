package no.mnemonic.act.platform.service.providers;

import com.hazelcast.config.Config;
import com.hazelcast.config.JavaSerializationFilterConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import no.mnemonic.act.platform.seb.model.v1.FactSEB;
import no.mnemonic.commons.component.LifecycleAspect;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicReference;

import static no.mnemonic.act.platform.seb.esengine.v1.handlers.FactKafkaToHazelcastHandler.FACT_HAZELCAST_QUEUE_NAME;

@Singleton
public class HazelcastInstanceProvider implements LifecycleAspect, Provider<HazelcastInstance> {

  private final AtomicReference<HazelcastInstance> instance = new AtomicReference<>();

  private final String instanceName;
  private final String groupName;
  private final String multicastAddress;
  private final int multicastPort;
  private final boolean multicastEnabled;

  @Inject
  public HazelcastInstanceProvider(
          @Named("act.hazelcast.instance.name") String instanceName,
          @Named("act.hazelcast.group.name") String groupName,
          @Named("act.hazelcast.multicast.address") String multicastAddress,
          @Named("act.hazelcast.multicast.port") int multicastPort,
          @Named("act.hazelcast.multicast.enabled") boolean multicastEnabled
  ) {
    this.instanceName = instanceName;
    this.groupName = groupName;
    this.multicastAddress = multicastAddress;
    this.multicastPort = multicastPort;
    this.multicastEnabled = multicastEnabled;
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
    // Disable Hazelcast's own shutdown hook because termination must be handle by the LifecycleAspect.
    cfg.setProperty("hazelcast.shutdownhook.enabled", "false");

    applyMulticastConfig(cfg);
    applySerializationConfig(cfg);
    applyQueueConfig(cfg);

    return Hazelcast.getOrCreateHazelcastInstance(cfg);
  }

  private void applyMulticastConfig(Config cfg) {
    cfg.getNetworkConfig().getJoin().getMulticastConfig()
            .setEnabled(multicastEnabled)
            .setMulticastGroup(multicastAddress)
            .setMulticastPort(multicastPort);
  }

  private void applySerializationConfig(Config cfg) {
    // Configure serializers for all classes handled by Hazelcast.
    cfg.getSerializationConfig().addSerializerConfig(new SerializerConfig()
            .setTypeClass(FactSEB.class)
            .setImplementation(new HazelcastFactSebSerializer())
    );

    // Only allow well-known classes to be deserialized, especially it shouldn't be allowed to deserialize
    // arbitrary classes implementing Serializable to avoid deserialization vulnerabilities.
    JavaSerializationFilterConfig filterCfg = new JavaSerializationFilterConfig().setDefaultsDisabled(true);
    filterCfg.getWhitelist().addClasses(FactSEB.class.getName());
    cfg.getSerializationConfig().setJavaSerializationFilterConfig(filterCfg);
  }

  private void applyQueueConfig(Config cfg) {
    // Configure the specifics of each Hazelcast queue.
    cfg.getQueueConfig(FACT_HAZELCAST_QUEUE_NAME)
            .setBackupCount(1)
            .setMaxSize(1_000);
  }
}
