package com.netflix.spinnaker.clouddriver.ctyun.deploy.ops

import cn.ctyun.ctapi.CTResponse
import cn.ctyun.ctapi.scaling.rulecreate.GroupCreateRuleRequest
import cn.ctyun.ctapi.scaling.rulecreate.GroupCreateRuleRequestBody
import cn.ctyun.ctapi.scaling.rulecreate.GroupCreateRuleResponseData
import cn.ctyun.ctapi.scaling.rulecreatealarm.GroupCreateAlarmRuleRequest
import cn.ctyun.ctapi.scaling.rulecreatealarm.GroupCreateAlarmRuleRequestBody
import cn.ctyun.ctapi.scaling.rulecreatealarm.GroupCreateAlarmRuleResponseData
import cn.ctyun.ctapi.scaling.rulecreatescheduled.GroupCreateScheduledRuleRequest
import cn.ctyun.ctapi.scaling.rulecreatescheduled.GroupCreateScheduledRuleRequestBody
import cn.ctyun.ctapi.scaling.ruleupdate.RuleUpdateResponseData
import cn.ctyun.ctapi.scaling.ruleupdatealarm.RuleUpdateAlarmResponseData
import com.alibaba.fastjson.JSON
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.UpsertCtyunAlarmActionDescription
import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunAutoScalingClient
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.UpsertCtyunScheduledActionDescription
import com.netflix.spinnaker.clouddriver.ctyun.exception.ExceptionUtils
import com.netflix.spinnaker.clouddriver.ctyun.exception.CtyunOperationException
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.CtyunClusterProvider
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum
import groovy.util.logging.Slf4j
import io.github.resilience4j.core.StringUtils
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired

import java.text.SimpleDateFormat

@Slf4j
class UpsertCtyunScheduledActionAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "UPSERT_SCHEDULED_ACTIONS"

  UpsertCtyunScheduledActionDescription description

  @Autowired
  CtyunClusterProvider ctyunClusterProvider

  UpsertCtyunScheduledActionAtomicOperation(UpsertCtyunScheduledActionDescription description) {
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

    task.updateStatus BASE_PHASE, "Initializing upsert ctyun scheduled action $serverGroupName in $region..."
    def client = new CtyunAutoScalingClient(
      description.credentials.credentials.accessKey,
      description.credentials.credentials.securityKey,
      region
    )

    try {
      if (description.operationType == UpsertCtyunScheduledActionDescription.OperationType.CREATE) {
        task.updateStatus BASE_PHASE, "create ctyun scaling scheduled policy in $serverGroupName..."
        GroupCreateRuleRequestBody requestBody = new GroupCreateRuleRequestBody()
        BeanUtils.copyProperties(description, requestBody)
        requestBody.setType(description.getRuleType())
        requestBody.setName("as-policy-" + new Date().time.toString())
        requestBody.setEffectiveFrom(StringUtils.isNotEmpty(requestBody.getEffectiveFrom())?requestBody.getEffectiveFrom().replaceAll("/","-"):null)
        requestBody.setEffectiveTill(StringUtils.isNotEmpty(requestBody.getEffectiveTill())?requestBody.getEffectiveTill().replaceAll("/","-"):null)
        requestBody.setExecutionTime(StringUtils.isNotEmpty(requestBody.getExecutionTime())?requestBody.getExecutionTime().replaceAll("/","-"):null)
        if(description.getRuleType()==3){
          SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          String fromDate=requestBody.getEffectiveFrom().substring(0,10)
          Long fromTime=df.parse(requestBody.getEffectiveFrom()).getTime()
          Long fromExecutionTime=df.parse(fromDate+" "+requestBody.getExecutionTime()).getTime()
          Long theExecutionTime=fromExecutionTime//执行时间,默认今天
          //如果取开始日期+执行时间不在范围内，就取第二天的这个时间，如果还不对，时间范围有问题
          if(fromTime>theExecutionTime){
            theExecutionTime=theExecutionTime+86400000//开始时间的第二天
          }
          log.info("采用时间={}",df.format(theExecutionTime))
          requestBody.setExecutionTime(df.format(theExecutionTime))
        }

        GroupCreateRuleRequest request = new GroupCreateRuleRequest().withBody(requestBody)
        //定时和周期都用同一个创建方法
        CTResponse<GroupCreateRuleResponseData> scalingPolicy = client.createGroupRules(request)
        log.info("创建弹性定时策略返回:{}", JSON.toJSONString(scalingPolicy))
        if(scalingPolicy.getData().getStatusCode()!=800){
          throw new CtyunOperationException(scalingPolicy.getData().getDescription())
        }
        def scalingPolicyId = scalingPolicy.getData().getReturnObj().getRuleID()
        task.updateStatus BASE_PHASE, "new ctyun scaling scheduled policy $scalingPolicyId is created."
      } else if (description.operationType == UpsertCtyunScheduledActionDescription.OperationType.MODIFY) {
        Integer ruleId = description.getRuleID()
        task.updateStatus BASE_PHASE, "update scaling scheduled policy $ruleId in $serverGroupName..."
        //定时和周期都用同一个修改方法
        CTResponse<RuleUpdateResponseData> response = client.modifyScheduledAction(description)
        log.info("更新弹性伸缩组定时策略响应:{}", JSON.toJSONString(response))
      } else {
        throw new CtyunOperationException("unknown operation type, operation quit.")
      }
    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.class)
      throw e
    }
    task.updateStatus BASE_PHASE, "Complete upsert scheduled action."
    null
    return null
  }


  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
