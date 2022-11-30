package com.netflix.spinnaker.clouddriver.hecloud.provider.agent


import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.hecloud.cache.Keys
import com.netflix.spinnaker.clouddriver.hecloud.client.HeCloudElasticCloudServerClient
import com.netflix.spinnaker.clouddriver.hecloud.enums.InstanceCondOperationStatusEnum
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudInstanceType
import com.netflix.spinnaker.clouddriver.hecloud.provider.view.MutableCacheData
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.hecloud.cache.Keys.Namespace.INSTANCE_TYPES

@Slf4j
@InheritConstructors
class HeCloudInstanceTypeCachingAgent extends AbstractHeCloudCachingAgent {
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

    HeCloudElasticCloudServerClient ecsClient = new HeCloudElasticCloudServerClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region,
    )

    def result = ecsClient.getInstanceTypes()
    result.each {

      def extraSpec = it.getOsExtraSpecs()
      def flavorStatus = extraSpec.getCondOperationStatus();

      //不返回售罄的规格
      // Remove abandon filter: InstanceCondOperationStatusEnum.ABANDON
      if(
        InstanceCondOperationStatusEnum.SELLOUT.name().toLowerCase() == flavorStatus ||
          InstanceCondOperationStatusEnum.OBT_SELLOUT.name().toLowerCase() == flavorStatus
      ){
        null
      }
      else {
        def instanceFamily = extraSpec.getEcsPerformancetype()
        def hecloudInstanceType = new HeCloudInstanceType(
          name: it.getName(),
          account: this.accountName,
          region: this.region,
          instanceFamily: instanceFamily,
          cpu: it.getVcpus() as int,
          mem: it.getRam()/1024
        )

        def instanceTypes = namespaceCache[INSTANCE_TYPES.ns]
        def instanceTypeKey = Keys.getInstanceTypeKey this.accountName, this.region, hecloudInstanceType.name

        instanceTypes[instanceTypeKey].attributes.instanceType = hecloudInstanceType
        null
      }

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
