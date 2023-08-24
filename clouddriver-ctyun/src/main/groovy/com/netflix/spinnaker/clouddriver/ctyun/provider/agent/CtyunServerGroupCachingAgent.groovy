package com.netflix.spinnaker.clouddriver.ctyun.provider.agent

import cn.ctyun.ctapi.scaling.configcreate.Tag
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.*
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.DefaultCacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.cache.OnDemandAgent
import com.netflix.spinnaker.clouddriver.cache.OnDemandMetricsSupport
import com.netflix.spinnaker.clouddriver.ctyun.client.CloudVirtualMachineClient
import com.netflix.spinnaker.clouddriver.names.NamerRegistry
import com.netflix.spinnaker.clouddriver.ctyun.CtyunCloudProvider
import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunAutoScalingClient
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunBasicResource
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunInstance
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunServerGroup
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.MutableCacheData
import com.netflix.spinnaker.clouddriver.ctyun.security.CtyunNamedAccountCredentials
import com.netflix.spinnaker.moniker.Moniker
import com.netflix.spinnaker.moniker.Namer
import groovy.util.logging.Slf4j

import java.text.SimpleDateFormat

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.INFORMATIVE
import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.*
@Slf4j
class CtyunServerGroupCachingAgent extends AbstractCtyunCachingAgent implements OnDemandAgent {
  String onDemandAgentType = "$agentType-OnDemand"

