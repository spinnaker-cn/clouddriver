package com.netflix.spinnaker.clouddriver.hecloud.provider.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.agent.*
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.DefaultCacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.core.provider.agent.HealthProvidingCachingAgent
import com.netflix.spinnaker.clouddriver.hecloud.cache.Keys
import com.netflix.spinnaker.clouddriver.hecloud.client.HeCloudElasticCloudServerClient
import com.netflix.spinnaker.clouddriver.hecloud.client.HeCloudLoadBalancerClient
import com.netflix.spinnaker.clouddriver.hecloud.model.loadbalance.HeCloudLoadBalancerTargetHealth
import com.netflix.spinnaker.clouddriver.hecloud.provider.HeCloudInfrastructureProvider
import com.netflix.spinnaker.clouddriver.hecloud.security.HeCloudNamedAccountCredentials
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.clouddriver.hecloud.cache.Keys.Namespace.HEALTH_CHECKS

@Slf4j
class HeCloudLoadBalancerInstanceStateCachingAgent implements CachingAgent, HealthProvidingCachingAgent, AccountAware {
  final String providerName = HeCloudInfrastructureProvider.name
  HeCloudNamedAccountCredentials credentials
  final String accountName
  final String region
  final ObjectMapper objectMapper
  final static String healthId = "hecloud-load-balancer-instance-health"


  HeCloudLoadBalancerInstanceStateCachingAgent(HeCloudNamedAccountCredentials credentials,
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

    def targetHealths = getLoadBalancerTargetHealth()
    Collection<String> evictions = providerCache.filterIdentifiers(HEALTH_CHECKS.ns, Keys.getTargetHealthKey('*', '*',
      '*', '*', accountName, region))

    List<CacheData> data = targetHealths.collect() { HeCloudLoadBalancerTargetHealth targetHealth ->
      Map<String, Object> attributes = ["targetHealth": targetHealth]
      def targetHealthKey = Keys.getTargetHealthKey(targetHealth.loadBalancerId, targetHealth.listenerId,
        targetHealth.poolId, targetHealth.instanceId, accountName, region)
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


  private List<HeCloudLoadBalancerTargetHealth> getLoadBalancerTargetHealth() {
    HeCloudLoadBalancerClient client = new HeCloudLoadBalancerClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )
    HeCloudElasticCloudServerClient ecsClient = new HeCloudElasticCloudServerClient(credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region)

    def poolSet = client.getAllPools()
    def memberHealths = client.getAllMembers()
    List<HeCloudLoadBalancerTargetHealth> hecloudLBTargetHealths = []
    HashMap<String, String> instanceMap = []
    for (pool in poolSet) {
      def loadBalancerIds = pool.getLoadbalancers()
      def loadBalancerId = ""
      if (loadBalancerIds.size() > 0) {
        loadBalancerId = loadBalancerIds[0].getId()
      }
      def listenerIds = pool.getListeners()
      def listenerId = ""
      if (listenerIds.size() > 0) {
        listenerId = listenerIds[0].getId()
      }

      def poolId = pool.getId()
      for (memberHealth in memberHealths) {
        if (memberHealth.getPoolId() != poolId) {
          continue
        }
        def healthStatus = memberHealth.getOperatingStatus().equals("ONLINE")
        def port = memberHealth.getProtocolPort()

        //通过实例ip地址获取实例id，memberId不是实例id
        def instanceId = ""

        if (instanceMap.containsKey(memberHealth.getAddress())) {
          instanceId = instanceMap.get(memberHealth.getAddress())
        } else {
          def instances = ecsClient.getInstancesByIp(memberHealth.getAddress())
          if (instances != null && instances.size() > 0) {
            instanceId = instances.get(0).getId()
            instanceMap.put(memberHealth.getAddress(), instanceId)
          }
        }

        if (!instanceId) {
          def memberId = memberHealth.getId()
          log.warn("The corresponding instance of member ${memberId} is not found")
          continue
        }
        def health = new HeCloudLoadBalancerTargetHealth(instanceId: instanceId,
          loadBalancerId: loadBalancerId, listenerId: listenerId, poolId: poolId,
          healthStatus: healthStatus, port: port)
        hecloudLBTargetHealths.add(health)
      }
    }

    return hecloudLBTargetHealths
  }
}
