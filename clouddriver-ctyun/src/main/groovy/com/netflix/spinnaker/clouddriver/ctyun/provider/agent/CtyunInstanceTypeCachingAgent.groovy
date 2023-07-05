package com.netflix.spinnaker.clouddriver.ctyun.provider.agent


import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys
import com.netflix.spinnaker.clouddriver.ctyun.client.CloudVirtualMachineClient
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunInstanceType
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.MutableCacheData
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.INSTANCE_TYPES

@Slf4j
@InheritConstructors
class CtyunInstanceTypeCachingAgent extends AbstractCtyunCachingAgent {
  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(INSTANCE_TYPES.ns)
  ] as Set

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info "start load instance types data"

    Map<String, Collection<CacheData>> cacheResults = [:]
    Map<String, Map<String, CacheData>> namespaceCache = [:].withDefault {
      namespace -> [:].withDefault { id -> new MutableCacheData(id as String) }
    }

    CloudVirtualMachineClient cvmClient = new CloudVirtualMachineClient(
      credentials.credentials.accessKey,
      credentials.credentials.securityKey,
      region,
    )

    def result = cvmClient.getInstanceTypes()
    result.each {
      def ctyunInstanceType = new CtyunInstanceType(
        name: it.flavorName,
        account: this.accountName,
        region: this.region,
        zone: "",
        instanceFamily: it.flavorName.split("\\.")[0],
        cpu: it.flavorCPU,
        mem: it.flavorRAM
      )

      def instanceTypes = namespaceCache[INSTANCE_TYPES.ns]
      def instanceTypeKey = Keys.getInstanceTypeKey this.accountName, this.region, ctyunInstanceType.name

      instanceTypes[instanceTypeKey].attributes.instanceType = ctyunInstanceType
      null
    }

    namespaceCache.each { String namespace, Map<String, CacheData> cacheDataMap ->
      cacheResults[namespace] = cacheDataMap.values()
    }

    CacheResult defaultCacheResult = new DefaultCacheResult(cacheResults)
    log.info "finish loads instance type data."
    log.info "Caching ${namespaceCache[INSTANCE_TYPES.ns].size()} items in $agentType"
    defaultCacheResult
  }
}
