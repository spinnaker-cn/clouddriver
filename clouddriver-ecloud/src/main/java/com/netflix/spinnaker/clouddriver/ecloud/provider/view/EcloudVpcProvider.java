package com.netflix.spinnaker.clouddriver.ecloud.provider.view;

import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.NETWORKS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudVpc;
import com.netflix.spinnaker.clouddriver.model.NetworkProvider;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Component
public class EcloudVpcProvider implements NetworkProvider<EcloudVpc> {

  private ObjectMapper objectMapper;

  private Cache cacheView;

  private EcloudProvider provider;

  @Autowired
  public EcloudVpcProvider(ObjectMapper objectMapper, Cache cacheView, EcloudProvider provider) {
    this.objectMapper = objectMapper;
    this.cacheView = cacheView;
    this.provider = provider;
  }

  @Override
  public String getCloudProvider() {
    return EcloudProvider.ID;
  }

  @Override
  public Set<EcloudVpc> getAll() {
    return getAllMatchingKeyPattern(Keys.getNetworkKey("*", "*", "*"));
  }

  private Set<EcloudVpc> getAllMatchingKeyPattern(String pattern) {
    return loadResults(cacheView.filterIdentifiers(NETWORKS.ns, pattern));
  }

  private Set<EcloudVpc> loadResults(Collection<String> identifiers) {
    Collection<CacheData> data =
        cacheView.getAll(NETWORKS.ns, identifiers, RelationshipCacheFilter.none());
    Set<EcloudVpc> transformed = data.stream().map(this::fromCacheData).collect(Collectors.toSet());
    return transformed;
  }

  private EcloudVpc fromCacheData(CacheData cacheData) {
    EcloudVpc vpc = this.objectMapper.convertValue(cacheData.getAttributes(), EcloudVpc.class);
    return vpc;
  }
}
