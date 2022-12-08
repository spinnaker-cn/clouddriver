package com.netflix.spinnaker.clouddriver.hecloud.provider.agent


import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.hecloud.cache.Keys
import com.netflix.spinnaker.clouddriver.hecloud.client.HeCloudAutoScalingClient
import com.netflix.spinnaker.clouddriver.hecloud.client.HeCloudElasticCloudServerClient
import com.netflix.spinnaker.clouddriver.hecloud.client.HeCloudLoadBalancerClient
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudInstance
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudInstanceHealth
import com.netflix.spinnaker.clouddriver.hecloud.provider.view.MutableCacheData
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.INFORMATIVE
import static com.netflix.spinnaker.clouddriver.hecloud.cache.Keys.Namespace.*

@Slf4j
@InheritConstructors
class HeCloudInstanceCachingAgent extends AbstractHeCloudCachingAgent {

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(INSTANCES.ns),
    INFORMATIVE.forType(SERVER_GROUPS.ns),
    INFORMATIVE.forType(CLUSTERS.ns)
  ] as Set

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info "start load instance data"

    Map<String, Collection<CacheData>> cacheResults = [:]
    Map<String, Map<String, CacheData>> namespaceCache = [:].withDefault {
      namespace -> [:].withDefault { id -> new MutableCacheData(id as String) }
    }

    HeCloudElasticCloudServerClient ecsClient = new HeCloudElasticCloudServerClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )

    HeCloudLoadBalancerClient elbClient = new HeCloudLoadBalancerClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )

    def memberHealths = elbClient.getAllMembers()
    Map<String, Boolean> memberHealthMap = [:]
    memberHealths.each {
      if (!memberHealthMap.containsKey(it.getAddress())) {
        memberHealthMap.put(it.getAddress(), it.getOperatingStatus().equals("ONLINE"))
      } else {
        Boolean health = it.getOperatingStatus().equals("ONLINE")
        Boolean memberHealth = memberHealthMap.get(it.getAddress())
        memberHealthMap.put(it.getAddress(), health & memberHealth)
      }
    }

    def result = ecsClient.getInstances()
    result.each {
      def tags = ecsClient.getInstanceTags(it.getId())
      def serverGroupName = tags.find {
        it.getKey() == HeCloudAutoScalingClient.defaultServerGroupTagKey
      }?.getValue()

      // security groups
      def sgIds = it.getSecurityGroups().collect {
        it.getId()
      }

      // vpc
      def vpcId = ""
      def privateIps = []
      def publicIps = []
      def vpcSet = it.getAddresses().keySet() as String[]
      if (vpcSet.size() > 0) {
        vpcId = vpcSet[0]
        // address
        def addresses = it.getAddresses()[vpcId]
        def privateIp = addresses.find {
          it.getOsEXTIPSType().toString() == "fixed"
        }?.getAddr()
        if (privateIp) {
          privateIps.add(privateIp)
        }
        def publicIp = addresses.find {
          it.getOsEXTIPSType().toString() == "floating"
        }?.getAddr()
        if (publicIp) {
          publicIps.add(publicIp)
        }
      }

      def launchTime = 0
      if (it.getOsSRVUSGLaunchedAt()) {
        launchTime = HeCloudElasticCloudServerClient.ConvertIsoDateTime it.getOsSRVUSGLaunchedAt() + "Z"
      }

      //status
      boolean elbBound = false
      it.getAddresses().each { k, v ->
        v.each { address ->
          if (memberHealthMap.containsKey(address.getAddr())
          ) {
            if (memberHealthMap.get(address.getAddr())) {
              it.setStatus(HeCloudInstanceHealth.Status.NORMAL.name())
            }
            elbBound = true
          }
        }
      }

      //没有绑定elb的主机使用主机运行状态判定主机状态
      if (!elbBound) {
        if (HeCloudInstanceHealth.Status.ACTIVE.name() == it.getStatus()) {
          it.setStatus(HeCloudInstanceHealth.Status.NORMAL.name())
        }
      }

      def hecloudInstance = new HeCloudInstance(
        account: accountName,
        name: it.getId(),
        instanceName: it.getName(),
        launchTime: launchTime ? launchTime.time : 0,
        //launchedTime: it.getOsSRVUSGLaunchedAt(),
        launchedTime: launchTime,
        zone: it.getOsEXTAZAvailabilityZone(),
        vpcId: vpcId,
        //subnetId: "",
        privateIpAddresses: privateIps,
        publicIpAddresses: publicIps,
        imageId: it.getImage(),
        instanceType: it.getFlavor(),
        securityGroupIds: sgIds,
        instanceHealth: new HeCloudInstanceHealth(instanceStatus: it.getStatus()),
        serverGroupName: serverGroupName
      )

      if (tags) {
        tags.each { tag ->
          hecloudInstance.tags.add(["key": tag.getKey(), "value": tag.getValue()])
        }
      }

      def instances = namespaceCache[INSTANCES.ns]
      def instanceKey = Keys.getInstanceKey it.getId(), this.accountName, this.region

      instances[instanceKey].attributes.instance = hecloudInstance

      def moniker = hecloudInstance.moniker
      if (moniker) {
        def clusterKey = Keys.getClusterKey moniker.cluster, moniker.app, accountName
        def serverGroupKey = Keys.getServerGroupKey hecloudInstance.serverGroupName, accountName, region
        instances[instanceKey].relationships[CLUSTERS.ns].add clusterKey
        instances[instanceKey].relationships[SERVER_GROUPS.ns].add serverGroupKey
      }
      null
    }
    namespaceCache.each { String namespace, Map<String, CacheData> cacheDataMap ->
      cacheResults[namespace] = cacheDataMap.values()
    }

    if (cacheResults[INSTANCES.ns] == null) {
      cacheResults[INSTANCES.ns] = []
    }
    CacheResult defaultCacheResult = new DefaultCacheResult(cacheResults)
    log.info 'finish loads instance data.'
    log.info "Caching ${namespaceCache[INSTANCES.ns].size()} items in $agentType"
    defaultCacheResult
  }

  @Override
  Optional<Map<String, String>> getCacheKeyPatterns() {
    return [
      (INSTANCES.ns): Keys.getInstanceKey('*', accountName, region),
    ]
  }
}
