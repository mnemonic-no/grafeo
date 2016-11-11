package no.mnemonic.act.platform.service.validators;

import java.util.regex.Pattern;

/**
 * The RegexValidator verifies that a value matches a specific regular expression.
 */
class RegexValidator implements Validator {

  private final Pattern regex;

  RegexValidator(String parameter) throws ValidatorConfigurationException {
    try {
      this.regex = Pattern.compile(parameter);
    } catch (Exception e) {
      throw new ValidatorConfigurationException(String.format("RegexValidator with pattern %s could not be created.", parameter));
    }
  }

  @Override
  public boolean validate(String value) {
    return value != null && regex.matcher(value).matches();
  }

}
