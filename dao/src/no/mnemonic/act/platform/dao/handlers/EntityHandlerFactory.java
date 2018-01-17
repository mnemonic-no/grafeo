package no.mnemonic.act.platform.dao.handlers;

/**
 * A factory which creates EntityHandlers.
 */
public interface EntityHandlerFactory {

  /**
   * Creates an EntityHandler based on it's name and a parameter string.
   *
   * @param handler   Name of EntityHandler.
   * @param parameter Parameter string which is EntityHandler specific.
   * @return An EntityHandler instance.
   */
  EntityHandler get(String handler, String parameter);

}
