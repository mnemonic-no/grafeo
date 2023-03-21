package no.mnemonic.services.grafeo.service.validators;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static no.mnemonic.services.grafeo.service.validators.ValidatorConfigurationException.Reason.NotFound;

public class DefaultValidatorFactory implements ValidatorFactory {

  private final LoadingCache<String, Validator> cache;

  public DefaultValidatorFactory() {
    cache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Validator>() {
              @Override
              public Validator load(String key) throws Exception {
                return createValidator(key);
              }
            });
  }

  @Override
  public Validator get(String validator, String parameter) {
    if (StringUtils.isBlank(validator)) throw new ValidatorConfigurationException("'validator' is null or empty.", NotFound);

    try {
      return cache.get(createCacheKey(validator, ObjectUtils.ifNull(parameter, "")));
    } catch (ExecutionException | UncheckedExecutionException ex) {
      // ValidatorConfigurationException is an expected exception, thus, let it pass through. Can be handled by caller if necessary.
      if (ex.getCause() instanceof ValidatorConfigurationException) throw (ValidatorConfigurationException) ex.getCause();
      // Otherwise, throw IllegalArgumentException. This shouldn't happen under normal circumstances.
      throw new IllegalArgumentException(ex.getCause().getMessage(), ex.getCause());
    }
  }

  private Validator createValidator(String key) {
    String validator = extractValidatorFromCacheKey(key);
    String parameter = extractParameterFromCacheKey(key);

    if ("TrueValidator".equals(validator)) {
      return new TrueValidator();
    }
    if ("RegexValidator".equals(validator)) {
      return new RegexValidator(parameter);
    }
    if ("NullValidator".equals(validator)) {
      return new NullValidator();
    }

    throw new ValidatorConfigurationException(String.format("Validator '%s' does not exist.", validator), NotFound);
  }

  private String createCacheKey(String validator, String parameter) {
    // In order to have a flat cache keys are of format validator#parameter.
    return validator + "#" + parameter;
  }

  private String extractValidatorFromCacheKey(String key) {
    return key.substring(0, key.indexOf("#"));
  }

  private String extractParameterFromCacheKey(String key) {
    return key.substring(key.indexOf("#") + 1);
  }

}
