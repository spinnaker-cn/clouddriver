package com.netflix.spinnaker.clouddriver.ctyun.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunServerGroup
import com.netflix.spinnaker.clouddriver.model.InstanceProvider
import com.netflix.spinnaker.clouddriver.ctyun.CtyunCloudProvider
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunInstance
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunTargetHealth
import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancerTargetHealth

import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.*

import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CtyunInstanceProvider implements InstanceProvider<CtyunInstance, String> {
  final String cloudProvider = CtyunCloudProvider.ID

  @Autowired
  ObjectMapper objectMapper

  @Autowired
  CtyunCloudProvider ctyunCloudProvider

  @Autowired
  Cache cacheView

  @Override
  CtyunInstance getInstance(String account, String region, String id) {
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

  CtyunInstance instanceFromCacheData(String account, String region, CacheData cacheData) {
    CtyunInstance instance = objectMapper.convertValue cacheData.attributes.instance, CtyunInstance

    def serverGroupName = instance.serverGroupName
    def serverGroupCache = cacheView.get(SERVER_GROUPS.ns, Keys.getServerGroupKey(serverGroupName, account, region))
    def serverGroup = new CtyunServerGroup(serverGroupCache?.attributes)
    def asgInfo = serverGroupCache?.attributes?.asg as Map
    //本参数表示健康检查方式。取值范围：1： 云服务器健康检查。 2： 弹性负载均衡健康检查。根据以上XXX取值范围XXX。
    if(asgInfo.healthMode==2){
      def lbInfos = serverGroup?.loadBlanders
      if (lbInfos) {
        for (lbInfo in lbInfos) {
          def lbId = lbInfo["lbID"] as String
          String loadBalancerKey = Keys.getLoadBalancerKey lbId, account, region
          CacheData loadBalancersData = cacheView.get LOAD_BALANCERS.ns, loadBalancerKey
          def listenerId = ''
          def locationId = ''
          Map<String,Object> map=loadBalancersData.attributes.listeners.find {ss->
            ss.targetGroupId==lbInfo.hostGroupID
          }
          if(map!=null&&map.get("listenerId")!=null&&String.valueOf(map.get("listenerId")).size()>0){
            listenerId=map.get("listenerId")
            locationId=lbInfo.hostGroupID
          }else {
            //查转发规则
            Collection<Map> listeners = loadBalancersData.attributes.listeners.findAll { liss->
              liss.rules.size()>0
            }
            Map<String,Object> listenermap = listeners.find {li->
              def ru = li.rules.find{rule->
                rule.ruleTargetGroupId == it.hostGroupID
              }
              if(ru!=null&&ru.get("locationId")!=null && String.valueOf(ru.get("locationId")).size()>0){
                listenerId=li.listenerId
                locationId=ru.get("locationId")
                return true
              }
            }
          }

          def lbHealthKey = Keys.getTargetHealthKey(
            lbId, listenerId, locationId, instance.name, account, region)
          def lbHealthCache = cacheView.get(HEALTH_CHECKS.ns, lbHealthKey)
          def loadBalancerTargetHealth = lbHealthCache?.attributes?.targetHealth as CtyunLoadBalancerTargetHealth
          if (loadBalancerTargetHealth) {
            def targetHealth = new CtyunTargetHealth(loadBalancerTargetHealth.healthStatus)
            def healthStatus = targetHealth.targetHealthStatus
            targetHealth.loadBalancers.add(new CtyunTargetHealth.LBHealthSummary(
              loadBalancerName: lbId,
              state: healthStatus.toServiceStatus()
            ))
            instance.targetHealths.add(targetHealth)
          } else {
            // if server group has lb, but can't get lb health check result for instance in server group
            // assume the target health check result is UNKNOWN
            instance.targetHealths.add(new CtyunTargetHealth())
          }
        }
      }
    }

    instance
  }

  @Override
  String getConsoleOutput(String account, String region, String id) {
    return null
  }
}
