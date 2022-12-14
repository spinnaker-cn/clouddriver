package com.netflix.spinnaker.clouddriver.hecloud.provider.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.agent.*
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.DefaultCacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.hecloud.cache.Keys
import com.netflix.spinnaker.clouddriver.hecloud.client.HeCloudVirtualPrivateCloudClient
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudNetworkDescription
import com.netflix.spinnaker.clouddriver.hecloud.provider.HeCloudInfrastructureProvider
import com.netflix.spinnaker.clouddriver.hecloud.security.HeCloudNamedAccountCredentials
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.hecloud.cache.Keys.Namespace.NETWORKS

@Slf4j
class HeCloudNetworkCachingAgent implements CachingAgent, AccountAware{
  final ObjectMapper objectMapper
  final String region
  final String accountName
  final HeCloudNamedAccountCredentials credentials
  final String providerName = HeCloudInfrastructureProvider.name

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(NETWORKS.ns)
  ] as Set


  HeCloudNetworkCachingAgent(
    HeCloudNamedAccountCredentials creds,
    ObjectMapper objectMapper,
    String region
  ) {
    this.accountName = creds.name
    this.credentials = creds
    this.objectMapper = objectMapper
    this.region = region
  }

  @Override
  String getAgentType() {
    return "$accountName/$region/${this.class.simpleName}"
  }

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info("Describing items in ${agentType}")

    def networks = loadNetworksAll()

    List<CacheData> data = networks.collect() { HeCloudNetworkDescription network ->
      Map<String, Object> attributes = [(NETWORKS.ns): network]
      new DefaultCacheData(Keys.getNetworkKey(network.vpcId, accountName, region),
        attributes,  [:])
    }

    log.info("Caching ${data.size()} items in ${agentType}")
    new DefaultCacheResult([(NETWORKS.ns): data])
  }

  private Set<HeCloudNetworkDescription> loadNetworksAll() {
    HeCloudVirtualPrivateCloudClient vpcClient = new HeCloudVirtualPrivateCloudClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region,
      accountName
    )

    def networkSet = vpcClient.getNetworksAll()  //vpc

    def networkDescriptionSet =  networkSet.collect {
      def networkDesc = new HeCloudNetworkDescription()
      networkDesc.vpcId = it.getId()
      networkDesc.vpcName = it.getName()
      networkDesc.cidrBlock = it.getCidr()
      networkDesc
    }
    return networkDescriptionSet
  }

  @Override
  Optional<Map<String, String>> getCacheKeyPatterns() {
    return [
      (NETWORKS.ns): Keys.getNetworkKey("*", accountName, region),
    ]
  }
}
