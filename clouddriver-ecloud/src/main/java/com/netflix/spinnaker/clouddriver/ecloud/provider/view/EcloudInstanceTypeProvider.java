package com.netflix.spinnaker.clouddriver.ecloud.provider.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudInstanceType;
import com.netflix.spinnaker.clouddriver.model.InstanceTypeProvider;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Description: 从缓存查询移动云实例规格并提供给查询接口。
 * @Author: han.pengfei-ai
 * @Date: 2024/06/27
 */
@Component
@Slf4j
public class EcloudInstanceTypeProvider implements InstanceTypeProvider<EcloudInstanceType> {

  private final ObjectMapper objectMapper;
  private final Cache cacheView;
  private final EcloudProvider provider;

  @Autowired
  public EcloudInstanceTypeProvider(
      ObjectMapper objectMapper, Cache cacheView, EcloudProvider provider) {
    this.objectMapper = objectMapper;
    this.cacheView = cacheView;
    this.provider = provider;
  }

  @Override
  public Set<EcloudInstanceType> getAll() {
    log.info("[ECLOUD-SPEC-DIAG] query start");
    Set<EcloudInstanceType> results = new HashSet<>();
    String globalKey = Keys.getInstanceTypeKey("*", "*", "*", "*");
    Collection<String> allInstanceTypeKeys =
        cacheView.filterIdentifiers(Keys.Namespace.INSTANCE_TYPES.ns, globalKey);
    Collection<CacheData> allData =
        cacheView.getAll(
            Keys.Namespace.INSTANCE_TYPES.ns, allInstanceTypeKeys, RelationshipCacheFilter.none());
    for (CacheData allDatum : allData) {
      Map<String, Object> attributes = allDatum.getAttributes();
      EcloudInstanceType type = objectMapper.convertValue(attributes, EcloudInstanceType.class);
      results.add(type);
    }
    if (log.isInfoEnabled()) {
      Map<String, Integer> countsByZone = new HashMap<>();
      for (EcloudInstanceType type : results) {
        if (type == null) {
          countsByZone.merge("<null>", 1, Integer::sum);
          continue;
        }
        String zoneKey =
            new StringBuffer()
                .append(type.getAccount())
                .append('/')
                .append(type.getRegion())
                .append('/')
                .append(type.getZone())
                .toString();
        countsByZone.merge(zoneKey, 1, Integer::sum);
      }
      log.info(
          "[ECLOUD-SPEC-DIAG] query complete, resultCount={}, countsByAccountRegionZone={}",
          results.size(),
          countsByZone);
    }
    return results;
  }
}
