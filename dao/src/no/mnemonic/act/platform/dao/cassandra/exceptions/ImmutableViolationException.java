package no.mnemonic.act.platform.dao.cassandra.exceptions;

public class ImmutableViolationException extends RuntimeException {

  public ImmutableViolationException(String message) {
    super(message);
  }

}
