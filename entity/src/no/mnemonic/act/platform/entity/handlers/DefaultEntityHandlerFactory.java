package no.mnemonic.act.platform.entity.handlers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class DefaultEntityHandlerFactory implements EntityHandlerFactory {

  private final LoadingCache<String, EntityHandler> cache;

  public DefaultEntityHandlerFactory() {
    cache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, EntityHandler>() {
              @Override
              public EntityHandler load(String key) throws Exception {
                return createEntityHandler(key);
              }
            });
  }

  @Override
  public EntityHandler get(String handler, String parameter) {
    if (StringUtils.isBlank(handler)) throw new IllegalArgumentException("'handler' is null or empty.");

    try {
      return cache.get(createCacheKey(handler, ObjectUtils.ifNull(parameter, "")));
    } catch (ExecutionException e) {
      throw new IllegalArgumentException(e.getCause());
    }
  }

  private EntityHandler createEntityHandler(String key) throws Exception {
    String handler = extractHandlerFromCacheKey(key);
    // 'parameter' is currently not used because it's not needed by the IdentityHandler, but other handlers might need it.
    // noinspection unused
    String parameter = extractParameterFromCacheKey(key);

    if ("IdentityHandler".equals(handler)) {
      return new IdentityHandler();
    }

    throw new Exception(String.format("Handler %s does not exist.", handler));
  }

  private String createCacheKey(String handler, String parameter) {
    // In order to have a flat cache keys are of format handler#parameter.
    return handler + "#" + parameter;
  }

  private String extractHandlerFromCacheKey(String key) {
    return key.substring(0, key.indexOf("#"));
  }

  private String extractParameterFromCacheKey(String key) {
    return key.substring(key.indexOf("#") + 1);
  }

}
