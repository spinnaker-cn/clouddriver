package com.netflix.spinnaker.clouddriver.ctyun.provider.agent


import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys
import com.netflix.spinnaker.clouddriver.ctyun.client.CloudVirtualMachineClient
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunKeyPair
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.MutableCacheData
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.KEY_PAIRS

@Slf4j
@InheritConstructors
class CtyunKeyPairCachingAgent extends AbstractCtyunCachingAgent {

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(KEY_PAIRS.ns)
  ] as Set

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info "start load key pair data"

    Map<String, Collection<CacheData>> cacheResults = [:]
    Map<String, Map<String, CacheData>> namespaceCache = [:].withDefault {
      namespace -> [:].withDefault {
        id -> new MutableCacheData(id as String)
      }
    }

    CloudVirtualMachineClient cvmClient = new CloudVirtualMachineClient(
      credentials.credentials.accessKey,
      credentials.credentials.securityKey,
      region)

    def result = cvmClient.getKeyPairs()
    result.each {
      def ctyunKeyPair = new CtyunKeyPair(
        keyId: it.keyPairID,
        keyName: it.keyPairName,
        keyFingerprint: "", // DescribeImages does not return finger print
        region: this.region,
        account: this.accountName
      )

      def keyPairs = namespaceCache[KEY_PAIRS.ns]
      def keyPairKey = Keys.getKeyPairKey ctyunKeyPair.keyName, this.accountName, this.region
      keyPairs[keyPairKey].attributes.keyPair = ctyunKeyPair
      null
    }

    namespaceCache.each { String namespace, Map<String, CacheData> cacheDataMap ->
      cacheResults[namespace] = cacheDataMap.values()
    }

    CacheResult defaultCacheResult = new DefaultCacheResult(cacheResults)
    log.info 'finish loads key pair data.'
    log.info "Caching ${namespaceCache[KEY_PAIRS.ns].size()} items in $agentType"
    defaultCacheResult
  }
}
