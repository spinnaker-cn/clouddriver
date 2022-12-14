package com.netflix.spinnaker.clouddriver.hecloud.provider.agent

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hecloud.sdk.elb.model.L7policyResp
import com.hecloud.sdk.elb.model.LBListener
import com.hecloud.sdk.elb.model.Member
import com.hecloud.sdk.elb.model.Pool
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.*
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.DefaultCacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.cache.OnDemandAgent
import com.netflix.spinnaker.clouddriver.cache.OnDemandMetricsSupport
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudProvider
import com.netflix.spinnaker.clouddriver.hecloud.cache.Keys
import com.netflix.spinnaker.clouddriver.hecloud.client.HeCloudLoadBalancerClient
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudBasicResource
import com.netflix.spinnaker.clouddriver.hecloud.model.loadbalance.*
import com.netflix.spinnaker.clouddriver.hecloud.provider.HeCloudInfrastructureProvider
import com.netflix.spinnaker.clouddriver.hecloud.provider.view.MutableCacheData
import com.netflix.spinnaker.clouddriver.hecloud.security.HeCloudNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.names.NamerRegistry
import com.netflix.spinnaker.moniker.Moniker
import com.netflix.spinnaker.moniker.Namer
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.INFORMATIVE
import static com.netflix.spinnaker.clouddriver.hecloud.cache.Keys.Namespace.*

@Slf4j
class HeCloudLoadBalancerCachingAgent implements OnDemandAgent, CachingAgent, AccountAware {
  final String accountName
  final String region
  final ObjectMapper objectMapper
  final String providerName = HeCloudInfrastructureProvider.name
  HeCloudNamedAccountCredentials credentials
  final OnDemandMetricsSupport metricsSupport
  final Namer<HeCloudBasicResource> namer
  String onDemandAgentType = "${agentType}-OnDemand"

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(APPLICATIONS.ns),
    AUTHORITATIVE.forType(LOAD_BALANCERS.ns),
    INFORMATIVE.forType(INSTANCES.ns),
  ] as Set

  private static final TypeReference<Map<String, Object>> ATTRIBUTES = new TypeReference<Map<String, Object>>() {}

  HeCloudLoadBalancerCachingAgent(
    HeCloudNamedAccountCredentials credentials,
    ObjectMapper objectMapper,
    Registry registry,
    String region
  ) {
    this.credentials = credentials
    this.accountName = credentials.name
    this.region = region
    this.objectMapper = objectMapper
    this.metricsSupport = new OnDemandMetricsSupport(
      registry,
      this,
      "${HeCloudProvider.ID}:${OnDemandAgent.OnDemandType.LoadBalancer}")
    this.namer = NamerRegistry.lookup()
      .withProvider(HeCloudProvider.ID)
      .withAccount(credentials.name)
      .withResource(HeCloudBasicResource)
  }

  @Override
  String getAgentType() {
    return "$accountName/$region/${this.class.simpleName}"
  }

  @Override
  String getProviderName() {
    return providerName
  }

  @Override
  String getAccountName() {
    return accountName
  }

  List<HeCloudLoadBalancer> loadLoadBalancerData(String loadBalancerId = null) {
    HeCloudLoadBalancerClient client = new HeCloudLoadBalancerClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )

    def lbSet = []
    def lbIds = []
    if (loadBalancerId) {
      lbSet = client.getLoadBalancerById(loadBalancerId)
    } else {
      lbSet = client.getAllLoadBalancer()
    }
    lbSet?.each {
      lbIds.add(it.getId())
    }

    //根据elb的子网id，获取VPC ID
    //elb返回的是子网id详情中的子网id，但是通过子网id查询的参数是子网id详情中的网络id
