package com.netflix.spinnaker.clouddriver.ecloud.provider.view;

import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.SUBNETS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudSubnet;
import com.netflix.spinnaker.clouddriver.model.SubnetProvider;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-09
 */
@Component
public class EcloudSubnetProvider implements SubnetProvider<EcloudSubnet> {
  ObjectMapper objectMapper;

  Cache cacheView;

  public EcloudSubnetProvider(Cache cacheView, ObjectMapper objectMapper) {
    this.cacheView = cacheView;
    this.objectMapper = objectMapper;
  }

  @Override
  public String getCloudProvider() {
    return EcloudProvider.ID;
  }

  @Override
  public Set<EcloudSubnet> getAll() {
    Set<EcloudSubnet> subNets =
        cacheView.getAll(SUBNETS.ns, RelationshipCacheFilter.none()).stream()
            .map(this::fromCacheData)
            .filter(subnet -> subnet.getName() != null && subnet.getName().length() > 0)
            .collect(Collectors.toSet());
    return subNets;
  }

  private EcloudSubnet fromCacheData(CacheData cacheData) {
    EcloudSubnet subnet =
        this.objectMapper.convertValue(cacheData.getAttributes(), EcloudSubnet.class);
    return subnet;
  }
}
