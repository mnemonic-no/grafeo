package no.mnemonic.act.platform.dao.cassandra.exceptions;

public class ImmutableViolationException extends Exception {

  public ImmutableViolationException(String message) {
    super(message);
  }

}
