package com.netflix.spinnaker.clouddriver.ctyun.deploy.ops

import cn.ctyun.ctapi.scaling.grouplistinstance.GroupListInstance
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunAutoScalingClient
import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.deploy.DeploymentResult
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.CtyunDeployDescription
import com.netflix.spinnaker.clouddriver.ctyun.deploy.handlers.CtyunDeployHandler
import com.netflix.spinnaker.clouddriver.ctyun.exception.ExceptionUtils
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.CtyunClusterProvider
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum
import groovy.util.logging.Slf4j
import io.micrometer.core.instrument.util.StringUtils
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class CloneCtyunServerGroupAtomicOperation implements AtomicOperation<DeploymentResult> {

  private static final String BASE_PHASE = "CLONE_SERVER_GROUP"

  CtyunDeployDescription description

  @Autowired
  CtyunClusterProvider ctyunClusterProvider

  @Autowired
  CtyunDeployHandler ctyunDeployHandler

  CloneCtyunServerGroupAtomicOperation(CtyunDeployDescription description) {
    this.description = description

  }

  @Override
  DeploymentResult operate(List priorOutputs) {
    def newDescription = cloneAndOverrideDescription()
    def result;
    try {
      result = ctyunDeployHandler.handle(newDescription, priorOutputs)
    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.class)
      throw e
    }
    result
  }

  private CtyunDeployDescription cloneAndOverrideDescription() {
    def newDescription = description.clone()

    if (!description?.source?.region || !description?.source?.serverGroupName) {
      return newDescription
    }

    String sourceServerGroupName = description.source.serverGroupName
    String sourceRegion = description.source.region
    String accountName = description.accountName
    task.updateStatus BASE_PHASE, "Initializing copy of server group $sourceServerGroupName..."

    // look up source server group
    def sourceServerGroup = ctyunClusterProvider.getServerGroup(accountName, sourceRegion, sourceServerGroupName)

    if (!sourceServerGroup) {
      return newDescription
    }

    // start override source description
    newDescription.region = description.region ?: sourceRegion
    newDescription.application = description.application ?: sourceServerGroup.moniker.app
    newDescription.stack = description.stack ?: sourceServerGroup.moniker.stack
    newDescription.detail = description.detail ?: sourceServerGroup.moniker.detail

    def ctyunAutoScalingClient = new CtyunAutoScalingClient(
      description.credentials.credentials.accessKey,
      description.credentials.credentials.securityKey,
      newDescription.region
    )

    def sourceLaunchConfig = sourceServerGroup.launchConfig
    if (sourceLaunchConfig) {
      //如果从界面输入配置相关信息，会有keyid相关信息，如果都没有就直接用现有服务configid创建新伸缩组
      if(description.loginSettings==null|| description.loginSettings.keyIds==null||StringUtils.isBlank(description.loginSettings.keyIds.toString())){
        newDescription.configId=sourceLaunchConfig.configID
        newDescription.securityGroupIds = description.securityGroupIds ?: sourceLaunchConfig.securityGroupList as List
      }else{
        newDescription.instanceType = description.instanceType ?: sourceLaunchConfig.specName
        newDescription.imageId = description.imageId ?: sourceLaunchConfig.imageID
        newDescription.projectId = description.projectId ?: sourceLaunchConfig.projectIDEcs as Integer
        newDescription.systemDisk = description.systemDisk ?: sourceLaunchConfig.systemDisk as Map
        newDescription.dataDisks = description.dataDisks ?: sourceLaunchConfig.dataDisks as List
        Map<String, Object> internetAccessible=new HashMap()
        internetAccessible.publicIpAssigned=sourceLaunchConfig.useFloatings
        internetAccessible.internetChargeType=sourceLaunchConfig.billingMode
        internetAccessible.internetMaxBandwidthOut=sourceLaunchConfig.bandwidth
        newDescription.internetAccessible = description.internetAccessible ?: internetAccessible
        newDescription.loginSettings = description.loginSettings
        newDescription.securityGroupIds = description.securityGroupIds ?: sourceLaunchConfig.securityGroupList as List
        newDescription.enhancedService = description.enhancedService ?: sourceLaunchConfig.enhancedService as Map
        newDescription.userData = description.userData ?: sourceLaunchConfig.userData
        newDescription.instanceChargeType = description.instanceChargeType ?: sourceLaunchConfig.instanceChargeType
        newDescription.instanceMarketOptionsRequest = description.instanceMarketOptionsRequest ?: sourceLaunchConfig.instanceMarketOptionsRequest as Map
        newDescription.instanceTypesCheckPolicy = description.instanceTypesCheckPolicy ?: sourceLaunchConfig.instanceTypesCheckPolicy

        if (description.instanceTags) {
          newDescription.instanceTags = description.instanceTags
        } else if (sourceLaunchConfig.tags) {
          def cloneInstanceTags = []
          for (tag in sourceLaunchConfig.tags) {
            if (tag.key != CtyunAutoScalingClient.defaultServerGroupTagKey) {
              cloneInstanceTags.add(tag)
            }
          }
          newDescription.instanceTags = cloneInstanceTags
        }
      }
    }

    def sourceAutoScalingGroup = sourceServerGroup.asg
    if (sourceAutoScalingGroup) {
      newDescription.maxSize = description.maxSize ?: sourceAutoScalingGroup.maxCount as Integer
      newDescription.minSize = description.minSize ?: sourceAutoScalingGroup.minCount as Integer
      newDescription.desiredCapacity = description.desiredCapacity ?: sourceAutoScalingGroup.expectedCount as Integer

      newDescription.vpcId = description.vpcId ?: sourceAutoScalingGroup.vpcID
      //多可用区资源池的实例可用区及子网信息。mazInfo和subnetIDList 2个参数互斥，如果资源池为多可用区时使用mazInfo则不传subnetIDList 参数
      if (description.mazInfoList!=null||sourceServerGroup.mazInfoList!=null) {
        newDescription.mazInfoList = description.mazInfoList ?: sourceServerGroup.mazInfoList
      }else{
        newDescription.subnetIds = description.subnetIds ?: sourceAutoScalingGroup.subnetIDList as List
      }
      newDescription.defaultCooldown = description.defaultCooldown ?: sourceServerGroup.cooldown as Integer
      def terminationPolicie=sourceAutoScalingGroup.moveOutStrategy as Integer
      List<Integer> tempTerminationPolicies=null
      if(terminationPolicie!=null){
        tempTerminationPolicies=new ArrayList()
        tempTerminationPolicies.add(terminationPolicie)
      }
      newDescription.terminationPolicies = description.terminationPolicies ?: tempTerminationPolicies

      List<Map<String, Object>> forwardLoadBalancers=new ArrayList<>()
      List<String> loadBalancerIds=new ArrayList<>()
      if (sourceServerGroup.loadBlanders) {
        sourceServerGroup.loadBlanders.each {
          Map<String ,Object> map=new HashMap<>()
          map.port = it.port
          map.weight = it.weight
          map.lbID = it.lbID
          //TODO YJS: 负载均衡需要传主机组ID
          map.hostGroupID = it.hostGroupID
          forwardLoadBalancers.add(map)
          loadBalancerIds.add(it.lbID)
        }
      }
      newDescription.loadBalancerIds = description.loadBalancerIds ?: loadBalancerIds as List
      newDescription.forwardLoadBalancers = description.forwardLoadBalancers ?: forwardLoadBalancers

      newDescription.healthPeriod=description.healthPeriod?:sourceAutoScalingGroup.healthPeriod as Integer
      newDescription.healthMode=description.healthMode?:sourceAutoScalingGroup.healthMode as Integer
      newDescription.recoveryMode=description.recoveryMode?:sourceAutoScalingGroup.recoveryMode as Integer

      newDescription.retryPolicy = description.retryPolicy ?: sourceAutoScalingGroup.retryPolicy
      newDescription.zonesCheckPolicy = description.zonesCheckPolicy ?: sourceAutoScalingGroup.zoneCheckPolicy
    }
    newDescription
  }

    private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
