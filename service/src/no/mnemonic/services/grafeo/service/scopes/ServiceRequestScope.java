package no.mnemonic.services.grafeo.service.scopes;

import com.google.inject.ScopeAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Use service request scope to inject objects which should live for the duration of one request to a back-end service.
 * Objects injected in this scope will not be shared across multiple requests from one user or multiple users. Use this
 * scope to only cache information for the duration of one request.
 */
@Target({TYPE, METHOD})
@Retention(RUNTIME)
@ScopeAnnotation
public @interface ServiceRequestScope {
}
