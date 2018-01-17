package no.mnemonic.act.platform.dao.handlers;

/**
 * An EntityHandler defines how to handle the value of an Object or Fact.
 * <p>
 * For example, a value might be stored differently in the database than it is presented to the user.
 * An EntityHandler provides a mechanism to customize this behaviour.
 */
public interface EntityHandler {

  /**
   * Defines how a value is stored in the database.
   * <p>
   * For example, www.example.org might be stored as org.example.www.
   *
   * @param value A user-supplied value.
   * @return The encoded value which is stored in the database.
   */
  String encode(String value);

  /**
   * Reverts the 'encode' operation in order to retrieve the original value representation.
   * <p>
   * The following equation must hold: decode(encode(value)) == value
   *
   * @param value Value as stored in the database.
   * @return The original value representation as presented to the user.
   */
  String decode(String value);

}
