package com.netflix.spinnaker.clouddriver.ctyun.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.ctyun.client.LoadBalancerClient
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.DeleteCtyunLoadBalancerDescription
import com.netflix.spinnaker.clouddriver.ctyun.exception.ExceptionUtils
import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancerRule
import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancerTarget
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.CtyunLoadBalancerProvider
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

/**
 * curl -X POST -H "Content-Type: application/json" -d '[ { "deleteLoadBalancer": {"application":"myapplication", "account":"account-test","loadBalancerId": "lb-kf2lp6cj", "region":"ap-guangzhou", "listener":[{"listenerId":"lbl-d2no6v2c", "targets":[{"instanceId":"ins-lq6o6xyc","port":8080}]}] }} ]' localhost:7004/ctyun/ops
 *
 * curl -X POST -H "Content-Type: application/json" -d '[ { "deleteLoadBalancer": {"application":"myapplication", "account":"account-test","loadBalancerId": "lb-kf2lp6cj", "region":"ap-guangzhou", "listener":[{"listenerId":"lbl-hzrdz86n","rules":[{"locationId":"loc-lbcmvnlt","targets":[{"instanceId":"ins-lq6o6xyc","port":8080}]}]}] }} ]' localhost:7004/ctyun/ops
 */

@Slf4j
class DeleteCtyunLoadBalancerAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "DELETE_LOAD_BALANCER"
  DeleteCtyunLoadBalancerDescription description

  @Autowired
  CtyunLoadBalancerProvider ctyunLoadBalancerProvider

  DeleteCtyunLoadBalancerAtomicOperation(DeleteCtyunLoadBalancerDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    task.updateStatus(BASE_PHASE, "Initializing delete of Ctyun loadBalancer ${description.loadBalancerId} " +
      "in ${description.region}...")
    log.info("params = ${description}")

    def lbListener = description.listener
    try {
      if (lbListener?.size() > 0) {    //如果有监听先删除listener
        lbListener.each {
          def listenerId = it.listenerId
          def rules = it.rules
          def targets = it.targets
          if (targets?.size() > 0) {    //如果有targets，delete listener's targets
            deleteListenerTargets(description.loadBalancerId, listenerId, targets)
          }
          if (rules?.size() > 0) {  //如果有转发规则，delete listener's rules
            rules.each {
              def ruleTargets = it.targets
              if (ruleTargets?.size() > 0) {    //如果有ruleTargets，delete rule's targets
                deleteRuleTargets(description.loadBalancerId, listenerId, it.locationId, ruleTargets)
              }
              //delete rule
              deleteListenerRule(description.loadBalancerId, listenerId, it)
            }
          }
          //delete listener
          deleteListener(description.loadBalancerId, listenerId)
        }
      }
      //delete loadBalancer
      deleteLoadBalancer(description.loadBalancerId)
    } catch (Exception e) {
      log.error(e)
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.class)
      throw e
    }
    return null
  }

  private void deleteLoadBalancer(String loadBalancerId) {
    task.updateStatus(BASE_PHASE, "Start delete loadBalancer ${loadBalancerId} ...")
    def lbClient = new LoadBalancerClient(
      description.credentials.credentials.accessKey,
      description.credentials.credentials.securityKey,
      description.region
    )
    def ret = lbClient.deleteLoadBalancerByIds(loadBalancerId)
    task.updateStatus(BASE_PHASE, "Delete loadBalancer ${loadBalancerId} ${ret} end")
  }

  private void deleteListener(String loadBalancerId, String listenerId) {
    task.updateStatus(BASE_PHASE, "Start delete Listener ${listenerId} ...")
    def lbClient = new LoadBalancerClient(
      description.credentials.credentials.accessKey,
      description.credentials.credentials.securityKey,
      description.region
    )
    def ret = lbClient.deleteLBListenerById(loadBalancerId, listenerId)
    task.updateStatus(BASE_PHASE, "Delete loadBalancer ${listenerId} ${ret} end")
  }

  private void deleteListenerTargets(String loadBalancerId, String listenerId, List<CtyunLoadBalancerTarget> targets) {
    task.updateStatus(BASE_PHASE, "Start delete Listener ${listenerId} targets ...")
    def lbClient = new LoadBalancerClient(
      description.credentials.credentials.accessKey,
      description.credentials.credentials.securityKey,
      description.region
    )
    def ret = lbClient.deRegisterTarget4Layer(loadBalancerId, listenerId, targets)
    task.updateStatus(BASE_PHASE, "Delete loadBalancer ${listenerId} targets ${ret} end")
  }

  private void deleteListenerRule(String loadBalancerId, String listenerId, CtyunLoadBalancerRule rule) {
    task.updateStatus(BASE_PHASE, "Start delete Listener ${listenerId} rules ...")
    def lbClient = new LoadBalancerClient(
      description.credentials.credentials.accessKey,
      description.credentials.credentials.securityKey,
      description.region
    )
    def rules = [rule]
    def ret = lbClient.deleteLBListenerRules(loadBalancerId, listenerId, rules)
    task.updateStatus(BASE_PHASE, "Delete loadBalancer ${listenerId} rules ${ret} end")
  }

  private void deleteRuleTargets(String loadBalancerId, String listenerId, String locationId, List<CtyunLoadBalancerTarget> targets) {
    task.updateStatus(BASE_PHASE, "Start delete Listener ${listenerId} rule ${locationId} targets ...")
    def lbClient = new LoadBalancerClient(
      description.credentials.credentials.accessKey,
      description.credentials.credentials.securityKey,
      description.region
    )
    def ret = lbClient.deRegisterTarget7Layer(loadBalancerId, listenerId, locationId, targets)
    task.updateStatus(BASE_PHASE, "Delete loadBalancer ${listenerId} rule ${locationId} targets ${ret} end")
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
