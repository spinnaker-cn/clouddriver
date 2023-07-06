package com.netflix.spinnaker.clouddriver.ctyun.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.NetworkProvider
import com.netflix.spinnaker.clouddriver.ctyun.CtyunCloudProvider
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunNetwork
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunNetworkDescription
import com.netflix.spinnaker.clouddriver.ctyun.provider.CtyunInfrastructureProvider
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RestController
import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys
import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.NETWORKS


@Slf4j
@RestController
@Component
class CtyunNetworkProvider implements NetworkProvider<CtyunNetwork> {
  final String cloudProvider = CtyunCloudProvider.ID
  final Cache cacheView
  final ObjectMapper objectMapper
  private final CtyunInfrastructureProvider ctyunProvider

  @Autowired
  CtyunNetworkProvider(CtyunInfrastructureProvider tCloudProvider, Cache cacheView, ObjectMapper objectMapper) {
    this.ctyunProvider = tCloudProvider
    this.cacheView = cacheView
    this.objectMapper = objectMapper
  }

  @Override
  Set<CtyunNetwork> getAll() {
    getAllMatchingKeyPattern(Keys.getNetworkKey('*', '*', '*'))
  }

  Set<CtyunNetwork> getAllMatchingKeyPattern(String pattern) {
    loadResults(cacheView.filterIdentifiers(NETWORKS.ns, pattern))
  }

  Set<CtyunNetwork> loadResults(Collection<String> identifiers) {
    def data = cacheView.getAll(NETWORKS.ns, identifiers, RelationshipCacheFilter.none())
    def transformed = data.collect(this.&fromCacheData)

    return transformed
  }

  CtyunNetwork fromCacheData(CacheData cacheData) {
    CtyunNetworkDescription vnet = objectMapper.convertValue(cacheData.attributes[NETWORKS.ns], CtyunNetworkDescription)
    def parts = Keys.parse(cacheData.id)
    //log.info("CtyunNetworkDescription id = ${cacheData.id}, parts = ${parts}")

    new CtyunNetwork(
      id: vnet.vpcId,
      name: vnet.vpcName,
      cidrBlock: vnet.cidrBlock,
      isDefault: vnet.isDefault,
      account: parts.account?: "none",
      region: parts.region?: "none",
    )
  }
}
