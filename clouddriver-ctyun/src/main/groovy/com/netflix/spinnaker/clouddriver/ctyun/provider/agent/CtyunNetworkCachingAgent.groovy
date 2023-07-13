package com.netflix.spinnaker.clouddriver.ctyun.provider.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.agent.AccountAware
import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.CachingAgent
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.DefaultCacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunVirtualPrivateCloudClient
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunNetworkDescription
import com.netflix.spinnaker.clouddriver.ctyun.provider.CtyunInfrastructureProvider
import com.netflix.spinnaker.clouddriver.ctyun.security.CtyunNamedAccountCredentials
import groovy.util.logging.Slf4j
import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys
import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.NETWORKS

@Slf4j
class CtyunNetworkCachingAgent implements CachingAgent, AccountAware{
  final ObjectMapper objectMapper
  final String region
  final String accountName
  final CtyunNamedAccountCredentials credentials
  final String providerName = CtyunInfrastructureProvider.name

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(NETWORKS.ns)
  ] as Set


  CtyunNetworkCachingAgent(
    CtyunNamedAccountCredentials creds,
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

    List<CacheData> data = networks.collect() { CtyunNetworkDescription network ->
      Map<String, Object> attributes = [(NETWORKS.ns): network]
      new DefaultCacheData(Keys.getNetworkKey(network.vpcId, accountName, region),
        attributes,  [:])
    }

    log.info("Caching ${data.size()} items in ${agentType}")
    new DefaultCacheResult([(NETWORKS.ns): data])
  }

  private Set<CtyunNetworkDescription> loadNetworksAll() {
    CtyunVirtualPrivateCloudClient vpcClient = new CtyunVirtualPrivateCloudClient(
      credentials.credentials.accessKey,
      credentials.credentials.securityKey,
      region
    )

    def networkSet = vpcClient.getNetworksAll()  //vpc

    def networkDescriptionSet =  networkSet.collect {
      def networkDesc = new CtyunNetworkDescription()
      networkDesc.vpcId = it.vpcID
      networkDesc.vpcName = it.name
      networkDesc.cidrBlock = it.CIDR
      networkDesc.isDefault = false
      networkDesc
    }
    return networkDescriptionSet
  }

}
