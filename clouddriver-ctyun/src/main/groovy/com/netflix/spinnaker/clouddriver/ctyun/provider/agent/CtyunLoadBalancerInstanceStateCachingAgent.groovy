package com.netflix.spinnaker.clouddriver.ctyun.provider.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.agent.*
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.DefaultCacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.core.provider.agent.HealthProvidingCachingAgent
import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys
import com.netflix.spinnaker.clouddriver.ctyun.client.LoadBalancerClient
import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancer
import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancerTargetHealth
import com.netflix.spinnaker.clouddriver.ctyun.provider.CtyunInfrastructureProvider
import com.netflix.spinnaker.clouddriver.ctyun.security.CtyunNamedAccountCredentials
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.HEALTH_CHECKS

@Slf4j
class CtyunLoadBalancerInstanceStateCachingAgent implements CachingAgent, HealthProvidingCachingAgent, AccountAware{
  final String providerName = CtyunInfrastructureProvider.name
  CtyunNamedAccountCredentials credentials
  final String accountName
  final String region
  final ObjectMapper objectMapper
  final static String healthId = "ctyun-load-balancer-instance-health"

  CtyunLoadBalancerInstanceStateCachingAgent(CtyunNamedAccountCredentials credentials,
                                               ObjectMapper objectMapper,
                                               String region) {
    this.credentials = credentials
    this.accountName = credentials.name
    this.region = region
    this.objectMapper = objectMapper
  }


  @Override
  String getHealthId() {
    healthId
  }

  @Override
  String getProviderName() {
    providerName
  }

  @Override
  String getAgentType() {
    return "$accountName/$region/${this.class.simpleName}"
  }

  @Override
  String getAccountName() {
    accountName
  }

  @Override
  Collection<AgentDataType> getProvidedDataTypes() {
    types
  }

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info("Enter loadData in ${agentType}")

    def targetHealths = getLoadBalancerTargetHealth(providerCache)
    Collection<String> evictions = providerCache.filterIdentifiers(HEALTH_CHECKS.ns, Keys.getTargetHealthKey('*', '*',
      '*', '*', accountName, region))

    List<CacheData> data = targetHealths.collect() { CtyunLoadBalancerTargetHealth targetHealth ->
      Map<String, Object> attributes = ["targetHealth": targetHealth]
      def targetHealthKey = Keys.getTargetHealthKey(targetHealth.loadBalancerId, targetHealth.listenerId,
                                               targetHealth.locationId, targetHealth.instanceId, accountName, region)
      def keepKey = evictions.find {
        it.equals(targetHealthKey)
      }
      if (keepKey) {
        evictions.remove(keepKey)
      }
      new DefaultCacheData(targetHealthKey, attributes, [:])
    }

    log.info("Caching ${data.size()} items evictions ${evictions.size()} items in ${agentType}")
    new DefaultCacheResult([(HEALTH_CHECKS.ns): data], [(HEALTH_CHECKS.ns): evictions])
  }


  private List<CtyunLoadBalancerTargetHealth> getLoadBalancerTargetHealth(ProviderCache providerCache) {
    //LoadBalancer中已有健康检查状态，从缓存获取
    Collection<String> identifiers = providerCache.filterIdentifiers(Keys.Namespace.LOAD_BALANCERS.ns,Keys.getLoadBalancerKey("*",accountName, region))

    def data = providerCache.getAll(Keys.Namespace.LOAD_BALANCERS.ns, identifiers, RelationshipCacheFilter.none())
    Set<CtyunLoadBalancer> lbs = data.collect{
      CtyunLoadBalancer loadBalancerDescription = objectMapper.convertValue(it.attributes, CtyunLoadBalancer)
      loadBalancerDescription
    }

    List<CtyunLoadBalancerTargetHealth> ctyunLBTargetHealths = []
    lbs.each{
      def loadBalancerId = it.loadBalancerId
      def listenerHealths = it.listeners
      listenerHealths.each {listenerHealth->
        def listenerId = listenerHealth.listenerId
        def ruleHealths = listenerHealth.rules
        def protocol = listenerHealth.protocol
        if(ruleHealths!=null&&ruleHealths.size()>0){
          ruleHealths.each {ruleHealth->
            def locationId = ''
            if (protocol == 'HTTP' || protocol == 'HTTPS') {
              locationId = ruleHealth.locationId
            }
            def tgID = ruleHealth.ruleTargetGroupId
            def instanceHealths = ruleHealth.targets
            if(instanceHealths!=null && instanceHealths.size()>0){
              instanceHealths.each {instanceHealth->
                def targetId = instanceHealth.targetId
                def instanceId = instanceHealth.instanceId
                def healthStatus = false
                if("active".equals(instanceHealth.healthCheckStatus)){
                  healthStatus = true
                }
                def port = instanceHealth.port
                def health = new CtyunLoadBalancerTargetHealth(instanceId:instanceId,targetId:targetId,
                  loadBalancerId:loadBalancerId, listenerId:listenerId, locationId:locationId,
                  healthStatus:healthStatus, port:port,targetGroupID: tgID )
                ctyunLBTargetHealths.add(health)
              }
            }
          }
        }
      }
    }

    return ctyunLBTargetHealths
  }
}
