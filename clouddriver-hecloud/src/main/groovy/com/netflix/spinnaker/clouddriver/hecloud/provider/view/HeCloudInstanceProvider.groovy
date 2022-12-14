package com.netflix.spinnaker.clouddriver.hecloud.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.InstanceProvider
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudProvider
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudInstance
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudInstanceHealth
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudTargetHealth
import com.netflix.spinnaker.clouddriver.hecloud.model.loadbalance.HeCloudLoadBalancerTargetHealth

import static com.netflix.spinnaker.clouddriver.hecloud.cache.Keys.Namespace.*

import com.netflix.spinnaker.clouddriver.hecloud.cache.Keys
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class HeCloudInstanceProvider implements InstanceProvider<HeCloudInstance, String> {
  final String cloudProvider = HeCloudProvider.ID

  @Autowired
  ObjectMapper objectMapper

  @Autowired
  HeCloudProvider heCloudProvider

  @Autowired
  Cache cacheView

  @Override
  HeCloudInstance getInstance(String account, String region, String id) {
    def key = Keys.getInstanceKey id, account, region

    cacheView.getAll(
      INSTANCES.ns,
      [key],
      RelationshipCacheFilter.include(
        LOAD_BALANCERS.ns,
        SERVER_GROUPS.ns,
        CLUSTERS.ns
      )
    )?.findResult { CacheData cacheData ->
      instanceFromCacheData(account, region, cacheData)
    }
  }

  HeCloudInstance instanceFromCacheData(String account, String region, CacheData cacheData) {
    HeCloudInstance instance = objectMapper.convertValue cacheData.attributes.instance, HeCloudInstance

    def serverGroupName = instance.serverGroupName
    def serverGroupCache = cacheView.get(SERVER_GROUPS.ns, Keys.getServerGroupKey(serverGroupName, account, region))
    def asgInfo = serverGroupCache?.attributes?.asg as Map
    // Scaling instance states
    def asgInstances = asgInfo?.get("instances") as List
    def asgInstance = asgInstances.find {
      it["instance_id"] = instance.name
    }
    // LB targets states
    def lbInfos = asgInfo?.get("forwardLoadBalancerSet") as List
    if (lbInfos) {
      for (lbInfo in lbInfos) {
        def lbId = lbInfo["loadBalancerId"] as String
        def listenerId = lbInfo["listenerId"] as String
        def poolId = lbInfo['poolId'] as String
        def lbHealthKey = Keys.getTargetHealthKey(
          lbId, listenerId, poolId, instance.name, account, region)
        def lbHealthCache = cacheView.get(HEALTH_CHECKS.ns, lbHealthKey)
        def loadBalancerTargetHealth = lbHealthCache?.attributes?.targetHealth as HeCloudLoadBalancerTargetHealth
        if (loadBalancerTargetHealth) {
          def targetHealth = new HeCloudTargetHealth(loadBalancerTargetHealth.healthStatus)
          def healthStatus = targetHealth.targetHealthStatus
          targetHealth.loadBalancers.add(new HeCloudTargetHealth.LBHealthSummary(
            loadBalancerName: lbId,
            state: healthStatus.toServiceStatus()
          ))
          instance.targetHealths.add(targetHealth)
        } else {
          // if server group has lb, but can't get lb health check result for instance in server group
          // assume the target health check result is UNKNOWN
          instance.targetHealths.add(new HeCloudTargetHealth())
        }
      }
    } else if (asgInstance && asgInstance["life_cycle_state"]?.equals("INSERVICE") && asgInstance["health_status"]?.equals("NORMAL")) {
      instance.instanceHealth = new HeCloudInstanceHealth(instanceStatus: "NORMAL")
    }
    instance
  }

  @Override
  String getConsoleOutput(String account, String region, String id) {
    return null
  }
}
