package no.mnemonic.services.grafeo.rest.client;

import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.lambda.LambdaUtils;
import no.mnemonic.messaging.requestsink.jms.ExceptionMessage;
import no.mnemonic.messaging.requestsink.jms.JMSRequestSink;
import no.mnemonic.messaging.requestsink.jms.ProtocolVersion;
import no.mnemonic.messaging.requestsink.jms.serializer.MessageSerializer;
import no.mnemonic.messaging.requestsink.jms.serializer.XStreamMessageSerializer;
import no.mnemonic.services.common.api.ServiceTimeOutException;
import no.mnemonic.services.common.messagebus.ServiceMessageClient;
import no.mnemonic.services.common.messagebus.ServiceResponseValueMessage;
import no.mnemonic.services.common.messagebus.ServiceStreamingResultSetResponseMessage;
import no.mnemonic.services.grafeo.api.service.v1.GrafeoService;

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
public class GrafeoSmbClient implements Provider<GrafeoService>, LifecycleAspect {

  private static final String ACTIVEMQ_CONTEXT_FACTORY = "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
  private static final String ACTIVEMQ_CONNECTION_FACTORY = "ConnectionFactory";
  private static final int MAX_WAIT_MS = 2000;

  private static final Logger logger = Logging.getLogger(GrafeoSmbClient.class);

  private final AtomicReference<Instance> instance = new AtomicReference<>();

  private final String queueName;
  private final String topicName;
  private final String contextURL;
  private final String userName;
  private final String password;

  @Inject
  public GrafeoSmbClient(@Named(value = "grafeo.smb.queue.name") String queueName,
                         @Named(value = "grafeo.smb.topic.name") String topicName,
                         @Named(value = "grafeo.smb.client.url") String contextURL,
                         @Named(value = "grafeo.smb.client.username") String userName,
                         @Named(value = "grafeo.smb.client.password") String password) {
    this.queueName = queueName;
    this.topicName = topicName;
    this.contextURL = contextURL;
    this.userName = userName;
    this.password = password;
  }

  @Override
  public GrafeoService get() {
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
            .setConnectionProperty("topic." + topicName, topicName)
            .setQueueName(queueName)
            .setTopicName(topicName)
            .setContextURL(contextURL)
            .setUsername(userName)
            .setPassword(password)
            // At least V3 is required for custom serializers.
            .setProtocolVersion(ProtocolVersion.V3)
            .setSerializer(createSerializer())
            .build();

    ServiceMessageClient<GrafeoService> client = ServiceMessageClient.builder(GrafeoService.class)
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
            .addAllowedClass(ExceptionMessage.class)
            .addAllowedClass(ServiceResponseValueMessage.class)
            .addAllowedClass(ServiceStreamingResultSetResponseMessage.class)
            .addAllowedClass(ServiceTimeOutException.class)
            // Allow all response classes defined in the API (including exceptions).
            .addAllowedClass("no.mnemonic.services.grafeo.api.model.*")
            .addAllowedClass("no.mnemonic.services.grafeo.api.exceptions.*")
            .build();
  }

  private class Instance {
    private final JMSRequestSink requestSink;
    private final ServiceMessageClient<GrafeoService> messageClient;

    private Instance(JMSRequestSink requestSink, ServiceMessageClient<GrafeoService> messageClient) {
      this.requestSink = requestSink;
      this.messageClient = messageClient;
    }

    private JMSRequestSink getRequestSink() {
      return requestSink;
    }

    private ServiceMessageClient<GrafeoService> getMessageClient() {
      return messageClient;
    }
  }
}
