package no.mnemonic.services.grafeo.service.container;

import no.mnemonic.services.common.api.proxy.serializer.Serializer;
import no.mnemonic.services.common.api.proxy.serializer.XStreamSerializer;
import no.mnemonic.services.grafeo.api.service.v1.RequestHeader;
import no.mnemonic.services.grafeo.auth.properties.model.SubjectCredentials;

import jakarta.inject.Provider;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Provides a {@link Serializer} implementation based on XStream intended to be used by {@link GrafeoServiceProxyServer}.
 */
public class XStreamMessageSerializerProvider implements Provider<Serializer> {

  @Override
  public Serializer get() {
    XStreamSerializer.XStreamSerializerBuilder builder = XStreamSerializer.builder()
            // Common Java classes used in requests (required for collections holding those).
            .setAllowedClass(String.class)
            .setAllowedClass(UUID.class)
            // RequestHeader and SubjectCredentials are part of every service request.
            .setAllowedClass(RequestHeader.class)
            .setAllowedClass(SubjectCredentials.class)
            // Allow all request classes defined in the API.
            .setAllowedClassesRegex("no.mnemonic.services.grafeo.api.request.*");

    // Add additional classes to white-list as specified by subclasses.
    additionalAllowedClasses().forEach(builder::setAllowedClass);
    additionalAllowedClassesRegex().forEach(builder::setAllowedClassesRegex);

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
