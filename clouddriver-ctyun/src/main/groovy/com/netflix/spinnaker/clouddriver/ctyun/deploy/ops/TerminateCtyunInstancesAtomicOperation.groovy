package com.netflix.spinnaker.clouddriver.ctyun.deploy.ops

import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunAutoScalingClient
import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.ctyun.client.CloudVirtualMachineClient
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.TerminateCtyunInstancesDescription
import com.netflix.spinnaker.clouddriver.ctyun.exception.ExceptionUtils
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.CtyunClusterProvider
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum
import org.springframework.beans.factory.annotation.Autowired

class TerminateCtyunInstancesAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "TERMINATE_INSTANCES"

  @Autowired
  CtyunClusterProvider ctyunClusterProvider

  TerminateCtyunInstancesDescription description

  TerminateCtyunInstancesAtomicOperation(TerminateCtyunInstancesDescription description) {
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
    def asgId = ctyunClusterProvider.getServerGroupAsgId(serverGroupName, accountName, region)
    try {
      def client = new CloudVirtualMachineClient(
        description.credentials.credentials.accessKey,
        description.credentials.credentials.securityKey,
        region
      )
      def scalingClient = new CtyunAutoScalingClient(
        description.credentials.credentials.accessKey,
        description.credentials.credentials.securityKey,
        region
      )
     //本参数表示保护状态。
      //取值范围：
      //1：已保护。
      //2：未保护。 设置成保护状态防止被自动清楚

     // scalingClient.setInstancesProtection(instanceIds,asgId,1)

      client.terminateInstances(instanceIds)

    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.class)
      throw e
    }
    task.updateStatus BASE_PHASE, "Complete termination of instance."
    return null
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
