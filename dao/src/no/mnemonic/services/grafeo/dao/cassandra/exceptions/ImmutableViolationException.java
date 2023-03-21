package no.mnemonic.services.grafeo.dao.cassandra.exceptions;

public class ImmutableViolationException extends RuntimeException {

  public ImmutableViolationException(String message) {
    super(message);
  }

}
