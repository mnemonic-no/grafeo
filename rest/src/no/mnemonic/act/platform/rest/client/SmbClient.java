package no.mnemonic.act.platform.rest.client;

import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.lambda.LambdaUtils;
import no.mnemonic.messaging.requestsink.jms.JMSRequestSink;
import no.mnemonic.messaging.requestsink.jms.ProtocolVersion;
import no.mnemonic.messaging.requestsink.jms.serializer.MessageSerializer;
import no.mnemonic.messaging.requestsink.jms.serializer.XStreamMessageSerializer;
import no.mnemonic.services.common.messagebus.ServiceMessageClient;
import no.mnemonic.services.common.messagebus.ServiceResponseValueMessage;
import no.mnemonic.services.common.messagebus.ServiceStreamingResultSetResponseMessage;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client-side implementation of the Service Message Bus using ActiveMQ.
 */
@Singleton
public class SmbClient implements Provider<ThreatIntelligenceService>, LifecycleAspect {

  private static final String ACTIVEMQ_CONTEXT_FACTORY = "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
  private static final String ACTIVEMQ_CONNECTION_FACTORY = "ConnectionFactory";
  private static final int MAX_WAIT_MS = 2000;

  private static final Logger logger = Logging.getLogger(SmbClient.class);

  private final AtomicReference<Instance> instance = new AtomicReference<>();

  private final String queueName;
  private final String contextURL;
  private final String userName;
  private final String password;

  @Inject
  public SmbClient(@Named(value = "smb.queue.name") String queueName,
                   @Named(value = "smb.client.url") String contextURL,
                   @Named(value = "smb.client.username") String userName,
                   @Named(value = "smb.client.password") String password) {
    this.queueName = queueName;
    this.contextURL = contextURL;
    this.userName = userName;
    this.password = password;
  }

  @Override
  public ThreatIntelligenceService get() {
    return instance.updateAndGet(i -> {
      if (i != null) return i;
      return setupInstance();
    }).getMessageClient().getInstance();
  }

  @Override
  public void startComponent() {
    get(); // This will instantiate and start the client components.
  }

  @Override
  public void stopComponent() {
    instance.updateAndGet(i -> {
      LambdaUtils.tryTo(() -> i.getRequestSink().stopComponent(), ex -> logger.error(ex, "Failed to cleanly shutdown request sink."));
      return null;
    });
  }

  private Instance setupInstance() {
    JMSRequestSink sink = JMSRequestSink.builder()
            .setContextFactoryName(ACTIVEMQ_CONTEXT_FACTORY)
            .setConnectionFactoryName(ACTIVEMQ_CONNECTION_FACTORY)
            .setConnectionProperty("queue." + queueName, queueName)
            .setDestinationName(queueName)
            .setContextURL(contextURL)
            .setUsername(userName)
            .setPassword(password)
            // At least V3 is required for custom serializers.
            .setProtocolVersion(ProtocolVersion.V3)
            .setSerializer(createSerializer())
            .build();

    ServiceMessageClient<ThreatIntelligenceService> client = ServiceMessageClient.builder(ThreatIntelligenceService.class)
            .setRequestSink(sink)
            .setMaxWait(MAX_WAIT_MS)
            .build();

    sink.startComponent();

    return new Instance(sink, client);
  }

  private MessageSerializer createSerializer() {
    // XStreamMessageSerializer is the only serializer supported by the server.
    return XStreamMessageSerializer.builder()
            // Common Java classes used in responses. Need to explicitly define Set/List because
            // XStream doesn't provide default converters for UnmodifiableSet/UnmodifiableList.
            .addAllowedClass(String.class)
            .addAllowedClass(UUID.class)
            .addAllowedClass(Set.class)
            .addAllowedClass(List.class)
            .addAllowedClass("java.util.Collections\\$EmptySet")
            .addAllowedClass("java.util.Collections\\$EmptyList")
            .addAllowedClass("java.util.Collections\\$UnmodifiableSet")
            .addAllowedClass("java.util.Collections\\$UnmodifiableList")
            // Response messages used by SMB.
            .addAllowedClass(ServiceResponseValueMessage.class)
            .addAllowedClass(ServiceStreamingResultSetResponseMessage.class)
            // Allow all response classes defined in the API (including exceptions).
            .addAllowedClass("no.mnemonic.act.platform.api.model.*")
            .addAllowedClass("no.mnemonic.act.platform.api.exceptions.*")
            .build();
  }

  private class Instance {
    private final JMSRequestSink requestSink;
    private final ServiceMessageClient<ThreatIntelligenceService> messageClient;

    private Instance(JMSRequestSink requestSink, ServiceMessageClient<ThreatIntelligenceService> messageClient) {
      this.requestSink = requestSink;
      this.messageClient = messageClient;
    }

    private JMSRequestSink getRequestSink() {
      return requestSink;
    }

    private ServiceMessageClient<ThreatIntelligenceService> getMessageClient() {
      return messageClient;
    }
  }
}
