package com.netflix.spinnaker.clouddriver.ctyun.deploy.ops

import cn.ctyun.ctapi.CTResponse
import cn.ctyun.ctapi.scaling.rulecreatealarm.AlarmTriggerInfo
import cn.ctyun.ctapi.scaling.rulecreatealarm.GroupCreateAlarmRuleRequest
import cn.ctyun.ctapi.scaling.rulecreatealarm.GroupCreateAlarmRuleRequestBody
import cn.ctyun.ctapi.scaling.rulecreatealarm.GroupCreateAlarmRuleResponseData
import cn.ctyun.ctapi.scaling.ruleupdate.RuleUpdateResponseData
import cn.ctyun.ctapi.scaling.ruleupdate.TriggerInfo
import cn.ctyun.ctapi.scaling.ruleupdatealarm.RuleUpdateAlarmResponseData
import com.alibaba.fastjson.JSON
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.UpsertCtyunAlarmActionDescription
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.UpsertCtyunScheduledActionDescription
import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunAutoScalingClient

import com.netflix.spinnaker.clouddriver.ctyun.exception.ExceptionUtils
import com.netflix.spinnaker.clouddriver.ctyun.exception.CtyunOperationException
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.CtyunClusterProvider
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum
import groovy.util.logging.Slf4j
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class UpsertCtyunScalingAlarmPolicyAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "UPSERT_SCALING_POLICY"

  UpsertCtyunAlarmActionDescription description

  @Autowired
  CtyunClusterProvider ctyunClusterProvider

  UpsertCtyunScalingAlarmPolicyAtomicOperation(UpsertCtyunAlarmActionDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    def region = description.regionID
    def serverGroupName = description.serverGroupName
    def accountName = description.accountName
    def asgId = ctyunClusterProvider.getServerGroupAsgId(serverGroupName, accountName, region)

    if (!asgId) {
      throw new CtyunOperationException("ASG of $serverGroupName is not found.")
    }

    task.updateStatus BASE_PHASE, "Initializing upsert scaling policy $serverGroupName in $region..."

    def client = new CtyunAutoScalingClient(
      description.credentials.credentials.accessKey,
      description.credentials.credentials.securityKey,
      region
    )
    try {
      if (description.operationType == UpsertCtyunAlarmActionDescription.OperationType.CREATE) {
        task.updateStatus BASE_PHASE, "create scaling policy in $serverGroupName..."
        GroupCreateAlarmRuleRequestBody requestBody = new GroupCreateAlarmRuleRequestBody()
        BeanUtils.copyProperties(description, requestBody)
        requestBody.setName("as-policy-" + new Date().time.toString())
        AlarmTriggerInfo triggerInfo=description.triggerObj
        triggerInfo.setName("as-alarm-" + new Date().time.toString())
        requestBody.setTriggerObj(triggerInfo)
        GroupCreateAlarmRuleRequest request = new GroupCreateAlarmRuleRequest().withBody(requestBody)
        CTResponse<GroupCreateAlarmRuleResponseData> scalingPolicy = client.createGroupAlarmRules(request)
        log.info("创建弹性告警策略返回:{}", JSON.toJSONString(scalingPolicy))
        def scalingPolicyId = scalingPolicy.getData().getReturnObj().getRuleID()
        task.updateStatus BASE_PHASE, "new ctyun scaling policy $scalingPolicyId is created."
      } else if (description.operationType == UpsertCtyunAlarmActionDescription.OperationType.MODIFY) {
        Integer ruleId = description.getRuleID()
        task.updateStatus BASE_PHASE, "update scaling policy $ruleId in $serverGroupName..."
        client.modifyAlarmAction(description)
      } else {
        throw new CtyunOperationException("unknown operation type, operation quit.")
      }
    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.class)
      throw e
    }
    task.updateStatus BASE_PHASE, "Complete ctyun upsert scaling policy."
    null
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
