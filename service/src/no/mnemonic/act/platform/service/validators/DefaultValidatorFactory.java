package no.mnemonic.act.platform.service.validators;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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
    if (StringUtils.isBlank(validator)) throw new IllegalArgumentException("'validator' is null or empty.");

    try {
      return cache.get(createCacheKey(validator, ObjectUtils.ifNull(parameter, "")));
    } catch (ExecutionException ex) {
      throw new IllegalArgumentException(ex.getCause().getMessage(), ex.getCause());
    }
  }

  private Validator createValidator(String key) throws Exception {
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

    throw new Exception(String.format("Validator '%s' does not exist.", validator));
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
