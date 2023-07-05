package com.netflix.spinnaker.clouddriver.ctyun.deploy.ops

import cn.ctyun.ctapi.scaling.rulecreatescheduled.GroupCreateScheduledRuleRequest
import cn.ctyun.ctapi.scaling.rulecreatescheduled.GroupCreateScheduledRuleRequestBody
import cn.ctyun.ctapi.scaling.ruleexecute.RuleExecuteRequest
import cn.ctyun.ctapi.scaling.ruleexecute.RuleExecuteRequestBody
import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunAutoScalingClient
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.ResizeCtyunServerGroupDescription
import com.netflix.spinnaker.clouddriver.ctyun.exception.ExceptionUtils
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.CtyunClusterProvider
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import java.text.SimpleDateFormat
@Slf4j
class ResizeCtyunServerGroupAtomicOperation implements AtomicOperation<Void> {
  private static final String BASE_PHASE = "RESIZE_SERVER_GROUP"

  @Autowired
  CtyunClusterProvider ctyunClusterProvider

  private final ResizeCtyunServerGroupDescription description

  ResizeCtyunServerGroupAtomicOperation(ResizeCtyunServerGroupDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    task.updateStatus BASE_PHASE, "Initializing resize of server group $description.serverGroupName in " +
      "$description.region..."
    def accountName = description.accountName
    //def credentials = description.credentials
    def region = description.region
    def serverGroupName = description.serverGroupName
    def asgId = ctyunClusterProvider.getServerGroupAsgId(serverGroupName, accountName, region)
    try {
      def client = new CtyunAutoScalingClient(
        description.credentials.credentials.accessKey,
        description.credentials.credentials.securityKey,
        region
      )
      //修改伸缩最大最小值
      Integer groupId=client.resizeAutoScalingGroup(asgId, description.capacity)
     /*因系统已经增加了期望值功能，因此下面功能注释掉
      //获取实例数
      def groupListInstanceList=client.getAutoScalingInstances(groupId)

      def groupInstanceSize=groupListInstanceList==null?0:groupListInstanceList.size()
      if(groupId!=null){
        //创建一个定时策略，执行一次
        GroupCreateScheduledRuleRequestBody requestBody = new GroupCreateScheduledRuleRequestBody().with {
          it.regionID = region
          it.groupID = groupId
          it.name = "as-policy-"+new Date().getTime()
          SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          //创建明天的定时任务
          Long times=new Date().getTime()+86400000
          it.executionTime =df.format(times)
          it.action = 3
          it.operateUnit = 1
          //如果最小值大于当前实例数，说明要按照最小值增加，所以不需要自己手动伸缩，相反最大值小于当前值，不需要自己手动减少
          if(description.capacity.min>groupInstanceSize||description.capacity.max<groupInstanceSize){
            it.operateCount=0
          }else {
            //期望值减掉现有值
            it.operateCount = description.capacity.desired-groupInstanceSize
            //如果期望值大于现有实例数，需要增加，反之减少
            if(it.operateCount>0){
              it.action = 1
            }else if(it.operateCount<0){
              it.action = 2
              it.operateCount=groupInstanceSize-description.capacity.desired
            }
          }
          it
        }
        //如果期望值与实际实例数不一致,且由最小值最大值自己控制，就不需要自己手动执行伸策略
        if(requestBody.operateCount!=0){
          log.info("resize operateCount={}",requestBody.operateCount)
          GroupCreateScheduledRuleRequest request = new GroupCreateScheduledRuleRequest().withBody(requestBody)
          Integer ruleId= client.createGroupScheduledRules(request)
          if(ruleId!=null){
            RuleExecuteRequestBody ruleExecuteRequestBody=new RuleExecuteRequestBody().with{
              it.regionID = region
              it.groupID=groupId
              it.ruleID=ruleId
              it.executionMode=2
              it
            }
            RuleExecuteRequest ruleExecuteRequest=new RuleExecuteRequest().withBody(ruleExecuteRequestBody)
            try{
              Integer ruleId2= client.ruleExecute(ruleExecuteRequest)
            }catch(Exception e){
              log.error("手动执行定时任务失败！",e)
            }
            //不管执行成没成功，都删除掉该定时任务
            client.deleteScheduledAction(ruleId,groupId)
          }
        }else{
          log.info("resize operateCount=0,不创建伸缩组")
        }
      }*/
    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.class)
      throw e
    }
    task.updateStatus BASE_PHASE, "Complete resize of server group $description.serverGroupName in " +
      "$description.region."
    null
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
