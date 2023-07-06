package com.netflix.spinnaker.clouddriver.ctyun.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.SubnetProvider
import com.netflix.spinnaker.clouddriver.ctyun.CtyunCloudProvider
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunSubnet
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunSubnetDescription
import com.netflix.spinnaker.clouddriver.ctyun.provider.CtyunInfrastructureProvider
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.SUBNETS



@Slf4j
@Component
class CtyunSubnetProvider implements SubnetProvider<CtyunSubnet> {
  final String cloudProvider = CtyunCloudProvider.ID
  private final Cache cacheView
  final ObjectMapper objectMapper
  private final CtyunInfrastructureProvider ctyunProvider

  @Autowired
  CtyunSubnetProvider(CtyunInfrastructureProvider tCloudProvider, Cache cacheView, ObjectMapper objectMapper) {
    this.ctyunProvider = tCloudProvider
    this.cacheView = cacheView
    this.objectMapper = objectMapper
  }

  @Override
  Set<CtyunSubnet> getAll() {
    getAllMatchingKeyPattern(Keys.getSubnetKey('*', '*', '*'))
  }

  Set<CtyunSubnet> getAllMatchingKeyPattern(String pattern) {
    loadResults(cacheView.filterIdentifiers(SUBNETS.ns, pattern))
  }

  Set<CtyunSubnet> loadResults(Collection<String> identifiers) {
    def data = cacheView.getAll(SUBNETS.ns, identifiers, RelationshipCacheFilter.none())
    def transformed = data.collect(this.&fromCacheData)

    return transformed
  }

  CtyunSubnet fromCacheData(CacheData cacheData) {
    CtyunSubnetDescription subnet = objectMapper.convertValue(cacheData.attributes[SUBNETS.ns], CtyunSubnetDescription)
    def parts = Keys.parse(cacheData.id)

    new CtyunSubnet(
      id: subnet.subnetId,
      name: subnet.subnetName,
      vpcId: subnet.vpcId,
      cidrBlock: subnet.cidrBlock,
      isDefault: subnet.isDefault,
      zone: subnet.zone,
      myAllZones:subnet.myAllZones,
      eips: subnet.eips,
      purpose: "",
      account: parts.account?: "unknown",
      region: parts.region?: "unknown"
    )
  }
}
