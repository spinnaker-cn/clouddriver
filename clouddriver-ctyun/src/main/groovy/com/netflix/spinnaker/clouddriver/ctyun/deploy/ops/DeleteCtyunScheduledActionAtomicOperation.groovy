package com.netflix.spinnaker.clouddriver.ctyun.deploy.ops

import cn.ctyun.ctapi.CTResponse
import cn.ctyun.ctapi.scaling.ruledelete.RuleDeleteResponseData
import com.alibaba.fastjson.JSON
import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunAutoScalingClient
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.DeleteCtyunScheduledActionDescription
import com.netflix.spinnaker.clouddriver.ctyun.exception.ExceptionUtils
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum
import groovy.util.logging.Slf4j

@Slf4j
class DeleteCtyunScheduledActionAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "DELETE_SCHEDULED_ACTION"

  DeleteCtyunScheduledActionDescription description

  DeleteCtyunScheduledActionAtomicOperation(DeleteCtyunScheduledActionDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    def region = description.region
    def scalingPolicyId = description.scalingPolicyId
    def scheduledActionId=description.scheduledActionId
    def groupId = description.groupId
    task.updateStatus BASE_PHASE, "Initializing delete scheduled action $scheduledActionId in $groupId..."
    try {
      def client = new CtyunAutoScalingClient(
        description.credentials.credentials.accessKey,
        description.credentials.credentials.securityKey,
        region
      )
      CTResponse<RuleDeleteResponseData> response = client.deleteScheduledAction(scheduledActionId, groupId)
      log.info("删除伸缩组定时策略响应:{}", JSON.toJSONString(response))
    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.class)
      throw e
    }
    task.updateStatus(BASE_PHASE, "Complete delete ctyun scheduled action. ")
    return null
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
