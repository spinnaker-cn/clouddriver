package com.netflix.spinnaker.clouddriver.tencent.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.tencent.client.AutoScalingClient
import com.netflix.spinnaker.clouddriver.tencent.deploy.description.TerminateAndDecrementTencentServerGroupDescription
import com.netflix.spinnaker.clouddriver.tencent.exception.ExceptionUtils
import com.netflix.spinnaker.clouddriver.tencent.provider.view.TencentClusterProvider
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum
import org.springframework.beans.factory.annotation.Autowired

class TerminateAndDecrementTencentServerGroupAtomicOperation implements AtomicOperation<Void> {
  private static final String BASE_PHASE = "TERMINATE_AND_DEC_INSTANCES"

  @Autowired
  TencentClusterProvider tencentClusterProvider

  TerminateAndDecrementTencentServerGroupDescription description

  TerminateAndDecrementTencentServerGroupAtomicOperation(TerminateAndDecrementTencentServerGroupDescription description) {
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

    def asgId = tencentClusterProvider.getServerGroupAsgId(serverGroupName, accountName, region)
    try {
      def client = new AutoScalingClient(
        description.credentials.credentials.secretId,
        description.credentials.credentials.secretKey,
        region
      )
      client.removeInstances(asgId, instanceIds)
    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.class)
      throw e
    }
    task.updateStatus BASE_PHASE, "Complete terminate instance and decrease server group desired capacity."
    return null
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
