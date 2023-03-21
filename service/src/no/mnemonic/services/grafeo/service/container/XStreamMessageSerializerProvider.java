package no.mnemonic.services.grafeo.service.container;

import no.mnemonic.messaging.requestsink.jms.serializer.MessageSerializer;
import no.mnemonic.messaging.requestsink.jms.serializer.XStreamMessageSerializer;
import no.mnemonic.services.common.messagebus.ServiceRequestMessage;
import no.mnemonic.services.grafeo.api.service.v1.RequestHeader;
import no.mnemonic.services.grafeo.auth.properties.model.SubjectCredentials;

import javax.inject.Provider;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Provides a {@link MessageSerializer} implementation based on XStream intended to be used by {@link SmbServer}.
 */
public class XStreamMessageSerializerProvider implements Provider<MessageSerializer> {

  @Override
  public MessageSerializer get() {
    XStreamMessageSerializer.Builder builder = XStreamMessageSerializer.builder()
            // Common Java classes used in requests (required for collections holding those).
            .addAllowedClass(String.class)
            .addAllowedClass(UUID.class)
            // Request message used by SMB.
            .addAllowedClass(ServiceRequestMessage.class)
            // RequestHeader and SubjectCredentials are part of every service request.
            .addAllowedClass(RequestHeader.class)
            .addAllowedClass(SubjectCredentials.class)
            // Allow all request classes defined in the API.
            .addAllowedClass("no.mnemonic.services.grafeo.api.request.*");

    // Add additional classes to white-list as specified by subclasses.
    additionalAllowedClasses().forEach(builder::addAllowedClass);
    additionalAllowedClassesRegex().forEach(builder::addAllowedClass);

    return builder.build();
  }

  /**
   * Override this method in order to allow additional classes in the XStream white-list.
   *
   * @return Additional allowed classes (empty by default)
   */
  protected Set<Class<?>> additionalAllowedClasses() {
    return Collections.emptySet();
  }

  /**
   * Override this method in order to allow additional classes in the XStream white-list (as regular expressions).
   *
   * @return Additional allowed classes (empty by default)
   */
  protected Set<String> additionalAllowedClassesRegex() {
    return Collections.emptySet();
  }
}
