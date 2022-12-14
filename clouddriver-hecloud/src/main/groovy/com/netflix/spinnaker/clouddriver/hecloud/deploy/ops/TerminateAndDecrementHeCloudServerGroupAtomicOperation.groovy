package com.netflix.spinnaker.clouddriver.hecloud.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.hecloud.client.HeCloudAutoScalingClient
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.TerminateAndDecrementHeCloudServerGroupDescription
import com.netflix.spinnaker.clouddriver.hecloud.provider.view.HeCloudClusterProvider
import org.springframework.beans.factory.annotation.Autowired

class TerminateAndDecrementHeCloudServerGroupAtomicOperation implements AtomicOperation<Void> {
  private static final String BASE_PHASE = "TERMINATE_AND_DEC_INSTANCES"

  @Autowired
  HeCloudClusterProvider heCloudClusterProvider

  TerminateAndDecrementHeCloudServerGroupDescription description

  TerminateAndDecrementHeCloudServerGroupAtomicOperation(TerminateAndDecrementHeCloudServerGroupDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    def region = description.region
    def serverGroupName = description.serverGroupName
    def instanceIds = [description.instance]
    def accountName = description.credentials.name

    task.updateStatus BASE_PHASE, "Initializing termination of instance (${description.instance}) in " +
      "$description.region:$serverGroupName and decrease server group desired capacity..."

    def asgId = heCloudClusterProvider.getServerGroupAsgId(serverGroupName, accountName, region)
    def client = new HeCloudAutoScalingClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      region,
      description.account
    )
    client.removeInstances(asgId, instanceIds)
    task.updateStatus BASE_PHASE, "Complete terminate instance and decrease server group desired capacity."
    return null
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
