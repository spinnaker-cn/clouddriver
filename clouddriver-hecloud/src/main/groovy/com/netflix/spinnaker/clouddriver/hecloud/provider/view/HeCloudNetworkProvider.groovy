package com.netflix.spinnaker.clouddriver.hecloud.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.NetworkProvider
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudProvider
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudNetwork
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudNetworkDescription
import com.netflix.spinnaker.clouddriver.hecloud.provider.HeCloudInfrastructureProvider
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RestController
import com.netflix.spinnaker.clouddriver.hecloud.cache.Keys
import static com.netflix.spinnaker.clouddriver.hecloud.cache.Keys.Namespace.NETWORKS


@Slf4j
@RestController
@Component
class HeCloudNetworkProvider implements NetworkProvider<HeCloudNetwork> {
  final String cloudProvider = HeCloudProvider.ID
  final Cache cacheView
  final ObjectMapper objectMapper
  private final HeCloudInfrastructureProvider hecloudProvider

  @Autowired
  HeCloudNetworkProvider(HeCloudInfrastructureProvider tCloudProvider, Cache cacheView, ObjectMapper objectMapper) {
    this.hecloudProvider = tCloudProvider
    this.cacheView = cacheView
    this.objectMapper = objectMapper
  }

  @Override
  Set<HeCloudNetwork> getAll() {
    getAllMatchingKeyPattern(Keys.getNetworkKey('*', '*', '*'))
  }

  Set<HeCloudNetwork> getAllMatchingKeyPattern(String pattern) {
    loadResults(cacheView.filterIdentifiers(NETWORKS.ns, pattern))
  }

  Set<HeCloudNetwork> loadResults(Collection<String> identifiers) {
    def data = cacheView.getAll(NETWORKS.ns, identifiers, RelationshipCacheFilter.none())
    def transformed = data.collect(this.&fromCacheData)

    return transformed
  }

  HeCloudNetwork fromCacheData(CacheData cacheData) {
    HeCloudNetworkDescription vnet = objectMapper.convertValue(cacheData.attributes[NETWORKS.ns], HeCloudNetworkDescription)
    def parts = Keys.parse(cacheData.id)
    //log.info("HeCloudNetworkDescription id = ${cacheData.id}, parts = ${parts}")

    new HeCloudNetwork(
      id: vnet.vpcId,
      name: vnet.vpcName,
      cidrBlock: vnet.cidrBlock,
      account: parts.account?: "none",
      region: parts.region?: "none",
    )
  }
}
