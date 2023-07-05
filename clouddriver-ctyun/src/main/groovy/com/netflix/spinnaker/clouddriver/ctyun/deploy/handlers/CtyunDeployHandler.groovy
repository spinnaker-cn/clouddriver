package com.netflix.spinnaker.clouddriver.ctyun.deploy.handlers

import cn.ctyun.ctapi.CTResponse
import cn.ctyun.ctapi.scaling.rulecreatealarm.AlarmTriggerInfo
import cn.ctyun.ctapi.scaling.rulecreatealarm.GroupCreateAlarmRuleRequest
import cn.ctyun.ctapi.scaling.rulecreatealarm.GroupCreateAlarmRuleRequestBody
import cn.ctyun.ctapi.scaling.rulecreatecycle.GroupCreateCycleRuleRequest
import cn.ctyun.ctapi.scaling.rulecreatecycle.GroupCreateCycleRuleRequestBody
import cn.ctyun.ctapi.scaling.rulecreatecycle.GroupCreateCycleRuleResponseData
import cn.ctyun.ctapi.scaling.rulecreatescheduled.GroupCreateScheduledRuleRequest
import cn.ctyun.ctapi.scaling.rulecreatescheduled.GroupCreateScheduledRuleRequestBody
import cn.ctyun.ctapi.scaling.rulecreatescheduled.GroupCreateScheduledRuleResponseData
import cn.ctyun.ctapi.scaling.rulelist.RuleInfo
import com.alibaba.fastjson.JSON
import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.deploy.DeployDescription
import com.netflix.spinnaker.clouddriver.deploy.DeployHandler
import com.netflix.spinnaker.clouddriver.deploy.DeploymentResult
import com.netflix.spinnaker.clouddriver.ctyun.deploy.CtyunServerGroupNameResolver
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.CtyunDeployDescription
import com.netflix.spinnaker.clouddriver.ctyun.exception.CtyunOperationException
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.CtyunClusterProvider
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunAutoScalingClient
import groovy.util.logging.Slf4j
import io.github.resilience4j.core.StringUtils
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.text.SimpleDateFormat

/*
curl -X POST \
  http://localhost:7002/Ctyun/ops \
  -H 'Content-Type: application/json' \
  -H 'Postman-Token: 16583564-d31a-442f-bb17-0a308ee2c529' \
  -H 'cache-control: no-cache' \
  -d '[{"createServerGroup":{"application":"myapp","stack":"dev","accountName":"test","imageId":"img-oikl1tzv","instanceType":"S2.SMALL2","zones":["ap-guangzhou-2"],"credentials":"my-account-name","maxSize":0,"minSize":0,"desiredCapacity":0,"vpcId":"","region":"ap-guangzhou","dataDisks":[{"diskType":"CLOUD_PREMIUM","diskSize":50}],"systemDisk":{"diskType":"CLOUD_PREMIUM","diskSize":50}}}]'
*/


@Component
@Slf4j
class CtyunDeployHandler implements DeployHandler<CtyunDeployDescription> {
  private static final String BASE_PHASE = "DEPLOY"

