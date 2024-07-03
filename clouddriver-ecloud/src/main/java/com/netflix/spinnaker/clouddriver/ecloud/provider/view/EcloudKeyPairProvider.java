package com.netflix.spinnaker.clouddriver.ecloud.provider.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudKeyPair;
import com.netflix.spinnaker.clouddriver.model.KeyPairProvider;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author xu.dangling
 * @date 2024/5/16 @Description
 */
@Component
public class EcloudKeyPairProvider implements KeyPairProvider<EcloudKeyPair> {

  private final ObjectMapper objectMapper;

  private final Cache cacheView;

  private final EcloudProvider provider;

  @Autowired
  public EcloudKeyPairProvider(
      ObjectMapper objectMapper, Cache cacheView, EcloudProvider provider) {
    this.objectMapper = objectMapper;
    this.cacheView = cacheView;
    this.provider = provider;
  }

  @Override
  public Set<EcloudKeyPair> getAll() {
    Collection<String> identifiers =
        cacheView.filterIdentifiers(Keys.Namespace.KEY_PAIRS.ns, Keys.getKeyPairKey("*", "*", "*"));

    Collection<CacheData> cacheDataList =
        cacheView.getAll(Keys.Namespace.KEY_PAIRS.ns, identifiers, RelationshipCacheFilter.none());

    Set<EcloudKeyPair> set = new HashSet<>();
    for (CacheData cacheData : cacheDataList) {
      EcloudKeyPair keyPair = new EcloudKeyPair();
      keyPair.setKeyId((String) cacheData.getAttributes().get("id"));
      keyPair.setKeyName((String) cacheData.getAttributes().get("name"));
      keyPair.setRegion((String) cacheData.getAttributes().get("poolId"));
      keyPair.setAccount((String) cacheData.getAttributes().get("account"));
      keyPair.setKeyFingerprint((String) cacheData.getAttributes().get("fingerPrint"));
      set.add(keyPair);
    }
    return set;
  }
}
