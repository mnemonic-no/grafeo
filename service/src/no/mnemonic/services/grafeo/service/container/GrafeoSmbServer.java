package no.mnemonic.services.grafeo.service.container;

import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.lambda.LambdaUtils;
import no.mnemonic.messaging.requestsink.jms.JMSRequestProxy;
import no.mnemonic.messaging.requestsink.jms.serializer.MessageSerializer;
import no.mnemonic.services.common.api.ServiceSessionFactory;
import no.mnemonic.services.common.messagebus.ServiceMessageHandler;
import no.mnemonic.services.grafeo.api.service.v1.GrafeoService;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Server-side implementation of the Service Message Bus using ActiveMQ.
 */
public class GrafeoSmbServer implements LifecycleAspect {

  private static final String ACTIVEMQ_CONTEXT_FACTORY = "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
  private static final String ACTIVEMQ_CONNECTION_FACTORY = "ConnectionFactory";

  private static final Logger logger = Logging.getLogger(GrafeoSmbServer.class);

  @Dependency
  private final GrafeoService service;
  @Dependency
  private final ServiceSessionFactory sessionFactory;

  private final MessageSerializer messageSerializer;

  private final String queueName;
  private final String contextURL;
  private final String userName;
  private final String password;

  private ServiceMessageHandler messageHandler;
  private JMSRequestProxy requestProxy;

  @Inject
  public GrafeoSmbServer(GrafeoService service,
                         ServiceSessionFactory sessionFactory,
                         MessageSerializer messageSerializer,
                         @Named(value = "grafeo.smb.queue.name") String queueName,
                         @Named(value = "grafeo.smb.server.url") String contextURL,
                         @Named(value = "grafeo.smb.server.username") String userName,
                         @Named(value = "grafeo.smb.server.password") String password) {
    this.service = service;
    this.sessionFactory = sessionFactory;
    this.messageSerializer = messageSerializer;
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
            .setQueueName(queueName)
            .setContextURL(contextURL)
            .setUsername(userName)
            .setPassword(password)
            .addSerializer(messageSerializer)
            .build();

    messageHandler.startComponent();
    requestProxy.startComponent();
  }

  @Override
  public void stopComponent() {
    LambdaUtils.tryTo(requestProxy::stopComponent, ex -> logger.error(ex, "Failed to cleanly shutdown request proxy."));
    LambdaUtils.tryTo(messageHandler::stopComponent, ex -> logger.error(ex, "Failed to cleanly shutdown message handler."));
  }
}