  @Autowired
  private CtyunClusterProvider ctyunClusterProvider

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }

  @Override
  boolean handles(DeployDescription description) {
    description instanceof CtyunDeployDescription
  }

  @Override
  DeploymentResult handle(CtyunDeployDescription description, List priorOutputs) {
    task.updateStatus BASE_PHASE, "Initializing deployment to ${description.zones}"

    def accountName = description.accountName
    def region = description.region
    def serverGroupNameResolver = new CtyunServerGroupNameResolver(
      accountName, region, ctyunClusterProvider, description.credentials)

    task.updateStatus BASE_PHASE, "Looking up next sequence..."

    def serverGroupName = serverGroupNameResolver.resolveNextServerGroupName(description.application, description.stack, description.detail, false)

    task.updateStatus BASE_PHASE, "Produced server group name: $serverGroupName"

    description.serverGroupName = serverGroupName

    CtyunAutoScalingClient autoScalingClient = new CtyunAutoScalingClient(
      description.credentials.credentials.accessKey,
      description.credentials.credentials.securityKey,
      region
    )

    if (description?.source?.useSourceCapacity) {
      log.info('copy ctyun source server group capacity')
      String sourceServerGroupName = description?.source?.serverGroupName
      String sourceRegion = description?.source?.region
      def sourceServerGroup = ctyunClusterProvider.getServerGroup(accountName, sourceRegion, sourceServerGroupName)
      if (!sourceServerGroup) {
        log.warn("source server group $sourceServerGroupName is not found")
      } else {
        description.desiredCapacity = sourceServerGroup.asg.expectedCount as Integer
        description.maxSize = sourceServerGroup.asg.maxCount as Integer
        description.minSize = sourceServerGroup.asg.minCount as Integer
      }
    }

    task.updateStatus BASE_PHASE, "Composing server group $serverGroupName..."

    def groupId = autoScalingClient.deploy(description)

    task.updateStatus BASE_PHASE, "Done creating server group $serverGroupName in $region."


    CtyunDeploymentResult ctyunDeploymentResult = new CtyunDeploymentResult()
    ctyunDeploymentResult.serverGroupNames = ["$region:$serverGroupName".toString()]
    ctyunDeploymentResult.serverGroupIdByRegion[region] = groupId
    ctyunDeploymentResult.serverGroupNameByRegion[region] = serverGroupName

    if (description.copySourceScalingPoliciesAndActions) {
      copyScalingPolicyAndScheduledActionAndLifecycleHooks(description, ctyunDeploymentResult)
      //copyNotification(description, ctyunDeploymentResult)  // copy notification by the way
    }

    return ctyunDeploymentResult
  }

 /* private def copyNotification(CtyunDeployDescription description, DeploymentResult deployResult) {
    task.updateStatus BASE_PHASE, "Enter copyNotification."
    String sourceServerGroupName = description?.source?.serverGroupName
    String sourceRegion = description?.source?.region
    String accountName = description?.accountName
    def sourceServerGroup = ctyunClusterProvider.getServerGroup(accountName, sourceRegion, sourceServerGroupName)

    if (!sourceServerGroup) {
      log.warn("source server group not found, account $accountName, region $sourceRegion, source sg name $sourceServerGroupName")
      return
    }

    String sourceAsgId = sourceServerGroup.asg.autoScalingGroupId

    task.updateStatus BASE_PHASE, "Initializing copy notification from $sourceAsgId."

    CtyunAutoScalingClient autoScalingClient = new CtyunAutoScalingClient(
      description.credentials.credentials.accessKey,
      description.credentials.credentials.securityKey,
      sourceRegion
    )

    String newServerGroupName = deployResult.serverGroupNameByRegion[sourceRegion]
    def newAsg = autoScalingClient.getAutoScalingGroupsByName(newServerGroupName)[0]
    String newAsgId = newAsg.groupID

    def notifications = autoScalingClient.getNotification(sourceAsgId)
    for (notification in notifications) {
      try {
        autoScalingClient.createNotification(newAsgId, notification)
      } catch (CtyunOperationException toe) {
        // something bad happened during creation, log the error and continue
        log.warn "create notification error $toe"
      }
    }
  }*/


  private def copyScalingPolicyAndScheduledActionAndLifecycleHooks(CtyunDeployDescription description, CtyunDeploymentResult ctyunDeployResult) {
    task.updateStatus BASE_PHASE, "Enter copyScalingPolicyAndScheduledActionAndLifecycleHooks."

    String sourceServerGroupName = description?.source?.serverGroupName
    String sourceRegion = description?.source?.region
    String accountName = description?.accountName
    def sourceServerGroup = ctyunClusterProvider.getServerGroup(accountName, sourceRegion, sourceServerGroupName)

    if (!sourceServerGroup) {
      log.warn("description is $description")
      log.warn("source server group not found, account $accountName, region $sourceRegion, source sg name $sourceServerGroupName")
      return
    }

    Integer sourceAsgId = sourceServerGroup.asg.groupID

    task.updateStatus BASE_PHASE, "Initializing copy scaling policy and scheduled action from $sourceAsgId."

    CtyunAutoScalingClient ctyunAutoScalingClient = new CtyunAutoScalingClient(
      description.credentials.credentials.accessKey,
      description.credentials.credentials.securityKey,
      sourceRegion
    )

    Integer newServerGroupId = ctyunDeployResult.serverGroupIdByRegion[sourceRegion]
    def newAsg = ctyunAutoScalingClient.getAutoScalingGroupByGroupId(newServerGroupId)
    //String newAsgId = newAsg.getGroupID()

    //填充serverGroupNames和serverGroupIdByRegion
    String region = description.region
    String groupName = newAsg.getName()
    ctyunDeployResult.serverGroupNames = ["$region:$groupName".toString()]
    ctyunDeployResult.serverGroupIdByRegion[region] = newServerGroupId

    // copy all scaling policies
    List<RuleInfo> alarmPolicies = ctyunAutoScalingClient.getScalingPolicies(sourceAsgId)

    List<RuleInfo> scheduledAction = ctyunAutoScalingClient.getScheduledAction(sourceAsgId)
    List<RuleInfo> scheduledPolicies = scheduledAction?.findAll({ policy ->
       policy.getRuleType() == 2
    })
    List<RuleInfo> cyclePolicies = scheduledAction?.findAll({ policy ->
       policy.getRuleType() == 3
    })
    if(null == scheduledPolicies){
      scheduledPolicies = new ArrayList<RuleInfo>()
    }
    if(null == cyclePolicies){
      cyclePolicies = new ArrayList<RuleInfo>()
    }
    if(null == alarmPolicies){
      alarmPolicies = new ArrayList<>()
    }

    // copy all alarm rules
    for (scalingPolicy in alarmPolicies) {
      try {
        GroupCreateAlarmRuleRequestBody requestBody = new GroupCreateAlarmRuleRequestBody().with {
          it.regionID = sourceRegion
          it.groupID = newServerGroupId
          it.name = scalingPolicy.getName()
          it.cooldown = scalingPolicy.getCooldown()
          it.action = scalingPolicy.getAction()
          it.operateUnit = scalingPolicy.getOperateUnit()
          it.operateCount = scalingPolicy.getOperateCount()
          AlarmTriggerInfo alarmTriggerInfo = new AlarmTriggerInfo()
          alarmTriggerInfo.name=scalingPolicy.getTriggerObj().getName()
          alarmTriggerInfo.metricName=scalingPolicy.getTriggerObj().getMetricName()
          alarmTriggerInfo.statistics=scalingPolicy.getTriggerObj().getStatistics()
          alarmTriggerInfo.comparisonOperator=scalingPolicy.getTriggerObj().getComparisonOperator()
          alarmTriggerInfo.threshold=Integer.parseInt(scalingPolicy.getTriggerObj().getThreshold())
          alarmTriggerInfo.period= StringUtils.isNotEmpty(scalingPolicy.getTriggerObj().getPeriod())?scalingPolicy.getTriggerObj().getPeriod():"1m"
          alarmTriggerInfo.evaluationCount=Integer.parseInt(scalingPolicy.getTriggerObj().getEvaluationCount())
          it.triggerObj = alarmTriggerInfo
          it
        }
        GroupCreateAlarmRuleRequest request = new GroupCreateAlarmRuleRequest().withBody(requestBody)
        CTResponse<GroupCreateScheduledRuleResponseData> response = ctyunAutoScalingClient.createGroupAlarmRules(request)
        log.info("copyScalingPolicyAndScheduledActionAndLifecycleHooks, createGroupAlarmRules response:{}", JSON.toJSONString(response))
      } catch (Exception e) {
        // something bad happened during creation, log the error and continue
        log.warn "create scaling alarm policy error $e"
      }
    }

    // copy all scheduled rules
    for (scalingPolicy in scheduledPolicies) {
      try {
        GroupCreateScheduledRuleRequestBody requestBody = new GroupCreateScheduledRuleRequestBody().with {
          it.regionID = sourceRegion
          it.groupID = newServerGroupId
          it.name = scalingPolicy.getName()
          //it.executionTime = scalingPolicy.getExecutionTime()
          SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          Long times=new Date().getTime()+300000
          it.executionTime =df.format(times)
          it.action = scalingPolicy.getAction()
          it.operateUnit = scalingPolicy.getOperateUnit()
          it.operateCount = scalingPolicy.getOperateCount()
          it
        }
        GroupCreateScheduledRuleRequest request = new GroupCreateScheduledRuleRequest().withBody(requestBody)
        CTResponse<GroupCreateScheduledRuleResponseData> response = ctyunAutoScalingClient.createGroupScheduledRules(request)
        log.info("copyScalingPolicyAndScheduledActionAndLifecycleHooks, createGroupScheduledRules response:{}", JSON.toJSONString(response))
      } catch (Exception e) {
        // something bad happened during creation, log the error and continue
        log.warn "create scaling scheduled policy error $e"
      }
    }

    // copy all cycle actions
    for (scalingPolicy in cyclePolicies) {
      try{
        GroupCreateCycleRuleRequestBody requestBody = new GroupCreateCycleRuleRequestBody().with {
          it.regionID = sourceRegion
          it.groupID = newServerGroupId
          it.name = scalingPolicy.getName()
          it.cycle = scalingPolicy.getCycle()
          it.day = scalingPolicy.getDay()




          SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          Long fromTime=df.parse(scalingPolicy.getEffectiveFrom()).getTime()
          if(fromTime<new Date().getTime()){
            fromTime=new Date().getTime()
          }
          it.effectiveFrom = df.format(fromTime)
          it.effectiveTill = scalingPolicy.getEffectiveTill()

          String fromDate=scalingPolicy.getEffectiveFrom().substring(0,10)
          String execTime=scalingPolicy.getExecutionTime().substring(11)
          Long fromExecutionTime=df.parse(fromDate+" "+execTime).getTime()
          Long theExecutionTime=fromExecutionTime//执行时间,默认今天
          //如果取开始日期+执行时间不在范围内，就取第二天的这个时间，如果还不对，时间范围有问题
          if(fromTime>theExecutionTime){
            theExecutionTime=theExecutionTime+86400000//开始时间的第二天
          }
          log.info("采用时间={}",df.format(theExecutionTime))
          it.executionTime = df.format(theExecutionTime)

          it.action = scalingPolicy.getAction()
          it.operateUnit = scalingPolicy.getOperateUnit()
          it.operateCount = scalingPolicy.getOperateCount()
          it
        }
        GroupCreateCycleRuleRequest request = new GroupCreateCycleRuleRequest().withBody(requestBody)
        CTResponse<GroupCreateCycleRuleResponseData> response = ctyunAutoScalingClient.createGroupCycleRules(request)
        log.info("copyScalingPolicyAndScheduledActionAndLifecycleHooks, createGroupCycleRules response:{}", JSON.toJSONString(response))
      } catch (Exception e) {
        // something bad happened during creation, log the error and continue
        log.warn "create scaling cycle policy error $e"
      }
    }
  }
}
