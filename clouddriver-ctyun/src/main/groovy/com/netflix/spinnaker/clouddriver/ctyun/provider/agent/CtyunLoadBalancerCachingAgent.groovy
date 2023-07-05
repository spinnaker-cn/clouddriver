package com.netflix.spinnaker.clouddriver.ctyun.provider.agent

import com.alibaba.fastjson.JSONObject
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.*
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.DefaultCacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.cache.OnDemandAgent
import com.netflix.spinnaker.clouddriver.cache.OnDemandMetricsSupport
import com.netflix.spinnaker.clouddriver.ctyun.CtyunCloudProvider
import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys
import com.netflix.spinnaker.clouddriver.ctyun.client.LoadBalancerClient
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunBasicResource
import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.*
import com.netflix.spinnaker.clouddriver.ctyun.provider.CtyunInfrastructureProvider
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.MutableCacheData
import com.netflix.spinnaker.clouddriver.ctyun.security.CtyunNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.names.NamerRegistry
import com.netflix.spinnaker.moniker.Moniker
import com.netflix.spinnaker.moniker.Namer
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.INFORMATIVE
import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.*

@Slf4j
class CtyunLoadBalancerCachingAgent implements OnDemandAgent, CachingAgent, AccountAware{
  final String accountName
  final String region
  final ObjectMapper objectMapper
  final String providerName = CtyunInfrastructureProvider.name
  CtyunNamedAccountCredentials credentials
  final OnDemandMetricsSupport metricsSupport
  final Namer<CtyunBasicResource> namer
  String onDemandAgentType = "${agentType}-OnDemand"

  Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(APPLICATIONS.ns),
    AUTHORITATIVE.forType(LOAD_BALANCERS.ns),
    INFORMATIVE.forType(INSTANCES.ns),
  ] as Set

  private static final TypeReference<Map<String, Object>> ATTRIBUTES = new TypeReference<Map<String, Object>>() {}

  CtyunLoadBalancerCachingAgent(
    CtyunNamedAccountCredentials credentials,
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
      "${CtyunCloudProvider.ID}:${OnDemandAgent.OnDemandType.LoadBalancer}")
    this.namer = NamerRegistry.lookup()
      .withProvider(CtyunCloudProvider.ID)
      .withAccount(credentials.name)
      .withResource(CtyunBasicResource)
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

  List<CtyunLoadBalancer> loadLoadBalancerData(String loadBalancerId = null) {
    LoadBalancerClient client = new LoadBalancerClient(
      credentials.credentials.accessKey,
      credentials.credentials.securityKey,
      region
    )

    def lbSet = []
    if (loadBalancerId) {
      lbSet = client.getLoadBalancerById(loadBalancerId)
    } else {
      lbSet = client.getAllLoadBalancer()
    }

    def loadBanancerList =  lbSet.collect {
      CtyunLoadBalancer loadBalancer = new CtyunLoadBalancer()
      loadBalancer.region = region
      loadBalancer.accountName = accountName
      loadBalancer.name = it.name
      loadBalancer.loadBalancerName = it.name
      loadBalancer.id = it.ID
      loadBalancer.loadBalancerId = it.ID
      loadBalancer.loadBalancerType = it.resourceType
      loadBalancer.vpcId = it.vpcID
      loadBalancer.subnetId = it.subnetID
      loadBalancer.createTime = it.createdTime
      loadBalancer.loadBalacnerVips = [it.privateIpAddress]
      loadBalancer.securityGroups = new ArrayList<String>();

      def queryListeners = client.getAllLBListener(loadBalancer.id)
      /*def listenerIdList = queryListeners.collect {
        it.ID
      } as List<String>*/
      //all listener's targets
      def lbTargetList = []
      /*if (listenerIdList.size() > 0) {
        lbTargetList = client.getLBTargetList(loadBalancer.id, listenerIdList)
      }*/
      /*取监听的后端主机组id*/
      /*def targetGroupIDList = queryListeners.collect {
        it.defaultAction.forwardConfig.targetGroups[0].targetGroupID
      } as List<String>
      if (targetGroupIDList.size() > 0) {
        lbTargetList = client.getLBTargetList(targetGroupIDs)
      }*/
      //查询转发规则
      def queryRules = client.getAllRule(loadBalancer.id)


      def listeners = queryListeners.collect {
        def listener = new CtyunLoadBalancerListener()
        listener.listenerId = it.ID
        listener.protocol = it.protocol
        listener.port = it.protocolPort
        //listener.scheduler = it.scheduler
        def targetGroups = client.getLBTargetGroupList(it.defaultAction.forwardConfig.targetGroups[0].targetGroupID)
        //取服务组的调度算法字段
        if(targetGroups.size()>0){
          if("wrr".equals(targetGroups[0].algorithm)){
            listener.scheduler = "WRR";
          }else if("lc".equals(targetGroups[0].algorithm)){
            listener.scheduler = "LEAST_CONN";
          }else {
            listener.scheduler = targetGroups[0].algorithm
          }
          listener.targetGroupId = targetGroups[0].ID
          listener.targetGroupName = targetGroups[0].name
        }
        //listener.sessionExpireTime = it.sessionExpireTime
        //listener.sessionExpireTime = 0
        //listener.sniSwitch = it.sniSwitch
        //listener.sniSwitch = -100
        listener.listenerName = it.name
        if ("HTTPS".equals(it.protocol)) {        //listener.certificate
          listener.certificate = new CtyunLoadBalancerCertificate()
          //true,双向认证
          if(it.caEnabled){
            //true,双向认证
            listener.certificate.sslMode = "MUTUAL"
          }else {
            //单项认证
            listener.certificate.sslMode = "UNIDIRECTIONAL"
          }
          listener.certificate.certId = it.certificateID
          listener.certificate.certCaId = it.clientCertificateID
        }
        //listener healtch check
        if (targetGroups[0].healthCheckID != null&&targetGroups[0].healthCheckID.size()>0) {
          listener.healthCheck = new CtyunLoadBalancerHealthCheck()
          //查询健康检查信息

          def healthchecks = client.getHealthcheckList(targetGroups[0].healthCheckID)
          if(healthchecks.size()>0){
            //是否开启了健康检查：1（开启）、0（关闭）
            listener.healthCheck.healthSwitch = 1
            listener.healthCheck.timeOut = healthchecks[0].timeout
            listener.healthCheck.intervalTime = healthchecks[0].Integererval
            //listener.healthCheck.healthNum = it.healthCheck.healthNum
            listener.healthCheck.unHealthNum = healthchecks[0].maxRetry
            listener.healthCheck.httpCode = healthchecks[0].httpExpectedCodes
            listener.healthCheck.httpCheckPath = healthchecks[0].httpUrlPath
            //listener.healthCheck.httpCheckDomain = it.healthCheck.httpCheckDomain
            listener.healthCheck.httpCheckMethod = healthchecks[0].httpMethod
          }
        }
        //targets 4 layer
        def lbTargets = client.getLBTargets(it.defaultAction.forwardConfig.targetGroups[0].targetGroupID)
        /*def lbTargets = lbTargetList.findAll {
          it.listenerId.equals(listener.listenerId)
        }*/
        listener.targets = lbTargets.collect { targetEntry ->
          def target = new CtyunLoadBalancerTarget()
          if (targetEntry != null) {
            target.instanceId = targetEntry.instanceID
            target.port = targetEntry.protocolPort
            target.weight = targetEntry.weight
            target.type = targetEntry.instanceType
            target.targetId = targetEntry.ID
            target.targetGroupID = targetEntry.targetGroupID
            target.healthCheckStatus = targetEntry.healthCheckStatus
            target.healthCheckStatusIpv6 = targetEntry.healthCheckStatusIpv6
          }
          target
        }
        /*lbTargets.each { listenBackend ->
          listener.targets = listenBackend.Targets.collect { targetEntry ->
            if (targetEntry != null) {
              def target = new CtyunLoadBalancerTarget()
              target.instanceId = targetEntry.instanceId
              target.port = targetEntry.port
              target.weight = targetEntry.weight
              target.type = targetEntry.type
              target
            }
          }
        }*/
        def listenerRules = queryRules.findAll(){
          it.listenerID.equals(listener.listenerId)
        }
        //rules
        def rules = listenerRules.collect() {
          def rule = new CtyunLoadBalancerRule()
          rule.locationId = it.ID
          if(it.conditions!=null && it.conditions.size()>0){
            it.conditions.each {
              if("server_name".equals(it.type)){
                rule.domain = it.serverNameConfig.serverName
              }else if("url_path".equals(it.type)){
                rule.url = it.urlPathConfig.urlPaths
              }
            }
          }
          rule.certificate = new CtyunLoadBalancerCertificate()
          /*if (it.certificate != null) {               //rule.certificate
            rule.certificate = new CtyunLoadBalancerCertificate()
            rule.certificate.sslMode = it.certificate.SSLMode
            rule.certificate.certId = it.certificate.certId
            rule.certificate.certCaId = it.certificate.certCaId
          }*/
          def ruleTargetGroups = client.getLBTargetGroupList(it.action.forwardConfig.targetGroups[0].targetGroupID)

          //转发规则对应的后端服务组信息
          rule.ruleTargetGroupId = ruleTargetGroups[0].ID
          rule.ruleTargetGroupName = ruleTargetGroups[0].name
          //转发规则对应的健康检查信息
          if (ruleTargetGroups[0].healthCheckID != null&&ruleTargetGroups[0].healthCheckID.size()>0) {
            //查询健康检查信息
            def ruleHealthchecks = client.getHealthcheckList(ruleTargetGroups[0].healthCheckID)
            if(ruleHealthchecks.size()>0){
              rule.healthCheck = new CtyunLoadBalancerHealthCheck()
              //是否开启了健康检查：1（开启）、0（关闭）
              rule.healthCheck.healthSwitch = 1
              rule.healthCheck.timeOut = ruleHealthchecks[0].timeout
              rule.healthCheck.intervalTime = ruleHealthchecks[0].Integererval
              //rule.healthCheck.healthNum = it.healthCheck.healthNum
              rule.healthCheck.unHealthNum = ruleHealthchecks[0].maxRetry
              rule.healthCheck.httpCode = ruleHealthchecks[0].httpExpectedCodes
              rule.healthCheck.httpCheckPath = ruleHealthchecks[0].httpUrlPath
              //rule.healthCheck.httpCheckDomain = ruleHealthchecks[0].httpCheckDomain
              rule.healthCheck.httpCheckMethod = ruleHealthchecks[0].httpMethod
            }
          }

          //rule targets 7Larer
          def ruleTargets = client.getLBTargets(it.action.forwardConfig.targetGroups[0].targetGroupID)

          rule.targets = ruleTargets.collect { ruletargetEntry ->
            def target = new CtyunLoadBalancerTarget()
            if (ruletargetEntry != null) {
              target.instanceId = ruletargetEntry.instanceID
              target.port = ruletargetEntry.protocolPort
              target.weight = ruletargetEntry.weight
              target.type = ruletargetEntry.instanceType
              target.targetId = ruletargetEntry.ID
              target.targetGroupID = ruletargetEntry.targetGroupID
              target.healthCheckStatus = ruletargetEntry.healthCheckStatus
              target.healthCheckStatusIpv6 = ruletargetEntry.healthCheckStatusIpv6
            }
            target
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
    type == OnDemandAgent.OnDemandType.LoadBalancer && cloudProvider == CtyunCloudProvider.ID
  }

  @Override
  OnDemandAgent.OnDemandResult handle(ProviderCache providerCache, Map<String, ? extends Object> data) {
    log.info("Enter handle, data = ${data}")
    if (!data.containsKey("loadBalancerId") ||
      !data.containsKey("account") ||
      !data.containsKey("region")  ||
      accountName != data.account ||
      region != data.region) {
      return null
    }

    def loadBalancer =  metricsSupport.readData {
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
    if(loadBalancerSet.size()==0&&providedDataTypes.size()==3){
      log.info("没有负载均衡数据，去掉applications数据类型，未删除前providedDataTypes={}", JSONObject.toJSONString(providedDataTypes))
      providedDataTypes=providedDataTypes.findAll {
        it.getTypeName()!=APPLICATIONS.ns
      }
      log.info("没有负载均衡数据，去掉applications数据类型，删除后providedDataTypes={}", JSONObject.toJSONString(providedDataTypes))
    }else if(loadBalancerSet.size()>0&&providedDataTypes.size()<3){
      log.info("有负载均衡数据，添加applications数据类型，未添加前providedDataTypes={}", JSONObject.toJSONString(providedDataTypes))
      providedDataTypes.add(AUTHORITATIVE.forType(APPLICATIONS.ns))
      log.info("有负载均衡数据，添加applications数据类型，添加后providedDataTypes={}", JSONObject.toJSONString(providedDataTypes))
    }
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

    /*
    result.cacheResults.each { String namespace, Collection<CacheData> caches->
      log.info "namespace $namespace"
      caches.each{
        log.info "attributes: $it.attributes, relationships: $it.relationships"
      }
    }*/
    return result
  }

  private CacheResult buildCacheResult(Collection<CtyunLoadBalancer> loadBalancerSet,
                                       Collection<CacheData> toKeepOnDemandCacheData,
                                       Collection<CacheData> toEvictOnDemandCacheData) {
    log.info "Start build cache for $agentType"

    Map<String, Collection<CacheData>> cacheResults = [:]
    Map<String, Collection<String>> evictions = toEvictOnDemandCacheData ? [(ON_DEMAND.ns):toEvictOnDemandCacheData*.id] : [:]

    Map<String, Map<String, CacheData>> namespaceCache = [:].withDefault {
      namespace->[:].withDefault {id->new MutableCacheData(id as String)}
    }

    loadBalancerSet.each {
      Moniker moniker = namer.deriveMoniker it
      def applicationName = moniker.app
      if (applicationName == null) {
        return  //=continue
      }

      def loadBalancerKey = Keys.getLoadBalancerKey(it.id, accountName, region)
      def appKey = Keys.getApplicationKey(applicationName)
      //List<String> instanceKeys = []

      // application
      def applications = namespaceCache[APPLICATIONS.ns]
      applications[appKey].attributes.name = applicationName
      applications[appKey].relationships[LOAD_BALANCERS.ns].add(loadBalancerKey)

      // compare onDemand
      //def onDemandLoadBalancerCache = toKeepOnDemandCacheData.find {
      //  it.id == loadBalancerKey
      //}
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
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.vpcId = it.vpcId
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.subnetId = it.subnetId
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.loadBalancerType = it.loadBalancerType
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.createTime = it.createTime
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.loadBalacnerVips = []
        it.loadBalacnerVips.each {
          def vip = new String(it)
          namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.loadBalacnerVips.add(vip)
        }
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.securityGroups = []
        it.securityGroups.each {
          def sg = new String(it)
          namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.securityGroups.add(sg)
        }
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.listeners = [] as List<CtyunLoadBalancerListener>
        it.listeners.each {
          def listener = new CtyunLoadBalancerListener()
          listener.copyListener(it)
          namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.listeners.add(listener)
        }
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].relationships[APPLICATIONS.ns].add(appKey)
      }
    }

    namespaceCache.each {String namespace, Map<String, CacheData> cacheDataMap ->
      cacheResults[namespace] = cacheDataMap.values()
    }
    cacheResults[ON_DEMAND.ns] = toKeepOnDemandCacheData

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


}
