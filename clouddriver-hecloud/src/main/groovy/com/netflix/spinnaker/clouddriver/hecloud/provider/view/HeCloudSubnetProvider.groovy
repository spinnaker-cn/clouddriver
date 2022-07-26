package com.netflix.spinnaker.clouddriver.hecloud.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.SubnetProvider
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudProvider
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudSubnet
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudSubnetDescription
import com.netflix.spinnaker.clouddriver.hecloud.provider.HeCloudInfrastructureProvider
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import com.netflix.spinnaker.clouddriver.hecloud.cache.Keys
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.hecloud.cache.Keys.Namespace.SUBNETS



@Slf4j
@Component
class HeCloudSubnetProvider implements SubnetProvider<HeCloudSubnet> {
  final String cloudProvider = HeCloudProvider.ID
  private final Cache cacheView
  final ObjectMapper objectMapper
  private final HeCloudInfrastructureProvider hecloudProvider

  @Autowired
  HeCloudSubnetProvider(HeCloudInfrastructureProvider tCloudProvider, Cache cacheView, ObjectMapper objectMapper) {
    this.hecloudProvider = tCloudProvider
    this.cacheView = cacheView
    this.objectMapper = objectMapper
  }

  @Override
  Set<HeCloudSubnet> getAll() {
    getAllMatchingKeyPattern(Keys.getSubnetKey('*', '*', '*'))
  }

  Set<HeCloudSubnet> getAllMatchingKeyPattern(String pattern) {
    loadResults(cacheView.filterIdentifiers(SUBNETS.ns, pattern))
  }

  Set<HeCloudSubnet> loadResults(Collection<String> identifiers) {
    def data = cacheView.getAll(SUBNETS.ns, identifiers, RelationshipCacheFilter.none())
    def transformed = data.collect(this.&fromCacheData)

    return transformed
  }

  HeCloudSubnet fromCacheData(CacheData cacheData) {
    HeCloudSubnetDescription subnet = objectMapper.convertValue(cacheData.attributes[SUBNETS.ns], HeCloudSubnetDescription)
    def parts = Keys.parse(cacheData.id)

    new HeCloudSubnet(
      id: subnet.networkId,
      name: subnet.subnetName,
      vpcId: subnet.vpcId,
      cidrBlock: subnet.cidrBlock,
      purpose: subnet.subnetId,
      account: parts.account?: "unknown",
      region: parts.region?: "unknown"
    )
  }
}
