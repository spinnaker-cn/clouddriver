package com.netflix.spinnaker.clouddriver.tencent.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.tencent.client.VirtualPrivateCloudClient
import com.netflix.spinnaker.clouddriver.tencent.deploy.description.DeleteTencentSecurityGroupDescription
import com.netflix.spinnaker.clouddriver.tencent.exception.ExceptionUtils
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum
import groovy.util.logging.Slf4j

@Slf4j
class DeleteTencentSecurityGroupAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "DELETE_SECURITY_GROUP"
  DeleteTencentSecurityGroupDescription description

  DeleteTencentSecurityGroupAtomicOperation(DeleteTencentSecurityGroupDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    task.updateStatus(BASE_PHASE, "Initializing delete of Tencent securityGroup ${description.securityGroupId} " +
      "in ${description.region}...")
    try {
      def vpcClient = new VirtualPrivateCloudClient(
        description.credentials.credentials.secretId,
        description.credentials.credentials.secretKey,
        description.region
      )
      def securityGroupId = description.securityGroupId
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
