package com.netflix.spinnaker.clouddriver.ctyun.deploy.ops

import com.netflix.spinnaker.clouddriver.ctyun.client.CloudVirtualMachineClient
import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunAutoScalingClient
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.EnableDisableCtyunServerGroupDescription
import com.netflix.spinnaker.clouddriver.ctyun.exception.ExceptionUtils
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum
import groovy.transform.Canonical

abstract class AbstractEnableDisableAtomicOperation implements AtomicOperation<Void> {
  EnableDisableCtyunServerGroupDescription description

  abstract boolean isDisable()

  abstract String getBasePhase()

  AbstractEnableDisableAtomicOperation(EnableDisableCtyunServerGroupDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    String basePhase = getBasePhase()

    task.updateStatus basePhase, "Initializing disable server group $description.serverGroupName in $description.region..."


    def serverGroupName = description.serverGroupName
    def region = description.region
    try {
      def client = new CtyunAutoScalingClient(
        description.credentials.credentials.accessKey,
        description.credentials.credentials.securityKey,
        region
      )
      def cvmClient = new CloudVirtualMachineClient(
        description.credentials.credentials.accessKey,
        description.credentials.credentials.securityKey,
        region
      )
      // find auto scaling group
      def asg = getAutoScalingGroup client, serverGroupName
      def asgId = asg.groupID
     // def loadBalancers=getLoadBalancerList client,asgId,serverGroupName


      // enable or disable auto scaling group
      enableOrDisableAutoScalingGroup client, asgId

      // get in service instances in auto scaling group

      def inServiceInstanceIds = getInServiceAutoScalingInstances client, asgId

      if (!inServiceInstanceIds) {
        task.updateStatus basePhase, "Auto scaling group has no IN_SERVICE instance. "
        return null
      }

      // enable or disable load balancer
      enableOrDisableInstances cvmClient,inServiceInstanceIds
    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.class)
      throw e
    }
    task.updateStatus basePhase, "Complete enable server group $serverGroupName in $region."
    null
  }

  private def getAutoScalingGroup(CtyunAutoScalingClient client, String serverGroupName) {
    def asgs = client.getAutoScalingGroupsByName serverGroupName
    if (asgs) {
      def asg = asgs[0]
      def asgId = asg.groupID
      task.updateStatus basePhase, "Server group $serverGroupName's auto scaling group id is $asgId"
      asg
    } else {
      task.updateStatus basePhase, "Server group $serverGroupName is not found."
      null
    }
  }


  private void enableOrDisableAutoScalingGroup(CtyunAutoScalingClient client, Integer asgId) {
    if (isDisable()) {
      task.updateStatus basePhase, "Disabling auto scaling group $asgId..."
      client.disableAutoScalingGroup asgId
      task.updateStatus basePhase, "Auto scaling group $asgId status is disabled."
    } else {
      task.updateStatus basePhase, "Enabling auto scaling group $asgId..."
      client.enableAutoScalingGroup asgId
      task.updateStatus basePhase, "Auto scaling group $asgId status is enabled."
    }
  }

  private def getInServiceAutoScalingInstances(CtyunAutoScalingClient client, Integer asgId) {
    task.updateStatus basePhase, "Get instances managed by auto scaling group $asgId"
    def instances = client.getAutoScalingInstances asgId

    if (!instances) {
      task.updateStatus basePhase, "Found no instance in $asgId."
      return null
    }

    def inServiceInstanceIds = instances.collect {
      //1：正常。 1：已启用。
      /*if (it.healthStatus == 1 && it.status == 1) {*/
      if (it.status == 1) {
        return it.instanceID
      }
    }

    task.updateStatus basePhase, "Auto scaling group $asgId has InService instances $inServiceInstanceIds"
    inServiceInstanceIds
  }
  private def enableOrDisableInstances(CloudVirtualMachineClient vcmClient, inServiceInstanceIds) {
    if (isDisable()) {
      task.updateStatus basePhase, "Auto scaling group is stoping thie instances"
      vcmClient.terminateInstances inServiceInstanceIds
    } else {
      task.updateStatus basePhase, "Auto scaling group is starting thie instances"
      vcmClient.startInstances inServiceInstanceIds
    }

  }

  static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }

  @Canonical
  static class Target {
    String instanceId
    Integer weight
    Integer port
  }
}
