package com.netflix.spinnaker.clouddriver.ctyun.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.ctyun.client.VirtualPrivateCloudClient
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.DeleteCtyunSecurityGroupDescription
import com.netflix.spinnaker.clouddriver.ctyun.exception.ExceptionUtils
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum
import groovy.util.logging.Slf4j

@Slf4j
class DeleteCtyunSecurityGroupAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "DELETE_SECURITY_GROUP"
  DeleteCtyunSecurityGroupDescription description

  DeleteCtyunSecurityGroupAtomicOperation(DeleteCtyunSecurityGroupDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    task.updateStatus(BASE_PHASE, "Initializing delete of Ctyun securityGroup ${description.securityGroupId} " +
      "in ${description.region}...")
    def securityGroupId=""
    try {
      def vpcClient = new VirtualPrivateCloudClient(
        description.credentials.credentials.getAccessKey(),
        description.credentials.credentials.getSecurityKey(),
        description.region
      )
      securityGroupId = description.securityGroupId
      task.updateStatus(BASE_PHASE, "Start delete securityGroup ${securityGroupId} ...")
      vpcClient.deleteSecurityGroup(securityGroupId)
    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.class)
      throw e
    }
    task.updateStatus(BASE_PHASE, "Delete securityGroup ${securityGroupId} end")

    return null
  }


  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