/*    HeCloudVirtualPrivateCloudClient vpcClient = new HeCloudVirtualPrivateCloudClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )
    Map<String,String> vpcIdMap = new HashMap<>()
    Iterator<LoadBalancer> iterator = lbSet.iterator()
    while (iterator.hasNext()){
      LoadBalancer loadBalancer = iterator.next()
      if(!vpcIdMap.containsKey(loadBalancer.getVipSubnetId())){
        Subnet subnet = vpcClient.getSubnet(loadBalancer.getVipSubnetId())
        vpcIdMap.put(subnet.getId(),subnet.getVpcId())
        loadBalancer.setVpcId(subnet.getVpcId())
      }else {
        loadBalancer.setVpcId(vpcIdMap.get(loadBalancer.getVipSubnetId()))
      }
    }*/

    def poolSet = client.getAllPools(lbIds)
    def poolMap = poolSet?.collectEntries({ [(it.id): it] })
    def listenerMap = client.getAllLBListener(lbIds).collectEntries({ [(it.id): it] })
    def healthMonitorMap = client.getAllHealthMonitors().collectEntries({ [(it.id): it] })
    def membersMap = client.getAllMembers().groupBy { it.poolId }

    def loadBanancerList = lbSet?.collect {
      HeCloudLoadBalancer loadBalancer = new HeCloudLoadBalancer()
      loadBalancer.region = region
      loadBalancer.accountName = accountName
      loadBalancer.name = it.getName()
      loadBalancer.loadBalancerName = it.getName()
      loadBalancer.id = it.getId()
      loadBalancer.loadBalancerId = it.getId()
      loadBalancer.subnetId = it.getVipSubnetId()
      loadBalancer.vpcId = it.getVpcId()
      loadBalancer.loadBalancerVip = it.getVipAddress()
      loadBalancer.createTime = it.getCreateTime()

      List<Pool> queryPools = []
      it.getPools().each {
        def poolId = it.getId()
        def pool = poolMap?.get(poolId)
        if (pool) {
          queryPools.add(pool)
        }
      }
      def refinedPools = queryPools.collect {
        def pool = new HeCloudLoadBalancerPool()
        pool.poolId = it.getId()
        pool.poolName = it.getName()
        pool
      }
      loadBalancer.pools = refinedPools

      List<LBListener> queryListeners = []
      it.getListeners().each {
        def listenerId = it.getId()
        def listener = listenerMap?.get listenerId
        if (listener) {
          queryListeners.add(listener)
        }
      }

      def listenerIds = []
      queryListeners.each {
        listenerIds.add(it.getId())
      }
      def policySet = []
      if (listenerIds.size() > 0) {
        policySet = client.getAllL7policies(listenerIds)
      }

      def listeners = queryListeners.collect {
        def listener = new HeCloudLoadBalancerListener()
        listener.listenerId = it.getId()
        def protocol = it.getProtocol()
        if (protocol == "TERMINATED_HTTPS") {
          protocol = "HTTPS"
          listener.certificate = new HeCloudLoadBalancerCertificate()
          listener.certificate.certId = it.getDefaultTlsContainerRef()
          listener.certificate.certCaId = it.getClientCaTlsContainerRef()
        }

        listener.protocol = protocol
        listener.port = it.getProtocolPort()
        listener.listenerName = it.getName()

        if (protocol == "TCP" || protocol == "UDP") {
          def pools = poolSet
          def listenerId = null
          for (int i = 0; i < pools.size(); i++) {
            listenerId = pools[i].getListeners().find {
              it.getId() == listener.listenerId
            }?.getId()
            if (listenerId) {
              def pool = pools[i]
              listener.poolId = pool.getId()
              if (pool.getHealthmonitorId()) {
                def healthMonitor = healthMonitorMap?.get pool.getHealthmonitorId()
                if (healthMonitor) {
                  listener.healthCheck = new HeCloudLoadBalancerHealthCheck()
                  listener.healthCheck.timeOut = healthMonitor.getTimeout()
                  listener.healthCheck.intervalTime = healthMonitor.getDelay()
                  listener.healthCheck.maxRetries = healthMonitor.getMaxRetries()
                  listener.healthCheck.httpCheckPath = healthMonitor.getUrlPath()
                  listener.healthCheck.httpCheckDomain = healthMonitor.getDomainName()
                }
              }
              List<Member> members = membersMap?.get(listener.poolId)
              listener.targets = members?.collect {
                def target = new HeCloudLoadBalancerTarget()
                target.instanceId = it.getId()
                target.port = it.getProtocolPort()
                target.weight = it.getWeight()
                target
              }

              break
            }
          }
        }

        List<L7policyResp> policies = []
        def listenerId = it.getId()
        policySet.each {
          if (it.getListenerId() == listenerId) {
            policies.add(it)
          }
        }
        def rules = policies?.collect() {
          def rule = new HeCloudLoadBalancerRule()
          rule.policyId = it.getId()
          rule.poolId = it.getRedirectPoolId()
          if (rule.poolId) {
            def pool = client.getPool(rule.poolId)
            if (pool.getHealthmonitorId()) {
              def healthMonitor = healthMonitorMap.get pool.getHealthmonitorId()
              if (healthMonitor) {
                rule.healthCheck = new HeCloudLoadBalancerHealthCheck()
                rule.healthCheck.timeOut = healthMonitor.getTimeout()
                rule.healthCheck.intervalTime = healthMonitor.getDelay()
                rule.healthCheck.maxRetries = healthMonitor.getMaxRetries()
                rule.healthCheck.httpCheckPath = healthMonitor.getUrlPath()
                rule.healthCheck.httpCheckDomain = healthMonitor.getDomainName()
              }
            }

            List<Member> members = membersMap?.get(listener.poolId)
            rule.targets = members?.collect {
              def target = new HeCloudLoadBalancerTarget()
              target.instanceId = it.getId()
              target.port = it.getProtocolPort()
              target.weight = it.getWeight()
              target
            }
          }
          rule
        }

        listener.rules = rules
        listener
      }
      loadBalancer.listeners = listeners
      loadBalancer
    }
    return loadBanancerList
  }

  @Override
  boolean handles(OnDemandAgent.OnDemandType type, String cloudProvider) {
    type == OnDemandAgent.OnDemandType.LoadBalancer && cloudProvider == HeCloudProvider.ID
  }

  @Override
  OnDemandAgent.OnDemandResult handle(ProviderCache providerCache, Map<String, ? extends Object> data) {
    log.info("Enter handle, data = ${data}")
    if (!data.containsKey("loadBalancerId") ||
      !data.containsKey("account") ||
      !data.containsKey("region") ||
      accountName != data.account ||
      region != data.region) {
      return null
    }

    def loadBalancer = metricsSupport.readData {
      loadLoadBalancerData(data.loadBalancerId as String)[0]
    }
    if (!loadBalancer) {
      log.info("Can not find loadBalancer ${data.loadBalancerId}")
      return null
    }

    def cacheResult = metricsSupport.transformData {
      buildCacheResult([loadBalancer], null, null)
    }

    def cacheResultAsJson = objectMapper.writeValueAsString(cacheResult.cacheResults)
    def loadBalancerKey = Keys.getLoadBalancerKey(data.loadBalancerId as String, accountName, region)
    if (cacheResult.cacheResults.values().flatten().empty) {
      // Avoid writing an empty onDemand cache record (instead delete any that may have previously existed).
      providerCache.evictDeletedItems ON_DEMAND.ns, [loadBalancerKey]
    } else {
      metricsSupport.onDemandStore {
        def cacheData = new DefaultCacheData(
          loadBalancerKey,
          10 * 60,
          [
            cacheTime   : new Date(),
            cacheResults: cacheResultAsJson
          ],
          [:]
        )
        providerCache.putCacheData ON_DEMAND.ns, cacheData
      }
    }

    Map<String, Collection<String>> evictions = loadBalancer ? [:] : [
      (LOAD_BALANCERS.ns): [loadBalancerKey]]

    return new OnDemandResult(
      sourceAgentType: getOnDemandAgentType(),
      cacheResult: cacheResult,
      evictions: evictions
    )
  }

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info("Enter LoadBalancerCacheingAgent loadData ")

    def loadBalancerSet = loadLoadBalancerData()
    log.info("Total loadBanancre Number = ${loadBalancerSet.size()} in ${agentType}")
    def toEvictOnDemandCacheData = []
    def toKeepOnDemandCacheData = []

    Long start = System.currentTimeMillis()
    def loadBalancerKeys = loadBalancerSet.collect {
      Keys.getLoadBalancerKey(it.id, credentials.name, region)
    } as Set<String>

    def pendingOnDemandRequestKeys = providerCache
      .filterIdentifiers(
        ON_DEMAND.ns,
        Keys.getLoadBalancerKey("*", credentials.name, region))
      .findAll { loadBalancerKeys.contains(it) }

    def pendingOnDemandRequestsForloadBalancer = providerCache.getAll(ON_DEMAND.ns, pendingOnDemandRequestKeys)
    pendingOnDemandRequestsForloadBalancer.each {
      if (it.attributes.cacheTime < start && it.attributes.processedCount > 0) {
        toEvictOnDemandCacheData << it
      } else {
        toKeepOnDemandCacheData << it
      }
    }

    CacheResult result = buildCacheResult(loadBalancerSet, toKeepOnDemandCacheData, toEvictOnDemandCacheData)

    result.cacheResults[ON_DEMAND.ns].each {
      it.attributes.processedTime = System.currentTimeMillis()
      it.attributes.processedCount = (it.attributes.processedCount ?: 0) + 1
    }

    return result
  }

  private CacheResult buildCacheResult(Collection<HeCloudLoadBalancer> loadBalancerSet,
                                       Collection<CacheData> toKeepOnDemandCacheData,
                                       Collection<CacheData> toEvictOnDemandCacheData) {
    log.info "Start build cache for $agentType"

    Map<String, Collection<CacheData>> cacheResults = [:]
    Map<String, Collection<String>> evictions = toEvictOnDemandCacheData ? [(ON_DEMAND.ns): toEvictOnDemandCacheData*.id] : [:]

    Map<String, Map<String, CacheData>> namespaceCache = [:].withDefault {
      namespace -> [:].withDefault { id -> new MutableCacheData(id as String) }
    }

    loadBalancerSet?.each {
      Moniker moniker = namer.deriveMoniker it
      def applicationName = moniker.app
      if (applicationName == null) {
        return
      }

      def loadBalancerKey = Keys.getLoadBalancerKey(it.id, accountName, region)
      def appKey = Keys.getApplicationKey(applicationName)

      // application
      def applications = namespaceCache[APPLICATIONS.ns]
      applications[appKey].attributes.name = applicationName
      applications[appKey].relationships[LOAD_BALANCERS.ns].add(loadBalancerKey)
      // compare onDemand
      def onDemandLoadBalancerCache = false
      if (onDemandLoadBalancerCache) {
        //mergeOnDemandCache(onDemandLoadBalancerCache, namespaceCache)
      } else {
        // LoadBalancer
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.application = applicationName
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.name = it.name
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.region = it.region
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.id = it.id
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.loadBalancerId = it.loadBalancerId
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.accountName = accountName
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.subnetId = it.subnetId
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.vpcId = it.vpcId
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.createTime = it.createTime
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.loadBalancerVip = it.loadBalancerVip
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.listeners = [] as List<HeCloudLoadBalancerListener>
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.pools = [] as List<HeCloudLoadBalancerPool>
        it.listeners.each {
          def listener = new HeCloudLoadBalancerListener()
          listener.copyListener(it)
          namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.listeners.add(listener)
        }
        it.pools.each {
          def pool = new HeCloudLoadBalancerPool()
          pool.copyPool(it)
          namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.pools.add(pool)
        }
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].relationships[APPLICATIONS.ns].add(appKey)
      }
    }

    namespaceCache.each { String namespace, Map<String, CacheData> cacheDataMap ->
      cacheResults[namespace] = cacheDataMap.values()
    }
    cacheResults[ON_DEMAND.ns] = toKeepOnDemandCacheData
    if (cacheResults[LOAD_BALANCERS.ns] == null) {
      cacheResults[LOAD_BALANCERS.ns] = []
    }
    CacheResult result = new DefaultCacheResult(
      cacheResults, evictions
    )
    result
  }

  def mergeOnDemandCache(CacheData onDemandLoadBalancerCache, Map<String, Map<String, CacheData>> namespaceCache) {
    Map<String, List<MutableCacheData>> onDemandCache = objectMapper.readValue(
      onDemandLoadBalancerCache.attributes.cacheResults as String,
      new TypeReference<Map<String, List<MutableCacheData>>>() {})

    onDemandCache.each { String namespace, List<MutableCacheData> cacheDataList ->
      if (namespace != 'onDemand') {
        cacheDataList.each {
          def existingCacheData = namespaceCache[namespace][it.id]
          if (!existingCacheData) {
            namespaceCache[namespace][it.id] = it
          } else {
            existingCacheData.attributes.putAll(it.attributes)
            it.relationships.each { String relationshipName, Collection<String> relationships ->
              existingCacheData.relationships[relationshipName].addAll(relationships)
            }
          }
        }
      }
    }
  }

  @Override
  Collection<Map> pendingOnDemandRequests(ProviderCache providerCache) {
    return []
  }

  @Override
  Optional<Map<String, String>> getCacheKeyPatterns() {
    return [
           (LOAD_BALANCERS.ns): Keys.getLoadBalancerKey("*", accountName, region),
    ]
  }
}
