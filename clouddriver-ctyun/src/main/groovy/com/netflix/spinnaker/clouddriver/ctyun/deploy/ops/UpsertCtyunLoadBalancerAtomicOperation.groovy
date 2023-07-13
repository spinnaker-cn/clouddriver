package com.netflix.spinnaker.clouddriver.ctyun.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.UpsertCtyunLoadBalancerDescription
import com.netflix.spinnaker.clouddriver.ctyun.exception.ExceptionUtils
import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancerHealthCheck
import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancerListener
import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancerRule
import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancerTarget
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.CtyunLoadBalancerProvider
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunLoadBalancerClient
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum
import com.tencentcloudapi.clb.v20180317.models.HealthCheck
import com.tencentcloudapi.clb.v20180317.models.ListenerBackend
import com.tencentcloudapi.clb.v20180317.models.Listener
import com.tencentcloudapi.clb.v20180317.models.RuleOutput
import com.tencentcloudapi.clb.v20180317.models.RuleTargets
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

/**
 curl -X POST -H "Content-Type: application/json" -d '[ { "upsertLoadBalancer": {"application":"myapplication", "account":"account-test", "loadBalancerName": "fengCreate5", "region":"ap-guangzhou", "loadBalancerType":"OPEN" ,"listener":[{"listenerName":"listen-create","port":80,"protocol":"TCP", "targets":[{"instanceId":"ins-lq6o6xyc", "port":8080}]}]}} ]' localhost:7004/ctyun/ops
 */

@Slf4j
class UpsertCtyunLoadBalancerAtomicOperation implements AtomicOperation<Map> {

  private static final String BASE_PHASE = "UPSERT_LOAD_BALANCER"
  UpsertCtyunLoadBalancerDescription description

  @Autowired
  CtyunLoadBalancerProvider ctyunLoadBalancerProvider

  UpsertCtyunLoadBalancerAtomicOperation(UpsertCtyunLoadBalancerDescription description) {
    this.description = description
  }

