package com.netflix.spinnaker.clouddriver.ctyun.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunAutoScalingClient
import com.netflix.spinnaker.clouddriver.ctyun.client.CloudVirtualMachineClient
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.DestroyCtyunServerGroupDescription
import com.netflix.spinnaker.clouddriver.ctyun.exception.ExceptionUtils
import com.netflix.spinnaker.clouddriver.ctyun.exception.CtyunOperationException
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunInstance
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.CtyunClusterProvider
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class DestroyCtyunServerGroupAtomicOperation implements AtomicOperation<Void> {
  private static final String BASE_PHASE = "DESTROY_SERVER_GROUP"

  DestroyCtyunServerGroupDescription description

  @Autowired
  CtyunClusterProvider ctyunClusterProvider

  DestroyCtyunServerGroupAtomicOperation(DestroyCtyunServerGroupDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    // 1. detach all instances from asg
    // 2. terminate detached instances
    // 3. delete asg
    // 4. delete asc
    task.updateStatus BASE_PHASE, "Initializing destroy server group $description.serverGroupName in " +
      "$description.region..."
    def region = description.region
    def accountName = description.accountName
    def serverGroupName = description.serverGroupName
    try {
      def client = new CtyunAutoScalingClient(
        description.credentials.credentials.accessKey,
        description.credentials.credentials.securityKey,
        region
      )

      /*def cvmClient = new CloudVirtualMachineClient(
        description.credentials.credentials.accessKey,
        description.credentials.credentials.securityKey,
        region
      )*/

      task.updateStatus(BASE_PHASE, "Start destroy server group $serverGroupName")
      def serverGroup = ctyunClusterProvider.getServerGroup(accountName, region, serverGroupName, true)

      if (serverGroup) {
        Integer asgId = serverGroup.asg.groupID
        Integer ascId = serverGroup.asg.configID
        /*Set<CtyunInstance> instances = serverGroup.instances
        def instanceIds = instances.collect {
          it.name
        }*/

        task.updateStatus(BASE_PHASE, "Server group $serverGroupName is related to " +
          "auto scaling group $asgId and launch configuration $ascId.")

        /*if (instanceIds) {
          //Integer maxQueryTime = 10000
          task.updateStatus(BASE_PHASE, "Will detach $instanceIds from $asgId")
          client.detachInstances(asgId, instanceIds)
          task.updateStatus(BASE_PHASE, "Detach activity has finished, will start terminate soon.")
          cvmClient.terminateInstances(instanceIds)
          task.updateStatus(BASE_PHASE, "$instanceIds are terminaing.")
        }*/
        //目前天翼云删除伸缩组具有释放云主机的效果，因此以上释放和停止主机操作取消
        task.updateStatus(BASE_PHASE, "Deleting auto scaling group $asgId...")
        client.deleteAutoScalingGroup(asgId)
        task.updateStatus(BASE_PHASE, "Auto scaling group $asgId is deleted.")

        /*配置文件是公用的，如果删除发生异常，不删除，可能被其他伸缩组使用中*/
        task.updateStatus(BASE_PHASE, "Deleting launch configuration $ascId...")
        try{
          log.info("DestroyCtyunServerGroupAtomicOperation--删除配置--start")
          client.deleteLaunchConfiguration(ascId)
          log.info("DestroyCtyunServerGroupAtomicOperation--删除配置--end")
        }catch(Exception e){
          log.error("DestroyCtyunServerGroupAtomicOperation--删除配置--Exception")
        }

        task.updateStatus(BASE_PHASE, "Launch configuration $ascId is deleted.")

        task.updateStatus(BASE_PHASE, "Complete destroy server group $serverGroupName.")
      } else {
        task.updateStatus(BASE_PHASE, "Server group $serverGroupName is not found.")
      }
    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.class)
      throw e
    }
    task.updateStatus(BASE_PHASE, "Complete destroy server group. ")
    null
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
