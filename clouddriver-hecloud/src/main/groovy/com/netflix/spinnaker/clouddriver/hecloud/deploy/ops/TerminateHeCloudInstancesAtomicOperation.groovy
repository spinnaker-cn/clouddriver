package com.netflix.spinnaker.clouddriver.hecloud.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.hecloud.client.HeCloudElasticCloudServerClient
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.TerminateHeCloudInstancesDescription
import com.netflix.spinnaker.clouddriver.hecloud.provider.view.HeCloudClusterProvider
import org.springframework.beans.factory.annotation.Autowired

class TerminateHeCloudInstancesAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "TERMINATE_INSTANCES"

  @Autowired
  HeCloudClusterProvider heCloudClusterProvider

  TerminateHeCloudInstancesDescription description

  TerminateHeCloudInstancesAtomicOperation(TerminateHeCloudInstancesDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    def region = description.region
    def serverGroupName = description.serverGroupName
    def instanceIds = description.instanceIds
    def accountName = description.accountName

    task.updateStatus BASE_PHASE, "Initializing termination of instances (${description.instanceIds.join(", ")}) in " +
      "$description.region:$serverGroupName..."

    def client = new HeCloudElasticCloudServerClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      region,
      description.accountName
    )
    client.terminateInstances(instanceIds)

    task.updateStatus BASE_PHASE, "Complete termination of instance."
    return null
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
