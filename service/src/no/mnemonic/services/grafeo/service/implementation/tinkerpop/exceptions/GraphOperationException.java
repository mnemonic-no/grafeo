package no.mnemonic.services.grafeo.service.implementation.tinkerpop.exceptions;

/**
 * A GraphOperationException is thrown when an operation on the graph could not be executed.
 */
public class GraphOperationException extends RuntimeException {

  private static final long serialVersionUID = -799287264768678971L;

  public GraphOperationException(String message) {
    super(message);
  }

}
