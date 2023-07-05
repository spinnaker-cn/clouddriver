package com.netflix.spinnaker.clouddriver.ctyun.deploy.ops

import cn.ctyun.ctapi.ctelb.createtarget.CreateTargetRequestBody
import cn.ctyun.ctapi.scaling.listloadbalancer.ListLoadBalancer
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
      //if (!asg.loadBalancerIdSet && !asg.forwardLoadBalancerSet) {
     /* if(!loadBalancers){
        task.updateStatus basePhase, "Auto scaling group does not have a load balancer. "
        return null
      }*/
      enableOrDisableInstances cvmClient,inServiceInstanceIds
      //enableOrDisableClassicLoadBalancer client, loadBalancers, inServiceInstanceIds
      //enableOrDisableForwardLoadBalancer client, asg, inServiceInstanceIds
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

  /*//获取关联负载均衡
  private def getLoadBalancerList(CtyunAutoScalingClient client, Integer groupId, String serverGroupName) {
    def lb = client.getLoadBalancerListByGroupId groupId
    if (lb&&lb.size()>0) {
      def lbSize=lb.size()
      task.updateStatus basePhase, "Server group $serverGroupName's loadBalancers size is $lbSize"
      lb
    } else {
      task.updateStatus basePhase, "Server group $serverGroupName is not found."
      null
    }
  }*/

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
  /*private def enableOrDisableClassicLoadBalancer(CtyunAutoScalingClient client, List<ListLoadBalancer> lbList, inServiceInstanceIds) {

   // def classicLbs = asg.loadBalancerIdSet
    def hostGroupIDList=lbList.collect({it.hostGroupID})
    task.updateStatus basePhase, "Auto scaling group is attached to classic load balancers hostGroupIDList $hostGroupIDList"

    for (def hostGroupID : hostGroupIDList) {
      if (isDisable()) {
        deregisterInstancesFromClassicalLb client, hostGroupID, inServiceInstanceIds
      } else {
        registerInstancesWithClassicalLb client, hostGroupID, inServiceInstanceIds
      }
    }
  }

  private void deregisterInstancesFromClassicalLb(CtyunAutoScalingClient client, hostGroupID, inServiceInstanceIds) {
    task.updateStatus basePhase, "Start detach instances $inServiceInstanceIds from classic load balancers hostGroupID $hostGroupID"
    def targetList=client.getTargetList(hostGroupID)
    def classicLbInstanceIds =targetList.collect({
      it.instanceID
    })
    def instanceIds = inServiceInstanceIds.grep classicLbInstanceIds


    if (instanceIds) {
      task.updateStatus basePhase, "Classic load balancer has instances $classicLbInstanceIds " +
        "instances $instanceIds in both auto scaling group and load balancer will be detached from load balancer."
     // client.detachAutoScalingInstancesFromClassicClb lbId, instanceIds
      targetList.each {
        if(instanceIds.contains(it.instanceID)){
          client.deleteTarget(it.ID)
        }
      }
    } else {
      task.updateStatus basePhase, "Instances $inServiceInstanceIds are not attached with load balancer hostGroupID $hostGroupID"
    }
    task.updateStatus basePhase, "Finish detach instances $inServiceInstanceIds from classic load balancers  hostGroupID $hostGroupID"
  }

  private void registerInstancesWithClassicalLb(CtyunAutoScalingClient client, hostGroupID, inServiceInstanceIds) {
    task.updateStatus basePhase, "Start attach instances $inServiceInstanceIds to classic load balancers hostGroupID $hostGroupID"
    //def inServiceClassicTargets = []
    inServiceInstanceIds.each { String instanceId ->
      *//*def target = new Target(
        instanceId: instanceId,
        weight: 10  // default weight 10 for classic lb
      )
      inServiceClassicTargets.add target*//*
      if(instanceId!=null){
        CreateTargetRequestBody body=new CreateTargetRequestBody().withRegionID(description.region).withClientToken(UUID.randomUUID().toString()).withTargetGroupID(hostGroupID)
          .withInstanceType("VM").withInstanceID(instanceId).withProtocolPort(80)
        client.createTarget(body)
      }
    }
    //client.attachAutoScalingInstancesToClassicClb lbId, inServiceClassicTargets
    task.updateStatus basePhase, "Finish attach instances $inServiceInstanceIds to classic load balancers hostGroupID $hostGroupID"
  }*/

 /* private def enableOrDisableForwardLoadBalancer(client, asg, inServiceInstanceIds) {
    if (!asg.forwardLoadBalancerSet) {
      return null
    }
    def forwardLbs = asg.forwardLoadBalancerSet

    for (def flb : forwardLbs) {
      if (isDisable()) {
        deregisterInstancesFromForwardlLb client, flb, inServiceInstanceIds
      } else {
        registerInstancesWithForwardlLb client, flb, inServiceInstanceIds
      }
    }
  }*/

  /*private def deregisterInstancesFromForwardlLb(client, flb, inServiceInstanceIds) {
    def flbId = flb.loadBalancerId
    task.updateStatus basePhase, "Start detach instances $inServiceInstanceIds from forward load balancers $flbId"

    def listeners = client.getForwardLbTargets(flb)
    def forwardLbTargets = []
    def inServiceTargets = []
    inServiceInstanceIds.each { String instanceId ->
      flb.targetAttributes.each { def targetAttribute ->
        def target = new Target(
          instanceId: instanceId,
          port: targetAttribute.port,
          weight: targetAttribute.weight
        )
        inServiceTargets.add target
      }
    }

    listeners.each {
      if (it.protocol == "HTTP" || it.protocol == "HTTPS") {
        it.rules.each { def rule ->
          if (rule.locationId == flb.locationId) {
            for (def flbTarget : rule.targets) {
              forwardLbTargets.add new Target(
                instanceId: flbTarget.instanceId,
                port: flbTarget.port,
                weight: flbTarget.weight
              )
            }
          }
        }
      } else if (it.protocol == "TCP" || it.protocol == "UDP") {
        for (def flbTarget : it.targets) {
          forwardLbTargets.add new Target(
            instanceId: flbTarget.instanceId,
            port: flbTarget.port,
            weight: flbTarget.weight
          )
        }
      } else {
        return
      }
    }

    def targets = inServiceTargets.grep forwardLbTargets
    if (targets) {
      task.updateStatus basePhase, "Forward load balancer has targets $forwardLbTargets " +
        "targets $targets in both auto scaling group and load balancer will be detached from load balancer $flbId."
      client.detachAutoScalingInstancesFromForwardClb flb, targets, true
    } else {
      task.updateStatus basePhase, "Instances $inServiceInstanceIds are not attached with load balancer $flbId"
    }

    task.updateStatus basePhase, "Finish detach instances $inServiceInstanceIds from forward load balancers $flbId"
  }*/

  /*private void registerInstancesWithForwardlLb(client, flb, inServiceInstanceIds) {
    def flbId = flb.loadBalancerId
    task.updateStatus basePhase, "Start attach instances $inServiceInstanceIds from forward load balancers $flbId"

    def inServiceTargets = []
    inServiceInstanceIds.each { String instanceId ->
      flb.targetAttributes.each { def targetAttribute ->
        def target = new Target(
          instanceId: instanceId,
          port: targetAttribute.port,
          weight: targetAttribute.weight
        )
        inServiceTargets.add target
      }
    }

    if (inServiceTargets) {
      task.updateStatus basePhase, "In service targets $inServiceTargets will be attached to forward load balancer $flbId"
      client.attachAutoScalingInstancesToForwardClb flb, inServiceTargets, true
    } else {
      task.updateStatus basePhase, "No instances need to be attached to forward load balancer $flbId"
    }
    task.updateStatus basePhase, "Finish attach instances $inServiceInstanceIds from forward load balancers $flbId"
  }*/

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
