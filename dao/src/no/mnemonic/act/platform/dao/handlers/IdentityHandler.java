package no.mnemonic.act.platform.dao.handlers;

/**
 * The IdentityHandler does not transform a value but stores it as provided in the database, i.e. encode(value) == value.
 */
class IdentityHandler implements EntityHandler {

  @Override
  public String encode(String value) {
    return value;
  }

  @Override
  public String decode(String value) {
    return value;
  }

}
