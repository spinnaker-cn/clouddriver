package com.netflix.spinnaker.clouddriver.ctyun.provider.agent

import cn.ctyun.ctapi.scaling.grouplistinstance.GroupListInstance
import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunAutoScalingClient
import com.netflix.spinnaker.clouddriver.ctyun.client.CloudVirtualMachineClient
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunInstance
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunInstanceHealth
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.MutableCacheData
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.INFORMATIVE
import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.*

@Slf4j
@InheritConstructors
class CtyunInstanceCachingAgent extends AbstractCtyunCachingAgent {

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(INSTANCES.ns),
    INFORMATIVE.forType(SERVER_GROUPS.ns),
    INFORMATIVE.forType(CLUSTERS.ns)
  ] as Set

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    // first, find all auto scaling instances
    // second, get detail info of below instances
    log.info "start load auto scaling instance data"

    Map<String, Collection<CacheData>> cacheResults = [:]
    Map<String, Map<String, CacheData>> namespaceCache = [:].withDefault {
      namespace -> [:].withDefault { id -> new MutableCacheData(id as String) }
    }

    CtyunAutoScalingClient asClient = new CtyunAutoScalingClient(
      credentials.credentials.accessKey,
      credentials.credentials.securityKey,
      region
    )
    CloudVirtualMachineClient cvmClient = new CloudVirtualMachineClient(
      credentials.credentials.accessKey,
      credentials.credentials.securityKey,
      region
    )
    def asgInstancesMap=asClient.getAllAutoScalingInstances();
    def groupNameList=asgInstancesMap.groupNameList as List<String>;
    def asgInstances=  asgInstancesMap.groupListInstanceList as List<GroupListInstance>;
    def asgInstanceIds = asgInstances.collect {
      it.instanceID
    }
    log.info "loads ${asgInstanceIds.size()} auto scaling instances. "

    log.info "start load instances detail info."
    def result = cvmClient.getInstances asgInstanceIds
    /*
    backingup	备份中	restarting	重启中
    creating	创建中	running	运行中
    expired	已到期	starting	执行开机中
    freezing	冻结中	stopped	已关机
    rebuild	重装	stopping	执行关机中
    */
    def status=["backingup":"BACKINGUP","restarting":"RESTARTING","creating":"CREATING","running":"RUNNING"
                ,"expired":"EXPIRED","starting":"STARTING","freezing":"FREEZING","stopped":"STOPPED","rebuild":"REBUILD","stopping":"STOPPING",]
    def i=0
    result.each {
      def launchTime = CloudVirtualMachineClient.ConvertIsoDateTime it.createdTime
      def launchConfigurationName = asgInstances.find { asgIns ->
        asgIns.instanceID == it.instanceID
      }?.configName
      def securityGroupIds=it.secGroupList.collect({ sss -> sss.securityGroupID })
      def serverGroupName=groupNameList.size()>0?groupNameList.get(i++):null
      def idInteger=asgInstances.find { asgIns ->
        asgIns.instanceID == it.instanceID
      }?.id
      def ctyunInstance = new CtyunInstance(
        account: accountName,
        name: it.instanceID,
        instanceName: it.instanceName,
        instanceId: it.instanceID,
        id: idInteger,
        launchTime: launchTime ? launchTime.time : 0,
        zone: it.azName,
        vpcId: it.vpcID,
        subnetId: it.subnetIDList?:it.subnetIDList.get(0),
        privateIpAddresses: it.fixedIP,
        publicIpAddresses: it.floatingIP?.split(","),
        imageId: it.image?.imageID,
        instanceType: it.displayName,
        securityGroupIds: securityGroupIds,
        instanceHealth: new CtyunInstanceHealth(instanceStatus: status[it.instanceStatus]),
        serverGroupName: serverGroupName?:launchConfigurationName
        // if default tag is invalid, use launchConfigurationName
        // launchConfigurationName is the same with autoScalingGroupName
      )

      /*if (it.tags) {
        it.tags.each { tag->
          tencentInstance.tags.add(["key": tag.key, "value": tag.value])
        }
      }*/

      def instances = namespaceCache[INSTANCES.ns]
      def instanceKey = Keys.getInstanceKey it.instanceID, this.accountName, this.region

      instances[instanceKey].attributes.instance = ctyunInstance

      def moniker = ctyunInstance.moniker
      if (moniker) {
        def clusterKey = Keys.getClusterKey moniker.cluster, moniker.app, accountName
        def serverGroupKey = Keys.getServerGroupKey ctyunInstance.serverGroupName, accountName, region
        instances[instanceKey].relationships[CLUSTERS.ns].add clusterKey
        instances[instanceKey].relationships[SERVER_GROUPS.ns].add serverGroupKey
      }
      null
    }
    namespaceCache.each { String namespace, Map<String, CacheData> cacheDataMap ->
      cacheResults[namespace] = cacheDataMap.values()
    }

    CacheResult defaultCacheResult = new DefaultCacheResult(cacheResults)
    log.info 'finish loads instance data.'
    log.info "Caching ${namespaceCache[INSTANCES.ns].size()} items in $agentType"
    defaultCacheResult
  }
}
