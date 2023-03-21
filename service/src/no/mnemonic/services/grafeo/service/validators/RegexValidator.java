package no.mnemonic.services.grafeo.service.validators;

import no.mnemonic.commons.utilities.StringUtils;

import java.util.regex.Pattern;

import static no.mnemonic.services.grafeo.service.validators.ValidatorConfigurationException.Reason.Misconfigured;

/**
 * The RegexValidator verifies that a value matches a specific regular expression.
 */
class RegexValidator implements Validator {

  private final Pattern regex;

  RegexValidator(String parameter) {
    if (StringUtils.isBlank(parameter)) throw new ValidatorConfigurationException("RegexValidator requires a configuration parameter.", Misconfigured);

    try {
      this.regex = Pattern.compile(parameter);
    } catch (Exception e) {
      throw new ValidatorConfigurationException(String.format("RegexValidator with pattern %s could not be created.", parameter), Misconfigured);
    }
  }

  @Override
  public boolean validate(String value) {
    return value != null && regex.matcher(value).matches();
  }

}
