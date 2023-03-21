package no.mnemonic.services.grafeo.auth;

import no.mnemonic.services.common.auth.model.Credentials;

import java.util.function.Supplier;

/**
 * Interface for resolving {@link Credentials} bound to the ThreatIntelligenceService. Implementations must return
 * {@link Credentials} which are bound to the service and not to a particular user (service account). The interface
 * is intended to be used in cases where data needs to be fetched independently from a user's session.
 */
public interface ServiceAccountSPI extends Supplier<Credentials> {
  // Just following the Supplier semantics.
}
