package com.netflix.spinnaker.clouddriver.tencent.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.tencent.client.CloudVirtualMachineClient
import com.netflix.spinnaker.clouddriver.tencent.deploy.description.RebootTencentInstancesDescription
import com.netflix.spinnaker.clouddriver.tencent.exception.ExceptionUtils
import com.netflix.spinnaker.clouddriver.tencent.provider.view.TencentClusterProvider
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum
import org.springframework.beans.factory.annotation.Autowired

class RebootTencentInstancesAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "REBOOT_INSTANCES"

  RebootTencentInstancesDescription description

  @Autowired
  TencentClusterProvider tencentClusterProvider

  RebootTencentInstancesAtomicOperation(RebootTencentInstancesDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    def region = description.region
    def serverGroupName = description.serverGroupName
    def instanceIds = description.instanceIds

    task.updateStatus BASE_PHASE, "Initializing reboot of instances (${description.instanceIds.join(", ")}) in " +
      "$description.region:$serverGroupName..."
    try {
      def client = new CloudVirtualMachineClient(
        description.credentials.credentials.secretId,
        description.credentials.credentials.secretKey,
        region
      )
      client.rebootInstances(instanceIds)
    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.class)
      throw e
    }
    task.updateStatus BASE_PHASE, "Complete reboot of instance."
    return null
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
