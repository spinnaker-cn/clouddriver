package com.netflix.spinnaker.clouddriver.hecloud.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.deploy.DeploymentResult
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.hecloud.client.HeCloudAutoScalingClient
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.HeCloudDeployDescription
import com.netflix.spinnaker.clouddriver.hecloud.deploy.handlers.HeCloudDeployHandler
import com.netflix.spinnaker.clouddriver.hecloud.provider.view.HeCloudClusterProvider
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class CloneHeCloudServerGroupAtomicOperation implements AtomicOperation<DeploymentResult> {

  private static final String BASE_PHASE = "CLONE_SERVER_GROUP"

  HeCloudDeployDescription description

  @Autowired
  HeCloudClusterProvider heCloudClusterProvider

  @Autowired
  HeCloudDeployHandler heCloudDeployHandler

  CloneHeCloudServerGroupAtomicOperation(HeCloudDeployDescription description) {
    this.description = description
  }

  @Override
  DeploymentResult operate(List priorOutputs) {
    def newDescription = cloneAndOverrideDescription()
    def result = heCloudDeployHandler.handle(newDescription, priorOutputs)
    result
  }

  private HeCloudDeployDescription cloneAndOverrideDescription() {
    def newDescription = description.clone()

    if (!description?.source?.region || !description?.source?.serverGroupName) {
      return newDescription
    }

    String sourceServerGroupName = description.source.serverGroupName
    String sourceRegion = description.source.region
    String accountName = description.accountName
    task.updateStatus BASE_PHASE, "Initializing copy of server group $sourceServerGroupName..."

    // look up source server group
    def sourceServerGroup = heCloudClusterProvider.getServerGroup(accountName, sourceRegion, sourceServerGroupName)

    if (!sourceServerGroup) {
      return newDescription
    }

    // start override source description
    newDescription.region = description.region ?: sourceRegion
    newDescription.application = description.application ?: sourceServerGroup.moniker.app
    newDescription.stack = description.stack ?: sourceServerGroup.moniker.stack
    newDescription.detail = description.detail ?: sourceServerGroup.moniker.detail

    def sourceLaunchConfig = sourceServerGroup.launchConfig
    if (sourceLaunchConfig) {
      newDescription.instanceType = description.instanceType ?: sourceLaunchConfig.instanceType
      newDescription.imageId = description.imageId ?: sourceLaunchConfig.imageId
      newDescription.systemDisk = description.systemDisk ?: sourceLaunchConfig.systemDisk as Map
      newDescription.dataDisks = description.dataDisks ?: sourceLaunchConfig.dataDisks as List
      newDescription.internetAccessible = description.internetAccessible ?: sourceLaunchConfig.internetAccessible as Map
      newDescription.securityGroupIds = description.securityGroupIds ?: sourceLaunchConfig.securityGroupIds as List
      newDescription.userData = description.userData ?: sourceLaunchConfig.userData
      newDescription.keyPair = description.keyPair ?: sourceLaunchConfig.keyPair


      if (description.instanceTags) {
        newDescription.instanceTags = description.instanceTags
      } else if (sourceLaunchConfig.instanceTags) {
        def cloneInstanceTags = []
        for (tag in sourceLaunchConfig.instanceTags) {
          if (tag.key != HeCloudAutoScalingClient.defaultServerGroupTagKey) {
            cloneInstanceTags.add(tag)
          }
        }
        newDescription.instanceTags = cloneInstanceTags
      }
    }

    def sourceAutoScalingGroup = sourceServerGroup.asg
    if (sourceAutoScalingGroup) {
      newDescription.maxSize = description.maxSize ?: sourceAutoScalingGroup.maxSize as Integer
      newDescription.minSize = description.minSize ?: sourceAutoScalingGroup.minSize as Integer
      newDescription.desiredCapacity = description.desiredCapacity ?: sourceAutoScalingGroup.desiredCapacity as Integer
      newDescription.vpcId = description.vpcId ?: sourceAutoScalingGroup.vpcId

      if (newDescription.vpcId) {
        newDescription.subnetIds = description.subnetIds ?: sourceAutoScalingGroup.subnetIdSet as List
      } else {
        newDescription.zones = description.zones ?: sourceAutoScalingGroup.zoneSet as List
      }

      newDescription.defaultCooldown = description.defaultCooldown ?: sourceAutoScalingGroup.defaultCooldown as Integer
      newDescription.terminationPolicies = description.terminationPolicies ?: sourceAutoScalingGroup.terminationPolicies as List
      newDescription.forwardLoadBalancers = description.forwardLoadBalancers ?: sourceAutoScalingGroup.forwardLoadBalancerSet as List
    }
    newDescription
  }

    private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
