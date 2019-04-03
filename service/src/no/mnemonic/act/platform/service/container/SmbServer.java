package no.mnemonic.act.platform.service.container;

import no.mnemonic.act.platform.api.service.v1.RequestHeader;
import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.act.platform.auth.properties.model.SubjectCredentials;
import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.lambda.LambdaUtils;
import no.mnemonic.messaging.requestsink.jms.JMSRequestProxy;
import no.mnemonic.messaging.requestsink.jms.serializer.MessageSerializer;
import no.mnemonic.messaging.requestsink.jms.serializer.XStreamMessageSerializer;
import no.mnemonic.services.common.api.ServiceSessionFactory;
import no.mnemonic.services.common.messagebus.ServiceMessageHandler;
import no.mnemonic.services.common.messagebus.ServiceRequestMessage;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.UUID;

/**
 * Server-side implementation of the Service Message Bus using ActiveMQ.
 */
public class SmbServer implements LifecycleAspect {

  private static final String ACTIVEMQ_CONTEXT_FACTORY = "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
  private static final String ACTIVEMQ_CONNECTION_FACTORY = "ConnectionFactory";

  private static final Logger logger = Logging.getLogger(SmbServer.class);

  @Dependency
  private final ThreatIntelligenceService service;
  @Dependency
  private final ServiceSessionFactory sessionFactory;

  private final String queueName;
  private final String contextURL;
  private final String userName;
  private final String password;

  private ServiceMessageHandler messageHandler;
  private JMSRequestProxy requestProxy;

  @Inject
  public SmbServer(ThreatIntelligenceService service,
                   ServiceSessionFactory sessionFactory,
                   @Named(value = "smb.queue.name") String queueName,
                   @Named(value = "smb.server.url") String contextURL,
                   @Named(value = "smb.server.username") String userName,
                   @Named(value = "smb.server.password") String password) {
    this.service = service;
    this.sessionFactory = sessionFactory;
    this.queueName = queueName;
    this.contextURL = contextURL;
    this.userName = userName;
    this.password = password;
  }

  @Override
  public void startComponent() {
    messageHandler = ServiceMessageHandler.builder()
            .setService(service)
            .setSessionFactory(sessionFactory)
            .build();
    requestProxy = JMSRequestProxy.builder()
            .setRequestSink(messageHandler)
            .setContextFactoryName(ACTIVEMQ_CONTEXT_FACTORY)
            .setConnectionFactoryName(ACTIVEMQ_CONNECTION_FACTORY)
            .setConnectionProperty("queue." + queueName, queueName)
            .setDestinationName(queueName)
            .setContextURL(contextURL)
            .setUsername(userName)
            .setPassword(password)
            .addSerializer(createSerializer())
            .build();

    messageHandler.startComponent();
    requestProxy.startComponent();
  }

  @Override
  public void stopComponent() {
    LambdaUtils.tryTo(requestProxy::stopComponent, ex -> logger.error(ex, "Failed to cleanly shutdown request proxy."));
    LambdaUtils.tryTo(messageHandler::stopComponent, ex -> logger.error(ex, "Failed to cleanly shutdown message handler."));
  }

  private MessageSerializer createSerializer() {
    return XStreamMessageSerializer.builder()
            // Common Java classes used in requests (required for collections holding those).
            .addAllowedClass(String.class)
            .addAllowedClass(UUID.class)
            // Request message used by SMB.
            .addAllowedClass(ServiceRequestMessage.class)
            // RequestHeader and SubjectCredentials are part of every service request.
            .addAllowedClass(RequestHeader.class)
            .addAllowedClass(SubjectCredentials.class)
            // Allow all request classes defined in the API.
            .addAllowedClass("no.mnemonic.act.platform.api.request.*")
            .build();
  }
}