  final OnDemandMetricsSupport metricsSupport
  final Namer<CtyunBasicResource> namer

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(Keys.Namespace.APPLICATIONS.ns),
    AUTHORITATIVE.forType(Keys.Namespace.SERVER_GROUPS.ns),
    INFORMATIVE.forType(Keys.Namespace.CLUSTERS.ns),
    INFORMATIVE.forType(Keys.Namespace.INSTANCES.ns),
    INFORMATIVE.forType(Keys.Namespace.LOAD_BALANCERS.ns)  // todo
  ] as Set

  CtyunServerGroupCachingAgent(
    CtyunNamedAccountCredentials credentials,
    ObjectMapper objectMapper,
    Registry registry,
    String region
  ) {
    super(credentials, objectMapper, region)
    this.metricsSupport = new OnDemandMetricsSupport(
      registry,
      this,
      "$CtyunCloudProvider.ID:$OnDemandType.ServerGroup")
    this.namer = NamerRegistry.lookup()
      .withProvider(CtyunCloudProvider.ID)
      .withAccount(credentials.name)
      .withResource(CtyunBasicResource)
  }

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    Long start = System.currentTimeMillis()
    log.info "start load data"
    CtyunAutoScalingClient client = new CtyunAutoScalingClient(
      credentials.credentials.accessKey,
      credentials.credentials.securityKey,
      region
    )
    CloudVirtualMachineClient cvmClient = new CloudVirtualMachineClient(
      credentials.credentials.accessKey,
      credentials.credentials.securityKey,
      region
    )
    def serverGroups = loadAsgAsServerGroup(client,cvmClient)
    def toEvictOnDemandCacheData = [] as List<CacheData>
    def toKeepOnDemandCacheData = [] as List<CacheData>

    def serverGroupKeys = serverGroups.collect {
      Keys.getServerGroupKey(it.name, credentials.name, region)
    } as Set<String>

    def pendingOnDemandRequestKeys = providerCache
      .filterIdentifiers(
      ON_DEMAND.ns,
      Keys.getServerGroupKey("*", "*", credentials.name, region))
      .findAll { serverGroupKeys.contains(it) }

    def pendingOnDemandRequestsForServerGroups = providerCache.getAll(ON_DEMAND.ns, pendingOnDemandRequestKeys)
    pendingOnDemandRequestsForServerGroups.each {
      if (it.attributes.cacheTime < start && it.attributes.processedCount > 0) {
        toEvictOnDemandCacheData << it
      } else {
        toKeepOnDemandCacheData << it
      }
    }

    CacheResult result = buildCacheResult(
      serverGroups, toKeepOnDemandCacheData, toEvictOnDemandCacheData)

    result.cacheResults[ON_DEMAND.ns].each {
      it.attributes.processedTime = System.currentTimeMillis()
      it.attributes.processedCount = (it.attributes.processedCount ?: 0) + 1
    }
    result
  }

  private CacheResult buildCacheResult(Collection<CtyunServerGroup> serverGroups,
                                       Collection<CacheData> toKeepOnDemandCacheData,
                                       Collection<CacheData> toEvictOnDemandCacheData) {
    log.info "Start build cache for $agentType"

    Map<String, Collection<CacheData>> cacheResults = [:]
    Map<String, Collection<String>> evictions = [(ON_DEMAND.ns):toEvictOnDemandCacheData*.id]

    Map<String, Map<String, CacheData>> namespaceCache = [:].withDefault {
      namespace->[:].withDefault {id->new MutableCacheData(id as String)}
    }

    serverGroups.each {
      Moniker moniker = namer.deriveMoniker it
      def applicationName = moniker.app
      def clusterName = moniker.cluster

      if (applicationName == null || clusterName == null) {
        return
      }

      def serverGroupKey = Keys.getServerGroupKey it.name, accountName, region
      def clusterKey = Keys.getClusterKey clusterName, applicationName, accountName
      def appKey = Keys.getApplicationKey applicationName
      List<String> instanceKeys = []
      List<String> loadBalancerKeys = []

      Set<CtyunInstance> instances = it.instances
      instances.each {instance ->
        instanceKeys.add Keys.getInstanceKey(instance.name as String, accountName, region)
      }

      def loadBalancerIds = it.loadBalancers
      loadBalancerIds.each { String lbId ->
        loadBalancerKeys.add Keys.getLoadBalancerKey(lbId, accountName, region)
      }

      // application
      def applications = namespaceCache[APPLICATIONS.ns]
      applications[appKey].attributes.name = applicationName
      applications[appKey].relationships[CLUSTERS.ns].add clusterKey
      applications[appKey].relationships[SERVER_GROUPS.ns].add serverGroupKey

      // cluster
      namespaceCache[CLUSTERS.ns][clusterKey].attributes.name = clusterName
      namespaceCache[CLUSTERS.ns][clusterKey].attributes.accountName = accountName
      namespaceCache[CLUSTERS.ns][clusterKey].relationships[APPLICATIONS.ns].add appKey
      namespaceCache[CLUSTERS.ns][clusterKey].relationships[SERVER_GROUPS.ns].add serverGroupKey
      namespaceCache[CLUSTERS.ns][clusterKey].relationships[INSTANCES.ns].addAll instanceKeys
      namespaceCache[CLUSTERS.ns][clusterKey].relationships[LOAD_BALANCERS.ns].addAll loadBalancerKeys

      // loadBalancer
      loadBalancerKeys.each { lbKey ->
        namespaceCache[LOAD_BALANCERS.ns][lbKey].relationships[SERVER_GROUPS.ns].add serverGroupKey
      }

      // server group
      def onDemandServerGroupCache = toKeepOnDemandCacheData.find {
        it.attributes.name == serverGroupKey
      }
      if (onDemandServerGroupCache) {
        mergeOnDemandCache onDemandServerGroupCache, namespaceCache
      } else {

        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.asg = it.asg
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.cooldown = it.cooldown
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.mazInfoList=it.mazInfoList
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.vpcId=it.vpcId
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.accountName = accountName
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.name = it.name
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.region = it.region
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.launchConfig = it.launchConfig
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.disabled = it.disabled
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.scalingPolicies = it.scalingPolicies
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.scheduledActions = it.scheduledActions
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.loadBlanders=it.loadBlanders
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].relationships[APPLICATIONS.ns].add appKey
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].relationships[CLUSTERS.ns].add clusterKey
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].relationships[INSTANCES.ns].addAll instanceKeys
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].relationships[LOAD_BALANCERS.ns].addAll loadBalancerKeys
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

  def mergeOnDemandCache(CacheData onDemandServerGroupCache, Map<String, Map<String, CacheData>>  namespaceCache) {
    Map<String, List<MutableCacheData>> onDemandCache = objectMapper.readValue(
      onDemandServerGroupCache.attributes.cacheResults as String,
      new TypeReference<Map<String, List<MutableCacheData>>>() {})

    onDemandCache.each { String namespace, List<MutableCacheData> cacheDataList ->
      if (namespace != 'onDemand') {
        cacheDataList.each {
          def existingCacheData = namespaceCache[namespace][it.id]
          if (!existingCacheData) {
            namespaceCache[namespace][it.id] = it
          } else {
            existingCacheData.attributes.putAll it.attributes
            it.relationships.each { String relationshipName, Collection<String> relationships ->
              existingCacheData.relationships[relationshipName].addAll relationships
            }
          }
        }
      }
    }
  }

  List<CtyunServerGroup> loadAsgAsServerGroup(CtyunAutoScalingClient client,CloudVirtualMachineClient cvmClient, String serverGroupName = null) {
    def asgs
    if (serverGroupName) {
      asgs = client.getAutoScalingGroupsByName serverGroupName
    } else {
      asgs = client.getAllAutoScalingGroups()
    }
    List<CtyunServerGroup> serverGroupList=new ArrayList<>();
    int i=1
    asgs.each {
      log.info("=========================================伸缩组数据处理第{}次--start！",i)
      try{

        def autoScalingGroupId = it.groupID
        def autoScalingGroupName = it.name
        def disabled = it.status == 2//1是启用，2是停用
        CtyunServerGroup serverGroup = new CtyunServerGroup().with {
          it.accountName = this.accountName
          it.region = this.region
          it.name = autoScalingGroupName
          it.disabled = disabled
          it
        }
        Map<String, Object> asg = objectMapper.convertValue it, ATTRIBUTES
        serverGroup.asg = asg


        def getLaunchConfiguration=client.getLaunchConfiguration(it.configID,it.groupID)
        getLaunchConfiguration.setSecurityGroupList(Arrays.asList(it.getSecurityGroupIDList()))
        /*测试用*/
       /* List<Tag> tagList=new ArrayList<>()
        Tag tag=new Tag()
        tag.setKey("testKey")
        tag.setValue("testValue")
        tagList.add(tag)
        Tag tag2=new Tag()
        tag2.setKey("testKey2")
        tag2.setValue("testValue2")
        tagList.add(tag2)
        getLaunchConfiguration.setTags(tagList)*/

        Map<String, Object> asc = objectMapper.convertValue getLaunchConfiguration, ATTRIBUTES
        serverGroup.launchConfig = asc

        //def keyIds=cvmClient.keyPairs
        //serverGroup.launchConfig.loginSettings.keyIds=
        List<Map<String, Object>> systemDisk=new ArrayList<>()
        List<Map<String, Object>> dataDisks=new ArrayList<>()
        asc.volumes.each {ss->
          if(ss.flag==1){
            def lb = new HashMap()
            lb.diskSize = ss.volumeSize
            lb.diskType = ss.volumeType
            systemDisk.add(lb)
          }else if(ss.flag==2){
            def lb = new HashMap()
            lb.diskSize = ss.volumeSize
            lb.diskType = ss.volumeType
            dataDisks.add(lb)
          }
        }
        serverGroup.launchConfig.systemDisk = systemDisk[0]
        serverGroup.launchConfig.dataDisks=dataDisks
        List<Map<String,Object>> mazInfoList=new ArrayList<>()
        if(it.subnetList){
          it.subnetList.each {Map<String,Object> ss->
            Map<String,Object> map=new HashMap();
            ss.keySet().each {sss->
              map.azName=sss
              String[] nets=ss.get(sss)
              map.masterId=nets[0]
              map.optionId=nets.findAll {zz->zz!=map.masterId}
              mazInfoList.add(map)
            }
          }
        }
        serverGroup.mazInfoList =mazInfoList


        def getScalingPoliciesList=client.getScalingPolicies(it.groupID)
        serverGroup.cooldown=getScalingPoliciesList.size()==0?300:getScalingPoliciesList.get(0).cooldown
        serverGroup.scalingPolicies = getScalingPoliciesList.collect {
          Map<String, Object> asp = objectMapper.convertValue it, ATTRIBUTES
          return asp
        }
        def getScalingActivitesList=client.getScheduledAction(it.groupID)
        serverGroup.scheduledActions = getScalingActivitesList.collect {
          Map<String, Object> asp = objectMapper.convertValue it, ATTRIBUTES
          return asp
        }

        def instances = client.getAutoScalingInstances autoScalingGroupId
        instances.each {
          def instance = new CtyunInstance()
          instance.name = it.instanceID
          instance.instanceId=it.instanceID
          instance.id=it.id
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          instance.launchTime = it.joinDate!=null?sdf.parse(it.joinDate).time:null
          /*def instanceItem=cvmClient.getInstanceById(it.instanceID)
          instance.zone = instanceItem?.getAzName()*/
          serverGroup.instances.add(instance)
        }
        //serverGroup.asg.desiredCapacity=instances!=null?instances.size():0
        if(asg.useLb==1){
          def getLoadBalancerListByGroupId=client.getLoadBalancerListByGroupId autoScalingGroupId
          serverGroup.loadBlanders = getLoadBalancerListByGroupId.collect {
            Map<String, Object> asp = objectMapper.convertValue it, ATTRIBUTES
            return asp
          }
        }
        /*if(i==32){
          log.info("伸缩组数据处理第{}次！",i)
        }*/
        i++
        serverGroupList.add(serverGroup)
      }catch(Exception e){
        log.error("伸缩组数据处理异常！e:{}",e)
      }
    }
    return serverGroupList
  }


  @Override
  OnDemandResult handle(ProviderCache providerCache, Map<String, ? extends Object> data) {
    if (!data.containsKey("serverGroupName") || data.accountName != accountName || data.region != region) {
      return null
    }
    log.info("Enter ctyun server group agent handle " + data.serverGroupName)
    CtyunAutoScalingClient client = new CtyunAutoScalingClient(
      credentials.credentials.accessKey,
      credentials.credentials.securityKey,
      region
    )
    CtyunServerGroup serverGroup = metricsSupport.readData {
      loadAsgAsServerGroup(client, data.serverGroupName as String)[0]
    }

    if (!serverGroup) {
      return null
    }

    def cacheResult = metricsSupport.transformData {
      buildCacheResult([serverGroup], null, null)
    }

    def cacheResultAsJson = objectMapper.writeValueAsString cacheResult.cacheResults
    def serverGroupKey = Keys.getServerGroupKey(
      serverGroup?.moniker?.cluster, data.serverGroupName as String, accountName, region)

    if (cacheResult.cacheResults.values().flatten().empty) {
      // Avoid writing an empty onDemand cache record (instead delete any that may have previously existed).
      providerCache.evictDeletedItems ON_DEMAND.ns, [serverGroupKey]
    } else {
      metricsSupport.onDemandStore {
        def cacheData = new DefaultCacheData(
          serverGroupKey,
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

    Map<String, Collection<String>> evictions = serverGroup.asg ? [:] : [
      (SERVER_GROUPS.ns): [serverGroupKey]]

    return new OnDemandResult(
      sourceAgentType: getOnDemandAgentType(),
      cacheResult: cacheResult,
      evictions: evictions
    )
  }

  @Override
  boolean handles(OnDemandType type, String cloudProvider) {
    type == OnDemandType.ServerGroup && cloudProvider == CtyunCloudProvider.ID
  }

  @Override
  Collection<Map> pendingOnDemandRequests(ProviderCache providerCache) {
    def keys = providerCache.filterIdentifiers(
      ON_DEMAND.ns,
      Keys.getServerGroupKey("*", "*", credentials.name, region)
    )
    return fetchPendingOnDemandRequests(providerCache, keys)
  }

  private Collection<Map> fetchPendingOnDemandRequests(
    ProviderCache providerCache, Collection<String> keys) {
    return providerCache.getAll(ON_DEMAND.ns, keys, RelationshipCacheFilter.none()).collect {
      def details = Keys.parse(it.id)

      return [
        id            : it.id,
        details       : details,
        moniker       : convertOnDemandDetails(details),
        cacheTime     : it.attributes.cacheTime,
        cacheExpiry   : it.attributes.cacheExpiry,
        processedCount: it.attributes.processedCount,
        processedTime : it.attributes.processedTime
      ]
    }
  }
}
