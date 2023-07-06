package com.netflix.spinnaker.clouddriver.ctyun.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunAutoScalingClient
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.DeleteCtyunScalingAlarmPolicyDescription
import com.netflix.spinnaker.clouddriver.ctyun.exception.ExceptionUtils
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum

class DeleteCtyunScalingPolicyAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "DELETE_SCALING_POLICY"

  DeleteCtyunScalingAlarmPolicyDescription description

  DeleteCtyunScalingPolicyAtomicOperation(DeleteCtyunScalingAlarmPolicyDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    def region = description.region
    def scalingPolicyId = description.scalingPolicyId
    def serverGroupName = description.serverGroupName
    def groupId = description.groupId
    try {
      task.updateStatus BASE_PHASE, "Initializing delete scaling policy $scalingPolicyId in $serverGroupName..."
      def client = new CtyunAutoScalingClient(
        description.credentials.credentials.accessKey,
        description.credentials.credentials.securityKey,
        region
      )
      client.deleteScalingPolicy(scalingPolicyId, groupId)
    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.class)
      throw e
    }
    task.updateStatus(BASE_PHASE, "Complete delete ctyun scaling alarm policy. ")
    null
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