  @Override
  Map operate(List priorOutputs) {
    task.updateStatus(BASE_PHASE, "Initializing upsert of Ctyun loadBalancer ${description.loadBalancerName} " +
      "in ${description.region}...")
    log.info("params = ${description}")
    try {
      def loadBalancerId = description.loadBalancerId
      if (loadBalancerId?.length() > 0) {
        updateLoadBalancer(description)
      } else {  //create new loadBalancer
        insertLoadBalancer(description)
      }
    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.class)
      throw e
    }
    return [loadBalancers: [(description.region): [name: description.loadBalancerName]]]
  }


  private String insertLoadBalancer(UpsertCtyunLoadBalancerDescription description) {
    task.updateStatus(BASE_PHASE, "Start create new loadBalancer ${description.loadBalancerName} ...")

    def lbClient = new CtyunLoadBalancerClient(
      description.credentials.credentials.accessKey,
      description.credentials.credentials.securityKey,
      description.region
    )
    def loadBalancerId = lbClient.createLoadBalancer(description)[0]
    Thread.sleep(3000)  //wait for create loadBalancer success
    def loadBalancer = lbClient.getLoadBalancerById(loadBalancerId) //query is create success
    if (loadBalancer.isEmpty()) {
      task.updateStatus(BASE_PHASE, "Create new loadBalancer ${description.loadBalancerName} failed!")
      return ""
    }
    task.updateStatus(BASE_PHASE, "Create new loadBalancer ${description.loadBalancerName} success, id is ${loadBalancerId}.")

    //set securityGroups to loadBalancer
    if (description.loadBalancerType.equals("OPEN") && (description.securityGroups?.size() > 0)) {
      //天翼云负载均衡没有设置安全组接口
      /*task.updateStatus(BASE_PHASE, "Start set securityGroups ${description.securityGroups} to loadBalancer ${loadBalancerId} ...")
      lbClient.setLBSecurityGroups(loadBalancerId, description.securityGroups)
      task.updateStatus(BASE_PHASE, "set securityGroups toloadBalancer ${loadBalancerId} end")*/
    }

    //create listener
    def lbListener = description.listener
    if (lbListener?.size() > 0) {
      lbListener.each {
        insertListener(lbClient, loadBalancerId, it,description.vpcId)
      }
    }
    task.updateStatus(BASE_PHASE, "Create new loadBalancer ${description.loadBalancerName} end")
    return ""
  }

  private String updateLoadBalancer(UpsertCtyunLoadBalancerDescription description) {
    task.updateStatus(BASE_PHASE, "Start update loadBalancer ${description.loadBalancerId} ...")

    def lbClient = new CtyunLoadBalancerClient(
      description.credentials.credentials.accessKey,
      description.credentials.credentials.securityKey,
      description.region
    )
    def loadBalancerId = description.loadBalancerId
    /*def loadBalancer = lbClient.getLoadBalancerById(loadBalancerId) //query is exist
    if (loadBalancer.isEmpty()) {
      task.updateStatus(BASE_PHASE, "LoadBalancer ${loadBalancerId} not exist!")
      return ""
    }*/

    //update securityGroup
    /*if (loadBalancer[0].loadBalancerType.equals("OPEN")) {
      task.updateStatus(BASE_PHASE, "Start update securityGroups ${description.securityGroups} to loadBalancer ${loadBalancerId} ...")
      lbClient.setLBSecurityGroups(loadBalancerId, description.securityGroups)
      task.updateStatus(BASE_PHASE, "update securityGroups to loadBalancer ${loadBalancerId} end")
    }*/

    def newListeners = description.listener

    //get all listeners info
    def queryListeners = lbClient.getAllLBListener(loadBalancerId)
//    def listenerIdList = queryListeners.collect {
//      it.ID
//    } as List<String>
//    def queryLBTargetList = lbClient.getLBTargetList(loadBalancerId, listenerIdList)

    //delete old listener
    queryListeners.each { oldListener ->
      def keepListener = newListeners.find {
        it.listenerId.equals(oldListener.ID)
      }
      if (keepListener == null) {
        task.updateStatus(BASE_PHASE, "Start delete listener ${oldListener.ID} in ${loadBalancerId} ...")
        //如有转发规则，先删转发规则
        if(oldListener.protocol in ["HTTP", "HTTPS"]){
          //查询转发规则
          def queryRules = lbClient.getAllRule(loadBalancerId)
          def listenerRules = queryRules.findAll(){
            it.listenerID.equals(oldListener.ID)
          }
          def delRules = listenerRules.collect() {
            def delRule = new CtyunLoadBalancerRule()
            delRule.locationId = it.ID
            delRule
          }
          //删除监听下的所有转发规则
          if(listenerRules!=null&&queryRules.size()>0){
            lbClient.deleteLBListenerRules(loadBalancerId,oldListener.ID,delRules)
          }
        }
        def ret = lbClient.deleteLBListenerById(loadBalancerId, oldListener.ID)
        task.updateStatus(BASE_PHASE, "Delete listener ${oldListener.ID} in ${loadBalancerId} ${ret} end")
      }
    }
    //comapre listener
    if (newListeners?.size() > 0) {
      newListeners.each { inputListener ->
        if (inputListener.listenerId?.length() > 0) {
          /*def oldListener = queryListeners.find {
            it.listenerId.equals(inputListener.listenerId)
          }
          if (oldListener != null) {
            def oldTargets = queryLBTargetList.find {
              it.listenerId.equals(inputListener.listenerId)
            }
            updateListener(lbClient, loadBalancerId, oldListener, inputListener, oldTargets) //modify
          } else {
            task.updateStatus(BASE_PHASE, "Input listener ${inputListener.listenerId} not exist!")
          }*/
        } else {  //not listener id, create new
          insertListener(lbClient, loadBalancerId, inputListener,description.vpcId)
        }
      }
    }

    task.updateStatus(BASE_PHASE, "Update loadBalancer ${description.loadBalancerId} end")
    return ""
  }


  private String insertListener(CtyunLoadBalancerClient lbClient, String loadBalancerId, CtyunLoadBalancerListener listener,String vpcId) {
    task.updateStatus(BASE_PHASE, "Start create new ${listener.protocol} listener in ${loadBalancerId} ...")

    def listenerId = lbClient.createLBListener(loadBalancerId, listener, vpcId)[0]
    if (listenerId?.length() > 0) {
      task.updateStatus(BASE_PHASE, "Create new ${listener.protocol} listener in ${loadBalancerId} success, id is ${listenerId}.")
      if (listener.protocol in ["TCP", "UDP"]) {   //tcp/udp 4 layer
        def targets = listener.targets
        if (targets?.size() > 0) {
          task.updateStatus(BASE_PHASE, "Start Register targets to listener ${listenerId} ...")
          //def ret = lbClient.registerTarget4Layer(loadBalancerId, listenerId, targets)
          task.updateStatus(BASE_PHASE, "Register targets to listener ${listenerId} ${ret} end.")
        }
      } else if (listener.protocol in ["HTTP", "HTTPS"]) {   //http/https 7 layer
        def rules = listener.rules
        if (rules?.size() > 0) {
          rules.each {
            insertLBListenerRule(lbClient, loadBalancerId, listenerId, it, listener, vpcId)
          }
        }
      }
    } else {
      task.updateStatus(BASE_PHASE, "Create new listener failed!")
      return ""
    }
    task.updateStatus(BASE_PHASE, "Create new ${listener.protocol} listener in ${loadBalancerId} end")
    return ""
  }

  private boolean isEqualHealthCheck(HealthCheck oldHealth, CtyunLoadBalancerHealthCheck newHealth) {
    if ((oldHealth != null) && (newHealth != null)) {
      if (!oldHealth.healthSwitch.equals(newHealth.healthSwitch)
        || !oldHealth.timeOut.equals(newHealth.timeOut)
        || !oldHealth.intervalTime.equals(newHealth.intervalTime)
        || !oldHealth.healthNum.equals(newHealth.healthNum)
        || !oldHealth.unHealthNum.equals(newHealth.unHealthNum)
        || !oldHealth.httpCode.equals(newHealth.httpCode)
        || !oldHealth.httpCheckPath.equals(newHealth.httpCheckPath)
        || !oldHealth.httpCheckDomain.equals(newHealth.httpCheckDomain)
        || !oldHealth.httpCheckMethod.equals(newHealth.httpCheckMethod)) {
        return false
      }
    }
    return true
  }

  private boolean isEqualListener(Listener oldListener, CtyunLoadBalancerListener newListener) {
    def oldHealth = oldListener.healthCheck
    def newHealth = newListener.healthCheck

    if (!isEqualHealthCheck(oldHealth, newHealth)) {
      return false
    }
    return true
  }

  private String modifyListenerAttr(CtyunLoadBalancerClient lbClient, String loadBalancerId,
                                    CtyunLoadBalancerListener listener) {
    task.updateStatus(BASE_PHASE, "Start modify listener ${listener.listenerId} attr in ${loadBalancerId} ...")
    def ret = lbClient.modifyListener(loadBalancerId, listener)
    task.updateStatus(BASE_PHASE, "modify listener ${listener.listenerId} attr in ${loadBalancerId} ${ret} end")
    return ""
  }

  private String updateListener(CtyunLoadBalancerClient lbClient, String loadBalancerId, Listener oldListener,
                                CtyunLoadBalancerListener newListener, ListenerBackend targets) {
    task.updateStatus(BASE_PHASE, "Start update listener ${newListener.listenerId} in ${loadBalancerId} ...")

    if (!isEqualListener(oldListener, newListener)) {
      modifyListenerAttr(lbClient, loadBalancerId, newListener)
    }

    def oldRules = oldListener.rules
    def newRules = newListener.rules

    if (newListener.protocol in ["TCP", "UDP"]) {   //tcp/udp 4 layer, targets
      def oldTargets = targets.targets
      def newTargets = newListener.targets
      //delete targets
      def delTargets = [] as List<CtyunLoadBalancerTarget>
      oldTargets.each { oldTargetEntry ->
        def keepTarget = newTargets.find {
          it.instanceId.equals(oldTargetEntry.instanceId)
        }
        if (keepTarget == null) {
          def delTarget = new CtyunLoadBalancerTarget(instanceId: oldTargetEntry.instanceId,
            port: oldTargetEntry.port, weight: oldTargetEntry.weight, type: oldTargetEntry.type)
          delTargets.add(delTarget)
        }
      }
      if (!delTargets.isEmpty()) {
        task.updateStatus(BASE_PHASE, "delete listener target in ${loadBalancerId}.${newListener.listenerId} ...")
        lbClient.deRegisterTarget4Layer(loadBalancerId, newListener.listenerId, delTargets)
      }
      //add targets
      def addTargets = [] as List<CtyunLoadBalancerTarget>
      newTargets.each { newTargetEntry ->
        if (newTargetEntry.instanceId?.length() > 0) {
          addTargets.add(newTargetEntry)
        }
      }
      if (!addTargets.isEmpty()) {
        task.updateStatus(BASE_PHASE, "add listener target to ${loadBalancerId}.${newListener.listenerId} ...")
        lbClient.registerTarget4Layer(loadBalancerId, newListener.listenerId, addTargets)
      }
    } else if (newListener.protocol in ["HTTP", "HTTPS"]) {  // 7 layer, rules, targets
      oldRules.each { oldRuleEntry ->          //delete rule
        def keepRule = newRules.find {
          oldRuleEntry.locationId.equals(it.locationId)
        }
        if (keepRule == null) {
          lbClient.deleteLBListenerRule(loadBalancerId, newListener.listenerId, oldRuleEntry.locationId)
        }
      }
      newRules.each { newRuleEntry ->         //modify rule
        if (newRuleEntry.locationId?.length() > 0) {
          def oldRule = oldRules.find {
            newRuleEntry.locationId.equals(it.locationId)
          }
          if (oldRule != null) {  //modify rule
            def ruleTargets = targets.rules.find {
              it.locationId.equals(newRuleEntry.locationId)
            }
            updateLBListenerRule(lbClient, loadBalancerId, newListener.listenerId, oldRule, newRuleEntry, ruleTargets)
          } else {
            task.updateStatus(BASE_PHASE, "Input rule ${newRuleEntry.locationId} not exist!")
          }
        } else {    //create new rule
          lbClient.createLBListenerRule(loadBalancerId, newListener.listenerId, newRuleEntry)
        }
      }
    }

    task.updateStatus(BASE_PHASE, "update listener ${newListener.listenerId} in ${loadBalancerId} end")
    return ""
  }

  private boolean isEqualRule(RuleOutput oldRule, CtyunLoadBalancerRule newRule) {
    def oldHealth = oldRule.healthCheck
    def newHealth = newRule.healthCheck

    if (!isEqualHealthCheck(oldHealth, newHealth)) {
      return false
    }
    return true
  }

  private modifyRuleAttr(CtyunLoadBalancerClient lbClient, String loadBalancerId,
                         String listenerId, CtyunLoadBalancerRule newRule) {
    task.updateStatus(BASE_PHASE, "Start modify rule ${newRule.locationId} attr in ${loadBalancerId}.${listenerId} ...")
    def ret = lbClient.modifyLBListenerRule(loadBalancerId, listenerId, newRule)
    task.updateStatus(BASE_PHASE, "modify rule ${newRule.locationId} attr in ${loadBalancerId}.${listenerId} ${ret} end")
    return ""
  }

  private String updateLBListenerRule(CtyunLoadBalancerClient lbClient, String loadBalancerId,
                                      String listenerId, RuleOutput oldRule,
                                      CtyunLoadBalancerRule newRule, RuleTargets targets) {
    task.updateStatus(BASE_PHASE, "Start update rule ${newRule.locationId} in ${loadBalancerId}.${listenerId} ...")

    if (!isEqualRule(oldRule, newRule)) {      //modifyRuleAttr()
      modifyRuleAttr(lbClient, loadBalancerId, listenerId, newRule)
    }

    def newTargets = newRule.targets
    def oldTargets = targets.Targets

    //delete target
    def delTargets = [] as List<CtyunLoadBalancerTarget>
    oldTargets.each { oldTargetEntry ->
      def keepTarget = newTargets.find {
        it.instanceId.equals(oldTargetEntry.instanceId)
      }
      if (keepTarget == null) {
        def delTarget = new CtyunLoadBalancerTarget(instanceId: oldTargetEntry.instanceId,
          port: oldTargetEntry.port, weight: oldTargetEntry.weight, type: oldTargetEntry.type)
        delTargets.add(delTarget)
      }
    }
    if (!delTargets.isEmpty()) {
      task.updateStatus(BASE_PHASE, "del rule target in ${loadBalancerId}.${listenerId}.${newRule.locationId} ...")
      lbClient.deRegisterTarget7Layer(loadBalancerId, listenerId, newRule.locationId, delTargets)
    }

    //add target
    def addTargets = [] as List<CtyunLoadBalancerTarget>
    newTargets.each { newTargetEntry ->
      if (newTargetEntry.instanceId?.length() > 0) {
        addTargets.add(newTargetEntry)
      }
    }
    if (!addTargets.isEmpty()) {
      task.updateStatus(BASE_PHASE, "add rule target to ${loadBalancerId}.${listenerId}.${newRule.locationId} ...")
      lbClient.registerTarget7Layer(loadBalancerId, listenerId, newRule.locationId, addTargets)
    }

    task.updateStatus(BASE_PHASE, "update rule ${newRule.locationId} in ${loadBalancerId}.${listenerId} end")
    return ""
  }


  synchronized String genId() {
    long number = System.currentTimeMillis()
    /** 将数字转为62进制。小端，个位数在前。 */
    final char[] NumberToText_SIXTWO_ARR = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    final int scale = 62;
    StringBuilder sb = new StringBuilder(12);
    boolean negative=number<0;
    if(negative) number=-number;
    if(number<0) return "8m85Y0n8LzA-";
    //SU.Log("NumberToText_SIXTWO_LE", number, -(number+1));
    long remainder;
    while (number != 0) {
      remainder = number % scale;
      sb.append(NumberToText_SIXTWO_ARR[(int) remainder]);
      number = number / scale;
    }
    if(negative) sb.append('-');
    return sb.toString();
  }

  private String insertLBListenerRule(CtyunLoadBalancerClient lbClient, String loadBalancerId,
                                      String listenerId, CtyunLoadBalancerRule rule,
                                      CtyunLoadBalancerListener listener,String vpcId) {
    task.updateStatus(BASE_PHASE, "Start create new rule ${rule.domain} ${rule.url} in ${listenerId}")
    String id = genId()
    //创建健康检查
    def healthCheckId
    String[] httpCodes = []
    if(rule.healthCheck.httpCode!=null && rule.healthCheck.httpCode.size()>0){
      httpCodes = rule.healthCheck.httpCode.split(",");
    }else {
      httpCodes[0]="http_2xx"
    }
    if (rule.healthCheck != null) {
      try {
        healthCheckId = lbClient.createHealthCheck("Health_"+id,"HTTP", httpCodes, rule.healthCheck)
      }catch (Exception e){
        log.error("createHealthCheck err:",e)
        throw e;
      }
    }
    //先创建后端服务组
    def targetGroupId = lbClient.createTargetGroup(healthCheckId, "ruleTg_"+id, loadBalancerId, listener, vpcId)[0]

    def ret = lbClient.createLBListenerRule(loadBalancerId, listenerId, rule,targetGroupId)
    task.updateStatus(BASE_PHASE, "Create new rule ${rule.domain} ${rule.url} in ${listenerId} ${ret} end.")
    def ruleTargets = rule.targets
    if (ruleTargets?.size() > 0) {
      task.updateStatus(BASE_PHASE, "Start Register targets to listener ${listenerId} rule ...")
      //def retVal = lbClient.registerTarget7Layer(loadBalancerId, listenerId, rule.domain, rule.url, ruleTargets)
      task.updateStatus(BASE_PHASE, "Register targets to listener ${listenerId} rule ${retVal} end.")
    }
    return ""
  }


  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
