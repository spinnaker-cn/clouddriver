package com.netflix.spinnaker.clouddriver.ctyun.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunVirtualPrivateCloudClient
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.UpsertCtyunSecurityGroupDescription
import com.netflix.spinnaker.clouddriver.ctyun.exception.ExceptionUtils
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunSecurityGroupRule
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum
import groovy.util.logging.Slf4j

@Slf4j
class UpsertCtyunSecurityGroupAtomicOperation implements AtomicOperation<Map> {

  private static final String BASE_PHASE = "UPSERT_SECURITY_GROUP"
  UpsertCtyunSecurityGroupDescription description

  UpsertCtyunSecurityGroupAtomicOperation(UpsertCtyunSecurityGroupDescription description) {
    this.description = description
  }


  @Override
  Map operate(List priorOutputs) {
    task.updateStatus(BASE_PHASE, "Initializing upsert of Ctyun securityGroup ${description.securityGroupName} " +
      "in ${description.region}...")
    log.info("params = ${description}")
    try {
      def securityGroupId = description.securityGroupId
      if (securityGroupId?.length() > 0) {
        updateSecurityGroup(description)
      } else {
        insertSecurityGroup(description)
      }
    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.class)
      throw e
    }
    log.info("upsert securityGroup name:${description.securityGroupName}, id:${description.securityGroupId}")
    return [securityGroups: [(description.region): [name: description.securityGroupName, id: description.securityGroupId]]]
  }


  private String updateSecurityGroup(UpsertCtyunSecurityGroupDescription description) {
    task.updateStatus(BASE_PHASE, "Start update securityGroup ${description.securityGroupName} ${description.securityGroupId} ...")
    def securityGroupId = description.securityGroupId
    def vpcClient = new CtyunVirtualPrivateCloudClient(
      description.credentials.credentials.getAccessKey(),
      description.credentials.credentials.getSecurityKey(),
      description.region
    )

    def oldGroupRules = vpcClient.getSecurityGroupPolicies(securityGroupId)
    def newGroupInRules = description.inRules
    def i=0
    //del in rules
    def delGroupInRules = [] as List<CtyunSecurityGroupRule>
    oldGroupRules?.stream().filter({s -> "ingress".equals(s.getDirection())}).each { ingress ->
      def keepRule = newGroupInRules.find {
        //it.id.equals(ingress.id)
        it.index==i
      }
      i++
      if (keepRule == null) {
        def delInRule = new CtyunSecurityGroupRule(id: ingress.id)
        delGroupInRules.add(delInRule)
      }
    }
    if (!delGroupInRules.isEmpty()) {
      task.updateStatus(BASE_PHASE, "Start delete securityGroup ${securityGroupId} rules ...")
      vpcClient.deleteSecurityGroupInRules(securityGroupId, delGroupInRules)
      task.updateStatus(BASE_PHASE, "delete securityGroup ${securityGroupId} rules end")
    }
    //add in rules
    def addGroupInRules = [] as List<CtyunSecurityGroupRule>
    newGroupInRules?.each {
      if (it.index == null) {
        addGroupInRules.add(it)
      }
    }
    if (!addGroupInRules.isEmpty()) {
      task.updateStatus(BASE_PHASE, "Start add securityGroup ${securityGroupId} rules ...")
      vpcClient.createSecurityGroupRules(securityGroupId, addGroupInRules, null)
      task.updateStatus(BASE_PHASE, "add securityGroup ${securityGroupId} rules end")
    }

    task.updateStatus(BASE_PHASE, "Update securityGroup ${description.securityGroupName} ${description.securityGroupId} end")
    return ""
  }


  private String insertSecurityGroup(UpsertCtyunSecurityGroupDescription description) {
    task.updateStatus(BASE_PHASE, "Start create new securityGroup ${description.securityGroupName} ...")

    def vpcClient = new CtyunVirtualPrivateCloudClient(
      description.credentials.credentials.getAccessKey(),
      description.credentials.credentials.getSecurityKey(),
      description.region
    )
    def securityGroupId = vpcClient.createSecurityGroup(description.vpcId,description.securityGroupName, description.securityGroupDesc)
    description.securityGroupId = securityGroupId
    task.updateStatus(BASE_PHASE, "Create new securityGroup ${description.securityGroupName} success, id is ${securityGroupId}.")

    if (description.inRules?.size() > 0) {
      task.updateStatus(BASE_PHASE, "Start create new securityGroup rules in ${securityGroupId} ...")
      vpcClient.createSecurityGroupRules(securityGroupId, description.inRules, description.outRules)
      task.updateStatus(BASE_PHASE, "Create new securityGroup rules in ${securityGroupId} end")
    }
    return ""
  }


  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }

}
