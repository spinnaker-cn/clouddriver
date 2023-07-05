package com.netflix.spinnaker.clouddriver.ctyun.provider.agent

import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys
import com.netflix.spinnaker.clouddriver.ctyun.client.CloudVirtualMachineClient
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.MutableCacheData
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.ZONES

@Slf4j
@InheritConstructors
class CtyunZoneCachingAgent extends AbstractCtyunCachingAgent {

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(ZONES.ns)
  ] as Set

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info "start load zone data"

    Map<String, Collection<CacheData>> cacheResults = [:]
    Map<String, Map<String, CacheData>> namespaceCache = [:].withDefault {
      namespace -> [:].withDefault {
        id -> new MutableCacheData(id as String)
      }
    }
    def client = new CloudVirtualMachineClient(
      credentials.credentials.accessKey,
      credentials.credentials.securityKey,
      region
    )
    def myZones = client.getMyZones()
    def zones=client.getZones()

    myZones.each {s->
      Map<String,String> map=new HashMap()
      zones.each {ss->
        if(s.getAzName().equals(ss.name)){
          map.put("id",s.azID)
          map.put("name",s.azName)
          map.put("displayName",ss.azDisplayName)

          def zoneCache = namespaceCache[ZONES.ns]
          def zoneKey = Keys.getZonesKey s.azID, this.accountName,region
          zoneCache[zoneKey].attributes.zone = map
          null
        }
      }

    }



    namespaceCache.each { String namespace, Map<String, CacheData> cacheDataMap ->
      cacheResults[namespace] = cacheDataMap.values()
    }

    CacheResult defaultCacheResult = new DefaultCacheResult(cacheResults)
    log.info 'finish loads zone data.'
    log.info "Caching ${namespaceCache[ZONES.ns].size()} items in $agentType"
    defaultCacheResult
  }
}
