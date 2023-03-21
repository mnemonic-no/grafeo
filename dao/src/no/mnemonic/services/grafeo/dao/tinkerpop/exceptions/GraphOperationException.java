package no.mnemonic.services.grafeo.dao.tinkerpop.exceptions;

/**
 * A GraphOperationException is thrown when an operation on the graph could not be executed.
 *
 * @deprecated Will be replaced by the TinkerPop implementation in the service module.
 */
@Deprecated
public class GraphOperationException extends RuntimeException {

  private static final long serialVersionUID = -799287264768678971L;

  public GraphOperationException(String message) {
    super(message);
  }

}
