package com.netflix.spinnaker.clouddriver.ctyun.deploy.ops

import cn.ctyun.ctapi.scaling.listinstances.ListInstance
import com.netflix.spinnaker.clouddriver.ctyun.client.CloudVirtualMachineClient
import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunAutoScalingClient
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.TerminateAndDecrementCtyunServerGroupDescription
import com.netflix.spinnaker.clouddriver.ctyun.exception.ExceptionUtils
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.CtyunClusterProvider
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum
import org.springframework.beans.factory.annotation.Autowired

class TerminateAndDecrementCtyunServerGroupAtomicOperation implements AtomicOperation<Void> {
  private static final String BASE_PHASE = "TERMINATE_AND_DEC_INSTANCES"

  @Autowired
  CtyunClusterProvider ctyunClusterProvider

  TerminateAndDecrementCtyunServerGroupDescription description

  TerminateAndDecrementCtyunServerGroupAtomicOperation(TerminateAndDecrementCtyunServerGroupDescription description) {
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

    def asgId = ctyunClusterProvider.getServerGroupAsgId(serverGroupName, accountName, region)
    try {
      def client = new CtyunAutoScalingClient(
        description.credentials.credentials.accessKey,
        description.credentials.credentials.securityKey,
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
