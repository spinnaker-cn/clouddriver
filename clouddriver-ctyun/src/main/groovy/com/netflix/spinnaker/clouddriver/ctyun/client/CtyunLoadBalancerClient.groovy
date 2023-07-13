package com.netflix.spinnaker.clouddriver.ctyun.client

import cn.ctyun.ctapi.CTResponse
import cn.ctyun.ctapi.Credential
import cn.ctyun.ctapi.ctelb.CtelbClient
import cn.ctyun.ctapi.ctelb.createhealthcheck.CreateHealthCheckRequest
import cn.ctyun.ctapi.ctelb.createhealthcheck.CreateHealthCheckRequestBody
import cn.ctyun.ctapi.ctelb.createhealthcheck.CreateHealthCheckResponseData
import cn.ctyun.ctapi.ctelb.createlistener.CreateListenerRequest
import cn.ctyun.ctapi.ctelb.createlistener.CreateListenerRequestBody
import cn.ctyun.ctapi.ctelb.createlistener.CreateListenerResponseData
import cn.ctyun.ctapi.ctelb.createlistener.DefaultAction
import cn.ctyun.ctapi.ctelb.createlistener.ForwardConfig
import cn.ctyun.ctapi.ctelb.createlistener.TargetGroup
import cn.ctyun.ctapi.ctelb.createloadbalancer.CreateLoadbalancerRequest
import cn.ctyun.ctapi.ctelb.createloadbalancer.CreateLoadbalancerRequestBody
import cn.ctyun.ctapi.ctelb.createloadbalancer.CreateLoadbalancerResponseData
import cn.ctyun.ctapi.ctelb.createrule.Action
import cn.ctyun.ctapi.ctelb.createrule.Condition
import cn.ctyun.ctapi.ctelb.createrule.CreateRuleRequest
import cn.ctyun.ctapi.ctelb.createrule.CreateRuleRequestBody
import cn.ctyun.ctapi.ctelb.createrule.CreateRuleResponseData
import cn.ctyun.ctapi.ctelb.createrule.ServerNameConfig
import cn.ctyun.ctapi.ctelb.createrule.UrlPathConfig
import cn.ctyun.ctapi.ctelb.createtargetgroup.CreateTargetGroupRequest
import cn.ctyun.ctapi.ctelb.createtargetgroup.CreateTargetGroupRequestBody
import cn.ctyun.ctapi.ctelb.createtargetgroup.CreateTargetGroupResponseData
import cn.ctyun.ctapi.ctelb.createtargetgroup.SessionSticky
import cn.ctyun.ctapi.ctelb.deletelistener.DeleteListenerRequest
import cn.ctyun.ctapi.ctelb.deletelistener.DeleteListenerRequestBody
import cn.ctyun.ctapi.ctelb.deletelistener.DeleteListenerResponseData
import cn.ctyun.ctapi.ctelb.deleteloadbalancer.DeleteLoadbalancerRequest
import cn.ctyun.ctapi.ctelb.deleteloadbalancer.DeleteLoadbalancerRequestBody
import cn.ctyun.ctapi.ctelb.deleteloadbalancer.DeleteLoadbalancerResponseData
import cn.ctyun.ctapi.ctelb.deleterule.DeleteRuleRequest
import cn.ctyun.ctapi.ctelb.deleterule.DeleteRuleRequestBody
import cn.ctyun.ctapi.ctelb.deleterule.DeleteRuleResponseData
import cn.ctyun.ctapi.ctelb.deletetarget.DeleteTargetRequest
import cn.ctyun.ctapi.ctelb.deletetarget.DeleteTargetRequestBody
import cn.ctyun.ctapi.ctelb.deletetarget.DeleteTargetResponseData
import cn.ctyun.ctapi.ctelb.listhealthcheck.ListHealthCheckRequest
import cn.ctyun.ctapi.ctelb.listhealthcheck.ListHealthCheckResponseData
import cn.ctyun.ctapi.ctelb.listlistener.ListListenerRequest
import cn.ctyun.ctapi.ctelb.listlistener.ListListenerResponseData
import cn.ctyun.ctapi.ctelb.listloadbalancer.ListLoadbalancerRequest
import cn.ctyun.ctapi.ctelb.listloadbalancer.ListLoadbalancerResponseData
import cn.ctyun.ctapi.ctelb.listloadbalancer.ReturnObj
import cn.ctyun.ctapi.ctelb.listrule.ListRuleRequest
import cn.ctyun.ctapi.ctelb.listrule.ListRuleResponseData
import cn.ctyun.ctapi.ctelb.listtarget.ListTargetRequest
import cn.ctyun.ctapi.ctelb.listtarget.ListTargetResponseData
import cn.ctyun.ctapi.ctelb.listtargetgroup.ListTargetGroupRequest
import cn.ctyun.ctapi.ctelb.listtargetgroup.ListTargetGroupResponseData
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.UpsertCtyunLoadBalancerDescription
import com.netflix.spinnaker.clouddriver.ctyun.exception.CtyunOperationException
import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancerHealthCheck
import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancerListener
import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancerRule
import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancerTarget
import com.tencentcloudapi.clb.v20180317.models.*
import com.tencentcloudapi.common.exception.TencentCloudSDKException
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

@Component
@Slf4j
class CtyunLoadBalancerClient {
  private final DEFAULT_LIMIT = 100
  private final MAX_TRY_COUNT = 20
  private final MAX_RULE_TRY_COUNT = 40
  private final REQ_TRY_INTERVAL = 500  //MillSeconds
  private final DESCRIBE_TARGET_HEALTH_LIMIT = 30
  private final String endingPoint = "ctelb-global.ctapi.ctyun.cn"
  private Credential cred
  CtelbClient client
  private String regionId

  CtyunLoadBalancerClient(String accessKey, String securityKey, String region){
    cred = new Credential().withAk(accessKey).withSk(securityKey);
    client = new CtelbClient();
    client.init(cred, endingPoint);
    regionId=region
  }

  List<ReturnObj> getAllLoadBalancer() {
    List<ReturnObj> loadBalancerAll = []
    ListLoadbalancerRequest request = new ListLoadbalancerRequest()
      .withRegionID(regionId);
    CTResponse<ListLoadbalancerResponseData> response = client.listLoadbalancer(request);
    if(response.httpCode==200&&response.getData()!=null){
      ListLoadbalancerResponseData listLoadbalancerResponseData=response.getData()
      if(listLoadbalancerResponseData.getStatusCode()==800){
        if(listLoadbalancerResponseData.getReturnObj().size()>0){
          loadBalancerAll.addAll(listLoadbalancerResponseData.getReturnObj())
        }
      }else{
        log.error("查询负载均衡异常！错误码={}，错误信息={}",listLoadbalancerResponseData.getErrorCode(),listLoadbalancerResponseData.getMessage())
        throw new CtyunOperationException(listLoadbalancerResponseData.getMessage())
      }
    }else{
      log.error("请求负载均衡列表查询接口异常！错误信息={}",response.getMessage())
      throw new CtyunOperationException(response.getMessage())
    }
    loadBalancerAll
  }

  List<ReturnObj> getLoadBalancerByName(String name) {
    List<ReturnObj> loadBalancerAll = []
    try{
      ListLoadbalancerRequest request = new ListLoadbalancerRequest()
        .withRegionID(regionId).withName(name);
      CTResponse<ListLoadbalancerResponseData> response = client.listLoadbalancer(request);
      if(response.httpCode==200&&response.getData()!=null){
        ListLoadbalancerResponseData listLoadbalancerResponseData=response.getData()
        if(listLoadbalancerResponseData.getStatusCode()==800){
          if(listLoadbalancerResponseData.getReturnObj().size()>0){
            loadBalancerAll.addAll(listLoadbalancerResponseData.getReturnObj())
          }
        }else{
          log.error("查询负载均衡异常！错误码={}，错误信息={}",listLoadbalancerResponseData.getErrorCode(),listLoadbalancerResponseData.getMessage())
          throw new CtyunOperationException(listLoadbalancerResponseData.getMessage())
        }
      }else{
        log.error("负载均衡列表查询接口异常！错误信息={}",response.getMessage())
        throw new CtyunOperationException(response.getMessage())
      }

      return loadBalancerAll
    } catch (CtyunOperationException e) {
      log.error(e);
      throw new CtyunOperationException(e.toString())
    }
  }

  List<ReturnObj> getLoadBalancerById(String id) {
    List<ReturnObj> loadBalancerAll = []
    try{
      ListLoadbalancerRequest request = new ListLoadbalancerRequest()
        .withRegionID(regionId).withIDs(id);
      CTResponse<ListLoadbalancerResponseData> response = client.listLoadbalancer(request);
      if(response.httpCode==200&&response.getData()!=null){
        ListLoadbalancerResponseData listLoadbalancerResponseData=response.getData()
        if(listLoadbalancerResponseData.getStatusCode()==800){
          if(listLoadbalancerResponseData.getReturnObj().size()>0){
            loadBalancerAll.addAll(listLoadbalancerResponseData.getReturnObj())
          }
        }else{
          log.error("查询负载均衡异常！错误码={}，错误信息={},描述={}",listLoadbalancerResponseData.getErrorCode(),listLoadbalancerResponseData.getMessage(),listLoadbalancerResponseData.getDescription())
          throw new CtyunOperationException(listLoadbalancerResponseData.getMessage())
        }
      }else{
        log.error("负载均衡列表查询接口异常！错误信息={}",response.getMessage())
        throw new CtyunOperationException(response.getMessage())
      }
      return loadBalancerAll
    } catch (CtyunOperationException e) {
      log.error(e);
      throw new CtyunOperationException(e)
    }
  }

  List<String> createLoadBalancer(UpsertCtyunLoadBalancerDescription description) {
    List<String> ret = new ArrayList<String>();
    try{
      CreateLoadbalancerRequestBody body = new CreateLoadbalancerRequestBody()
        .withClientToken(UUID.randomUUID().toString())
        .withRegionID(regionId)
        .withSubnetID(description.getSubnetId())
        .withName(description.getLoadBalancerName())
        .withSlaName("elb.default")
      /*天翼云资源类型。internal：内网负载均衡，external：公网负载均衡*/ //OPEN:公网, INTERNAL:内网
      if(description.getLoadBalancerType()!=null){
        if("INTERNAL".equals(description.getLoadBalancerType())){
          body.withResourceType("internal")
        }else if("OPEN".equals(description.getLoadBalancerType())) {
          body.withResourceType("external")
          body.withEipID(description.eipId)
        }else {
          throw new CtyunOperationException("负载均衡类型LoadBalancerType数据错误")
        }
      }else{
        throw new CtyunOperationException("负载均衡类型LoadBalancerType不能为空")
      }
      if (description.vpcId!=null && description.vpcId.length() > 0) {
        body.withVpcID(description.vpcId);
      }
      if (description.projectId != null) {
        body.withProjectID(description.projectId.toString())
      }
      CreateLoadbalancerRequest request = new CreateLoadbalancerRequest().withBody(body);
      CTResponse<CreateLoadbalancerResponseData> response = client.createLoadbalancer(request);
      if(response.httpCode==200&&response.getData()!=null){
        CreateLoadbalancerResponseData createLoadbalancerResponseData=response.getData()
        if(createLoadbalancerResponseData.getStatusCode()==800){
          String lbid = createLoadbalancerResponseData.getReturnObj().getID();
          ret.add(lbid)
        }else{
          log.error("创建负载均衡报错！错误码={}，错误信息={},描述={}",createLoadbalancerResponseData.getErrorCode(),createLoadbalancerResponseData.getMessage(),createLoadbalancerResponseData.getDescription())
          throw new CtyunOperationException(createLoadbalancerResponseData.getMessage())
        }
      }else{
        log.error("创建负载均衡接口异常！错误信息={}",response.getMessage())
        throw new CtyunOperationException(response.getMessage())
      }

      return ret
    } catch (CtyunOperationException e) {
      log.error(e)
      throw new CtyunOperationException(e)
    }
  }

  /*String deleteLoadBalancerByIds(String[] loadBalancerIds) {
    try{
      DeleteLoadBalancerRequest req = new DeleteLoadBalancerRequest();
      req.setLoadBalancerIds(loadBalancerIds)
      DeleteLoadBalancerResponse resp = client.DeleteLoadBalancer(req);

      //DescribeTaskStatus is success
      for (def i = 0; i < MAX_TRY_COUNT; i++) {
        Thread.sleep(REQ_TRY_INTERVAL)
        DescribeTaskStatusRequest statusReq = new  DescribeTaskStatusRequest()
        statusReq.setTaskId(resp.getRequestId())
        DescribeTaskStatusResponse  statusResp = client.DescribeTaskStatus(statusReq)
        if (statusResp.getStatus() == 0) {   //task success
          return "success"
        }
      }
    } catch (TencentCloudSDKException e) {
      throw new CtyunOperationException(e.toString())
    }
  }*/

  String deleteLoadBalancerByIds(String loadBalancerIds) {
    String result;
    try{
      DeleteLoadbalancerRequestBody body = new DeleteLoadbalancerRequestBody()
        .withClientToken(UUID.randomUUID().toString())
        .withRegionID(regionId)
        .withID(loadBalancerIds);
      DeleteLoadbalancerRequest request = new DeleteLoadbalancerRequest().withBody(body);
      CTResponse<DeleteLoadbalancerResponseData> response = client.deleteLoadbalancer(request);
      if(response.httpCode==200&&response.getData()!=null){
        DeleteLoadbalancerResponseData deleteLoadbalancerResponseData=response.getData()
        if(deleteLoadbalancerResponseData.getStatusCode()==800){
          result = deleteLoadbalancerResponseData.getReturnObj()[0].getID();

        }else{
          log.error("删除负载均衡报错！错误码={}，错误信息={},描述={}",deleteLoadbalancerResponseData.getErrorCode(),deleteLoadbalancerResponseData.getMessage(),deleteLoadbalancerResponseData.getDescription())
          throw new CtyunOperationException(deleteLoadbalancerResponseData.getMessage())
        }
      }else{
        log.error("删除负载均衡接口异常！错误信息={}",response.getMessage())
        throw new CtyunOperationException(response.getMessage())
      }
      return result
    } catch (CtyunOperationException e) {
      log.error(e)
      throw new CtyunOperationException(e)
    }
  }

  List<cn.ctyun.ctapi.ctelb.listlistener.ReturnObj> getAllLBListener(String loadBalancerId) {
    List<cn.ctyun.ctapi.ctelb.listlistener.ReturnObj> listListenerAll = []
    try{
      ListListenerRequest request = new ListListenerRequest()
        .withLoadBalancerID(loadBalancerId)
        .withRegionID(regionId);
      CTResponse<ListListenerResponseData> response = client.listListener(request);
      if(response.httpCode==200&&response.getData()!=null){
        ListListenerResponseData listListenerResponseData=response.getData()
        if(listListenerResponseData.getStatusCode()==800){
          if(listListenerResponseData.getReturnObj().size()>0){
            listListenerAll.addAll(listListenerResponseData.getReturnObj())
          }
        }else{
          log.error("查询监听列表报错！错误码={}，错误信息={},描述={}",listListenerResponseData.getErrorCode(),listListenerResponseData.getMessage(),listListenerResponseData.getDescription())
          throw new CtyunOperationException(listListenerResponseData.getMessage())
        }
      }else{
        log.error("查询监听列表接口异常！错误信息={}",response.getMessage())
        throw new CtyunOperationException(response.getMessage())
      }

      return listListenerAll
    } catch (CtyunOperationException e) {
      throw new CtyunOperationException(e)
    }
  }

  /*List<Listener> getLBListenerById(String listenerId) {
    try{
      DescribeListenersRequest req = new DescribeListenersRequest();
      req.setLoadBalancerId(listenerId)
      DescribeListenersResponse resp = client.DescribeListeners(req);
      return resp.getListeners()
    } catch (TencentCloudSDKException e) {
      throw new CtyunOperationException(e.toString())
    }
  }*/

  List<cn.ctyun.ctapi.ctelb.listrule.ReturnObj> getAllRule(String loadBalancerId) {
    List<cn.ctyun.ctapi.ctelb.listrule.ReturnObj> ruleAll = []
    try{
      ListRuleRequest request = new ListRuleRequest()
        .withLoadBalancerID(loadBalancerId)
//                    .withIDs("listener-2py5fhdjsm")
        .withRegionID(regionId);
      CTResponse<ListRuleResponseData> response = client.listRule(request);
      if(response.httpCode==200&&response.getData()!=null){
        ListRuleResponseData listRuleResponseData=response.getData()
        if(listRuleResponseData.getStatusCode()==800){
          if(listRuleResponseData.getReturnObj().size()>0){
            ruleAll.addAll(listRuleResponseData.getReturnObj())
          }
        }else{
          log.error("查询转发规则列表报错！错误码={}，错误信息={}",listRuleResponseData.getErrorCode(),listRuleResponseData.getMessage())
          throw new CtyunOperationException(listRuleResponseData.getMessage())
        }
      }else{
        log.error("查询转发规则列表接口异常！错误信息={}",response.getMessage())
        throw new CtyunOperationException(response.getMessage())
      }

      return ruleAll
    } catch (CtyunOperationException e) {
      log.error(e);
      throw new CtyunOperationException(e)
    }
  }
  //创建健康检查
  String createHealthCheck(String name,String protocol,String[] httpCodes, CtyunLoadBalancerHealthCheck healthCheck) {
    String ret
    try{
      CreateHealthCheckRequestBody body = new CreateHealthCheckRequestBody()
        .withClientToken(UUID.randomUUID().toString())
        .withRegionID(regionId)
        .withName(name)
        .withProtocol(protocol)
        .withTimeout(healthCheck.timeOut)
        .withInterval(healthCheck.intervalTime)
        .withMaxRetry(healthCheck.unHealthNum)
        .withHttpUrlPath(healthCheck.httpCheckPath)
        .withHttpMethod(healthCheck.httpCheckMethod)
        .withHttpExpectedCodes(httpCodes);
      CreateHealthCheckRequest request = new CreateHealthCheckRequest().withBody(body);
      CTResponse<CreateHealthCheckResponseData> response = client.createHealthCheck(request);
      if(response.httpCode==200&&response.getData()!=null){
        CreateHealthCheckResponseData createHealthCheckResponseData=response.getData()
        if(createHealthCheckResponseData.getStatusCode()==800){
          ret = createHealthCheckResponseData.getReturnObj().getID();
        }else{
          log.error("创建健康检查报错！错误码={}，错误信息={},描述={}",createHealthCheckResponseData.getErrorCode(),createHealthCheckResponseData.getMessage(),createHealthCheckResponseData.getDescription())
          throw new CtyunOperationException(createHealthCheckResponseData.getMessage())
        }
      }else{
        log.error("创建健康检查接口异常！错误信息={}",response.getMessage())
        throw new CtyunOperationException(response.getMessage())
      }
      return ret
    }catch (CtyunOperationException e){
      log.error(e)
      throw new CtyunOperationException(e)
    }
  }

  //创建后端服务组
  List<String> createTargetGroup(String healthCheckId, String tgName, String loadBalancerId, CtyunLoadBalancerListener listener,String vpcId) {
    List<String> ret = new ArrayList<String>();
    try{
      CreateTargetGroupRequestBody body = new CreateTargetGroupRequestBody()
        .withClientToken(UUID.randomUUID().toString())
        .withRegionID(regionId)
        .withAlgorithm("wrr") //默认轮询
      if(tgName!=null&&tgName.size()>0){
        body.withName(tgName)
      }else{
        body.withName(listener.listenerName+"_tg")
      }
      if(vpcId!=null&&vpcId.size()>0){
        body.withVpcID(vpcId)
      }else{
        throw new CtyunOperationException("vpcId不能为空")
      }
      /*if (listener.protocol in ["TCP", "UDP"]) {   //tcp/udp 4 layer
        //会话保持信息，默认SOURCE_IP（源IP）
        SessionSticky sessionSticky = new SessionSticky()
          .withSessionStickyMode("SOURCE_IP")
        if(listener.sessionExpireTime>0){
          sessionSticky.withSourceIpTimeout(listener.sessionExpireTime)
        }else {
          sessionSticky.withSourceIpTimeout(1000)
        }
        body.withSessionSticky(sessionSticky)
      }*/
      //healthCheck
      if(healthCheckId){
        body.withHealthCheckID(healthCheckId)
      }

      CreateTargetGroupRequest request = new CreateTargetGroupRequest().withBody(body);
      CTResponse<CreateTargetGroupResponseData> response = client.createTargetGroup(request);
      if(response.httpCode==200&&response.getData()!=null){
        CreateTargetGroupResponseData createTargetGroupResponseData=response.getData()
        if(createTargetGroupResponseData.getStatusCode()==800){
          String tgid = createTargetGroupResponseData.getReturnObj()[0].getID();
          ret.add(tgid)
        }else{
          log.error("创建后端服务组报错！错误码={}，错误信息={},描述={}",createTargetGroupResponseData.getErrorCode(),createTargetGroupResponseData.getMessage(),createTargetGroupResponseData.getDescription())
          throw new CtyunOperationException(createTargetGroupResponseData.getMessage())
        }
      }else{
        log.error("创建后端服务组接口异常！错误信息={}",response.getMessage())
        throw new CtyunOperationException(response.getMessage())
      }
      return ret
    } catch (CtyunOperationException e) {
      log.error(e)
      throw new CtyunOperationException(e)
    }
  }


 List<String> createLBListener(String loadBalancerId, CtyunLoadBalancerListener listener,String vpcId) {
   List<String> ret = new ArrayList<String>();
   try{
     //创建健康检查
     def healthCheckId
     if (listener.protocol in ["TCP","UDP"] && listener.healthCheck != null) {
       healthCheckId = createHealthCheck(listener.listenerName+"_health",listener.protocol,null,listener.healthCheck)
     }

     //先创建后端服务组
     def targetGroupId = createTargetGroup(healthCheckId, listener.targetGroupName, loadBalancerId, listener, vpcId)[0]
     //创建监听
     TargetGroup targetGroup = new TargetGroup().withTargetGroupID(targetGroupId)
     TargetGroup[] tgs = [targetGroup]
     ForwardConfig forwardConfig = new ForwardConfig().withTargetGroups(tgs);
     DefaultAction defaultAction = new DefaultAction().withType("forward").withForwardConfig(forwardConfig);
     CreateListenerRequestBody body = new CreateListenerRequestBody()
       .withClientToken(UUID.randomUUID().toString())
       .withRegionID(regionId)
       .withLoadBalancerID(loadBalancerId)
       .withName(listener.listenerName)
       .withProtocol(listener.protocol)
       .withProtocolPort(listener.port)
       .withDefaultAction(defaultAction);
     if (listener.protocol in ["HTTPS"]) {
       //UNIDIRECTIONAL：单向认证，MUTUAL：双向认证
       if (listener.certificate.sslMode.equals("UNIDIRECTIONAL")){
         body.withCertificateID(listener.certificate.certId)
       }else if(listener.certificate.sslMode.equals("MUTUAL")){
         body.withCaEnabled(true)
         body.withClientCertificateID(listener.certificate.certCaId)
       }else{
         throw new CtyunOperationException("https认证方式错误")
       }
     }
     CreateListenerRequest request = new CreateListenerRequest().withBody(body);
     CTResponse<CreateListenerResponseData> response = client.createListener(request);
     if(response.httpCode==200&&response.getData()!=null){
       CreateListenerResponseData createListenerResponseData=response.getData()
       if(createListenerResponseData.getStatusCode()==800){
         String listenerId = createListenerResponseData.getReturnObj()[0].getID();
         ret.add(listenerId)
       }else{
         log.error("创建监听报错！错误码={}，错误信息={}",createListenerResponseData.getErrorCode(),createListenerResponseData.getMessage())
         throw new CtyunOperationException(createListenerResponseData.getMessage())
       }
     }else{
       log.error("创建监听接口异常！错误信息={}",response.getMessage())
       throw new CtyunOperationException(response.getMessage())
     }
     return ret
   } catch (CtyunOperationException e) {
     log.error(e)
     throw new CtyunOperationException(e)
   }
  }

  String deleteLBListenerById(String loadBalancerId, String listenerId) {
    String ret
    try{
      DeleteListenerRequestBody body = new DeleteListenerRequestBody()
        .withClientToken(UUID.randomUUID().toString())
        .withRegionID(regionId)
        .withID(listenerId);
      DeleteListenerRequest request = new DeleteListenerRequest().withBody(body);
      CTResponse<DeleteListenerResponseData> response = client.deleteListener(request);
      if(response.httpCode==200&&response.getData()!=null){
        DeleteListenerResponseData deleteListenerResponseData=response.getData()
        if(deleteListenerResponseData.getStatusCode()==800){
          ret = deleteListenerResponseData.getReturnObj()[0].getID();
        }else{
          log.error("删除监听报错！错误码={}，错误信息={},描述={}",deleteListenerResponseData.getErrorCode(),deleteListenerResponseData.getMessage(),deleteListenerResponseData.getDescription())
          throw new CtyunOperationException(deleteListenerResponseData.getMessage())
        }
      }else{
        log.error("删除监听接口异常！错误信息={}",response.getMessage())
        throw new CtyunOperationException(response.getMessage())
      }
      return ret

    } catch (TencentCloudSDKException e) {
      log.error(e)
      throw new CtyunOperationException(e)
    }
    return ""
  }

  String modifyListener(String loadBalancerId, CtyunLoadBalancerListener listener) {
    try{
      def isModify = false
      ModifyListenerRequest req = new ModifyListenerRequest();
      req.setLoadBalancerId(loadBalancerId)
      req.setListenerId(listener.listenerId)
      if (listener.healthCheck != null) {
        req.healthCheck = new HealthCheck(
          healthSwitch: listener.healthCheck.healthSwitch,
          timeOut: listener.healthCheck.timeOut,
          intervalTime: listener.healthCheck.intervalTime,
          healthNum: listener.healthCheck.healthNum,
          unHealthNum: listener.healthCheck.unHealthNum,
          httpCode: listener.healthCheck.httpCode,
          httpCheckPath: listener.healthCheck.httpCheckPath,
          httpCheckDomain: listener.healthCheck.httpCheckDomain,
          httpCheckMethod: listener.healthCheck.httpCheckMethod )
        isModify = true
      }
      if (!isModify) {
        return "no modify"
      }
      ModifyListenerResponse resp = client.ModifyListener(req);

      //DescribeTaskStatus is success
      for (def i = 0; i < MAX_TRY_COUNT; i++) {
        Thread.sleep(REQ_TRY_INTERVAL)
        DescribeTaskStatusRequest statusReq = new  DescribeTaskStatusRequest()
        statusReq.setTaskId(resp.getRequestId())
        DescribeTaskStatusResponse  statusResp = client.DescribeTaskStatus(statusReq)
        if (statusResp.getStatus() == 0) {   //task success
          return "success"
        }
      }
    } catch (TencentCloudSDKException e) {
      throw new CtyunOperationException(e.toString())
    }
    return ""
  }

  String registerTarget4Layer(String loadBalancerId, String listenerId, List<CtyunLoadBalancerTarget> targets) {
    try{
      RegisterTargetsRequest req = new RegisterTargetsRequest();
      req.setLoadBalancerId(loadBalancerId)
      req.setListenerId(listenerId)
      req.targets = targets.collect {
        return new Target(instanceId:it.instanceId, port:it.port, type:it.type, weight:it.weight)
      }
      RegisterTargetsResponse resp = client.RegisterTargets(req);

      //DescribeTaskStatus task is success
      def maxTryCount = targets.size() * MAX_TRY_COUNT
      for (def i = 0; i < maxTryCount; i++) {
        Thread.sleep(REQ_TRY_INTERVAL)
        DescribeTaskStatusRequest statusReq = new  DescribeTaskStatusRequest()
        statusReq.setTaskId(resp.getRequestId())
        DescribeTaskStatusResponse  statusResp = client.DescribeTaskStatus(statusReq)
        if (statusResp.getStatus() == 0) {   //task success
          //return resp.getRequestId()
          return "success"
        }
      }
    } catch (TencentCloudSDKException e) {
      throw new CtyunOperationException(e.toString())
    }
    return ""
  }

  String deRegisterTarget4Layer(String loadBalancerId, String listenerId, List<CtyunLoadBalancerTarget> targets) {
    try{
      targets.each {
        DeleteTargetRequestBody body = new DeleteTargetRequestBody()
          .withClientToken(UUID.randomUUID().toString())
          .withRegionID(regionId)
          .withID(it.targetId)
        DeleteTargetRequest request = new DeleteTargetRequest().withBody(body)
        CTResponse<DeleteTargetResponseData> response = client.deleteTarget(request)
        if(response.httpCode==200&&response.getData()!=null){
          DeleteTargetResponseData deleteTargetResponseData=response.getData()
          if(deleteTargetResponseData.getStatusCode()==800){
            String ret = deleteTargetResponseData.getReturnObj()[0].getID();
          }else{
            log.error("删除listenerTarget报错！错误码={}，错误信息={},描述={}",deleteTargetResponseData.getErrorCode(),deleteTargetResponseData.getMessage(),deleteTargetResponseData.getDescription())
            throw new CtyunOperationException(deleteTargetResponseData.getMessage())
          }
        }else{
          log.error("删除listenerTarget异常！错误信息={}",response.getMessage())
          throw new CtyunOperationException(response.getMessage())
        }
      }
      return "success"
    } catch (CtyunOperationException e) {
      log.error(e)
      throw new CtyunOperationException(e)
    }
    return ""
  }
  String createLBListenerRule(String loadBalancerId, String listenerId, CtyunLoadBalancerRule rule,String tgId) {
    try{
      String ruleId
      ServerNameConfig serverNameConfig = new ServerNameConfig()
      serverNameConfig.withServerName(rule.domain)
      UrlPathConfig urlPathConfig = new UrlPathConfig()
      urlPathConfig.withUrlPaths(rule.url)
      urlPathConfig.withMatchType("ABSOLUTE")

      Condition condition = new Condition()
      condition.withType("url_path")
      condition.withUrlPathConfig(urlPathConfig)

      Condition conditionServerName = new Condition()
      conditionServerName.withType("server_name")
      conditionServerName.withServerNameConfig(serverNameConfig)

      Condition[] conditions = [conditionServerName,condition]
      cn.ctyun.ctapi.ctelb.createrule.TargetGroup targetGroup = new cn.ctyun.ctapi.ctelb.createrule.TargetGroup()
      targetGroup.withTargetGroupID(tgId)
      targetGroup.withWeight(100)
      cn.ctyun.ctapi.ctelb.createrule.TargetGroup[] targetGroups = [targetGroup]
      cn.ctyun.ctapi.ctelb.createrule.ForwardConfig forwardConfig = new cn.ctyun.ctapi.ctelb.createrule.ForwardConfig()
      forwardConfig.withTargetGroups(targetGroups)
      Action action = new Action()
      action.withType("forward")
      action.withForwardConfig(forwardConfig)
      CreateRuleRequestBody body = new CreateRuleRequestBody()
        .withClientToken(UUID.randomUUID().toString())
        .withRegionID(regionId)
        .withListenerID(listenerId)
        .withAction(action)
        .withConditions(conditions)
      CreateRuleRequest request = new CreateRuleRequest().withBody(body)
      CTResponse<CreateRuleResponseData> response = client.createRule(request)
      if(response.httpCode==200&&response.getData()!=null){
        CreateRuleResponseData createRuleResponseData=response.getData()
        if(createRuleResponseData.getStatusCode()==800){
          ruleId = createRuleResponseData.getReturnObj()[0].getID()
        }else{
          log.error("创建转发规则报错！错误码={}，错误信息={},描述={}",createRuleResponseData.getErrorCode(),createRuleResponseData.getMessage(),createRuleResponseData.getDescription())
          throw new CtyunOperationException(createRuleResponseData.getMessage())
        }
      }else{
        log.error("创建转发规则异常！错误信息={}",response.getMessage())
        throw new CtyunOperationException(response.getMessage())
      }
      return ruleId
    } catch (CtyunOperationException e) {
      log.error(e)
      throw new CtyunOperationException(e)
    }
    return ""
  }

  String deleteLBListenerRules(String loadBalancerId, String listenerId, List<CtyunLoadBalancerRule> rules) {
    try{
      rules.each { rule ->
        DeleteRuleRequestBody body = new DeleteRuleRequestBody()
          .withClientToken(UUID.randomUUID().toString())
          .withRegionID(regionId)
          .withID(rule.locationId)
        DeleteRuleRequest request = new DeleteRuleRequest().withBody(body)
        CTResponse<DeleteRuleResponseData> response = client.deleteRule(request)
        if(response.httpCode==200&&response.getData()!=null){
          DeleteRuleResponseData deleteRuleResponseData=response.getData()
          if(deleteRuleResponseData.getStatusCode()==800){
            String ret = deleteRuleResponseData.getReturnObj().getID();
          }else{
            log.error("删除转发规则报错！错误码={}，错误信息={},描述={}",deleteRuleResponseData.getErrorCode(),deleteRuleResponseData.getMessage(),deleteRuleResponseData.getDescription())
            throw new CtyunOperationException(deleteRuleResponseData.getMessage())
          }
        }else{
          log.error("删除转发规则异常！错误信息={}",response.getMessage())
          throw new CtyunOperationException(response.getMessage())
        }
      }
      return "success"
    } catch (CtyunOperationException e) {
      throw new CtyunOperationException(e.toString())
    }
    return ""
  }

  String deleteLBListenerRule(String loadBalancerId, String listenerId, String locationId) {
    try{
      DeleteRuleRequest req = new DeleteRuleRequest();
      req.setLoadBalancerId(loadBalancerId)
      req.setListenerId(listenerId)
      req.setLocationIds(locationId)
      DeleteRuleResponse resp = client.DeleteRule(req);

      //DescribeTaskStatus task is success
      for (def i = 0; i < MAX_TRY_COUNT; i++) {
        Thread.sleep(REQ_TRY_INTERVAL)
        DescribeTaskStatusRequest statusReq = new  DescribeTaskStatusRequest()
        statusReq.setTaskId(resp.getRequestId())
        DescribeTaskStatusResponse  statusResp = client.DescribeTaskStatus(statusReq)
        if (statusResp.getStatus() == 0) {   //task success
          return "success"
        }
      }
    } catch (TencentCloudSDKException e) {
      throw new CtyunOperationException(e.toString())
    }
    return ""
  }

  String modifyLBListenerRule(String loadBalancerId, String listenerId, CtyunLoadBalancerRule rule) {
    try{
      def isModify = false
      ModifyRuleRequest req = new ModifyRuleRequest();
      req.setLoadBalancerId(loadBalancerId)
      req.setListenerId(listenerId)
      req.setLocationId(rule.locationId)

      if (rule.healthCheck != null) {
        req.healthCheck = new HealthCheck(
          healthSwitch: rule.healthCheck.healthSwitch,
          timeOut: rule.healthCheck.timeOut,
          intervalTime: rule.healthCheck.intervalTime,
          healthNum: rule.healthCheck.healthNum,
          unHealthNum: rule.healthCheck.unHealthNum,
          httpCode: rule.healthCheck.httpCode,
          httpCheckPath: rule.healthCheck.httpCheckPath,
          httpCheckDomain: rule.healthCheck.httpCheckDomain,
          httpCheckMethod: rule.healthCheck.httpCheckMethod )
        isModify = true
      }
      if (!isModify) {
        return "no modify"
      }

      ModifyRuleResponse resp = client.ModifyRule(req);

      //DescribeTaskStatus task is success
      for (def i = 0; i < MAX_RULE_TRY_COUNT; i++) {
        Thread.sleep(REQ_TRY_INTERVAL)
        DescribeTaskStatusRequest statusReq = new  DescribeTaskStatusRequest()
        statusReq.setTaskId(resp.getRequestId())
        DescribeTaskStatusResponse  statusResp = client.DescribeTaskStatus(statusReq)
        if (statusResp.getStatus() == 0) {   //task success
          return "success"
        }
      }
    } catch (TencentCloudSDKException e) {
      throw new CtyunOperationException(e.toString())
    }
    return ""
  }

  String registerTarget7Layer(String loadBalancerId, String listenerId, String domain, String url, List<CtyunLoadBalancerTarget> targets) {
    try{
      RegisterTargetsRequest req = new RegisterTargetsRequest();
      req.setLoadBalancerId(loadBalancerId)
      req.setListenerId(listenerId)
      req.setDomain(domain)
      req.setUrl(url)
      req.targets = targets.collect {
        return new Target(instanceId:it.instanceId, port:it.port, type:it.type, weight:it.weight)
      }
      RegisterTargetsResponse resp = client.RegisterTargets(req);

      //DescribeTaskStatus task is success
      def maxTryCount = targets.size()
      for (def i = 0; i < maxTryCount; i++) {
        Thread.sleep(REQ_TRY_INTERVAL)
        DescribeTaskStatusRequest statusReq = new  DescribeTaskStatusRequest()
        statusReq.setTaskId(resp.getRequestId())
        DescribeTaskStatusResponse  statusResp = client.DescribeTaskStatus(statusReq)
        if (statusResp.getStatus() == 0) {   //task success
          return "success"
        }
      }
    } catch (TencentCloudSDKException e) {
      throw new CtyunOperationException(e.toString())
    }
    return ""
  }

  String registerTarget7Layer(String loadBalancerId, String listenerId, String locationId, List<CtyunLoadBalancerTarget> targets) {
    try{
      RegisterTargetsRequest req = new RegisterTargetsRequest();
      req.setLoadBalancerId(loadBalancerId)
      req.setListenerId(listenerId)
      req.setLocationId(locationId)
      req.targets = targets.collect {
        return new Target(instanceId:it.instanceId, port:it.port, type:it.type, weight:it.weight)
      }
      RegisterTargetsResponse resp = client.RegisterTargets(req);

      //DescribeTaskStatus task is success
      def maxTryCount = targets.size()
      for (def i = 0; i < maxTryCount; i++) {
        Thread.sleep(REQ_TRY_INTERVAL)
        DescribeTaskStatusRequest statusReq = new  DescribeTaskStatusRequest()
        statusReq.setTaskId(resp.getRequestId())
        DescribeTaskStatusResponse  statusResp = client.DescribeTaskStatus(statusReq)
        if (statusResp.getStatus() == 0) {   //task success
          return "success"
        }
      }
    } catch (TencentCloudSDKException e) {
      throw new CtyunOperationException(e.toString())
    }
    return ""
  }

  String deRegisterTarget7Layer(String loadBalancerId, String listenerId, String locationId, List<CtyunLoadBalancerTarget> targets) {
    try{
      targets.each {
        DeleteTargetRequestBody body = new DeleteTargetRequestBody()
          .withClientToken(UUID.randomUUID().toString())
          .withRegionID(regionId)
          .withID(it.targetId)
        DeleteTargetRequest request = new DeleteTargetRequest().withBody(body)
        CTResponse<DeleteTargetResponseData> response = client.deleteTarget(request)
        if(response.httpCode==200&&response.getData()!=null){
          DeleteTargetResponseData deleteTargetResponseData=response.getData()
          if(deleteTargetResponseData.getStatusCode()==800){
            String ret = deleteTargetResponseData.getReturnObj()[0].getID();
          }else{
            log.error("删除ruleTarget报错！错误码={}，错误信息={},描述={}",deleteTargetResponseData.getErrorCode(),deleteTargetResponseData.getMessage(),deleteTargetResponseData.getDescription())
            throw new CtyunOperationException(deleteTargetResponseData.getMessage())
          }
        }else{
          log.error("删除ruleTarget异常！错误信息={}",response.getMessage())
          throw new CtyunOperationException(response.getMessage())
        }
      }
      return "success"
    } catch (CtyunOperationException e) {
      log.error(e)
      throw new CtyunOperationException(e)
    }
    return ""
  }

  List<cn.ctyun.ctapi.ctelb.listtarget.ReturnObj> getLBTargets(String targetGroupID) {
    List<cn.ctyun.ctapi.ctelb.listtarget.ReturnObj> targetAll = []
    try{
      ListTargetRequest request = new ListTargetRequest()
        .withTargetGroupID(targetGroupID)
        .withRegionID(regionId);
      CTResponse<ListTargetResponseData> response = client.listTarget(request);
      if(response.httpCode==200&&response.getData()!=null){
        ListTargetResponseData listTargetResponseData=response.getData()
        if(listTargetResponseData.getStatusCode()==800){
          if(listTargetResponseData.getReturnObj().size()>0){
            targetAll.addAll(listTargetResponseData.getReturnObj())
          }
        }else{
          log.error("查询后端服务列表报错！错误码={}，错误信息={}",listTargetResponseData.getErrorCode(),listTargetResponseData.getMessage())
          throw new CtyunOperationException(listTargetResponseData.getMessage())
        }
      }else{
        log.error("查询后端服务列表接口异常！错误信息={}",response.getMessage())
        throw new CtyunOperationException(response.getMessage())
      }
      return targetAll
    } catch (CtyunOperationException e) {
      log.error(e)
      throw new CtyunOperationException(e)
    }
  }

  //  0522new 查询targetGroup
  List<cn.ctyun.ctapi.ctelb.listhealthcheck.ReturnObj> getHealthcheckList(String healthcheckIDs) {
    List<cn.ctyun.ctapi.ctelb.listhealthcheck.ReturnObj> listHealthcheckAll = []
    try {
      ListHealthCheckRequest request = new ListHealthCheckRequest()
        .withIDs(healthcheckIDs)
        .withRegionID(regionId);
      CTResponse<ListHealthCheckResponseData> response = client.listHealthCheck(request);

      if(response.httpCode==200&&response.getData()!=null){
        ListHealthCheckResponseData listHealthCheckResponseData=response.getData()
        if(listHealthCheckResponseData.getStatusCode()==800){
          if(listHealthCheckResponseData.getReturnObj().size()>0){
            listHealthcheckAll.addAll(listHealthCheckResponseData.getReturnObj())
          }
        }else{
          log.error("查询健康检查列表报错！错误码={}，错误信息={}",listHealthCheckResponseData.getErrorCode(),listHealthCheckResponseData.getMessage())
          throw new CtyunOperationException(listHealthCheckResponseData.getMessage())
        }
      }else{
        log.error("查询健康检查列表接口异常！错误信息={}",response.getMessage())
        throw new CtyunOperationException(response.getMessage())
      }

      return listHealthcheckAll
    } catch (CtyunOperationException e) {
      throw new CtyunOperationException(e)
    }
  }
  //  0522new 查询targetGroup
  List<cn.ctyun.ctapi.ctelb.listtargetgroup.ReturnObj> getLBTargetGroupList(String targetGroupIDs) {
    List<cn.ctyun.ctapi.ctelb.listtargetgroup.ReturnObj> listTargetGroupAll = []
    try {
      ListTargetGroupRequest request = new ListTargetGroupRequest()
        .withIDs(targetGroupIDs)
        .withRegionID(regionId);
      CTResponse<ListTargetGroupResponseData> response = client.listTargetGroup(request);

      if(response.httpCode==200&&response.getData()!=null){
        ListTargetGroupResponseData listTargetGroupResponseData=response.getData()
        if(listTargetGroupResponseData.getStatusCode()==800){
          if(listTargetGroupResponseData.getReturnObj().size()>0){
            listTargetGroupAll.addAll(listTargetGroupResponseData.getReturnObj())
          }
        }else{
          log.error("查询后端服务组列表报错！错误码={}，错误信息={}",listTargetGroupResponseData.getErrorCode(),listTargetGroupResponseData.getMessage())
          throw new CtyunOperationException(listTargetGroupResponseData.getMessage())
        }
      }else{
        log.error("查询后端服务组列表接口异常！错误信息={}",response.getMessage())
        throw new CtyunOperationException(response.getMessage())
      }
      return listTargetGroupAll
    } catch (CtyunOperationException e) {
      throw new CtyunOperationException(e)
    }
  }

//  0522new targetGroupID
  List<cn.ctyun.ctapi.ctelb.listtarget.ReturnObj> getLBTargetList(String targetGroupID) {
    List<cn.ctyun.ctapi.ctelb.listtarget.ReturnObj> listTargetAll = []
    try {
      ListTargetRequest request = new ListTargetRequest()
        .withTargetGroupID(targetGroupID)
        .withRegionID(regionId);
      CTResponse<ListTargetResponseData> response = client.listTarget(request);

      if(response.httpCode==200&&response.getData()!=null){
        ListTargetResponseData listTargetResponseData=response.getData()
        if(listTargetResponseData.getStatusCode()==800){
          if(listTargetResponseData.getReturnObj().size()>0){
            listTargetAll.addAll(listTargetResponseData.getReturnObj())
          }
        }else{
          log.error("查询后端服务列表报错！错误码={}，错误信息={}",listTargetResponseData.getErrorCode(),listTargetResponseData.getMessage())
          throw new CtyunOperationException(listTargetResponseData.getMessage())
        }
      }else{
        log.error("查询后端服务列表接口异常！错误信息={}",response.getMessage())
        throw new CtyunOperationException(response.getMessage())
      }

      return listTargetAll
    } catch (CtyunOperationException e) {
      throw new CtyunOperationException(e)
    }
  }

  List<ListenerBackend> getLBTargetList(String loadBalancerId, List<String> listenerIds) {
    try {
      DescribeTargetsRequest req = new DescribeTargetsRequest();
      req.setLoadBalancerId(loadBalancerId)
      req.listenerIds = listenerIds.collect {
        return new String(it)
      }
      DescribeTargetsResponse resp = client.DescribeTargets(req);
      return resp.getListeners()
    } catch (CtyunOperationException e) {
      throw new CtyunOperationException(e.toString())
    }
  }

  String setLBSecurityGroups(String loadBalancerId, List<String> securityGroups) {
    try {
      SetLoadBalancerSecurityGroupsRequest req = new SetLoadBalancerSecurityGroupsRequest()
      req.setLoadBalancerId(loadBalancerId)
      req.securityGroups = securityGroups.collect {
        return new String(it)
      }
      SetLoadBalancerSecurityGroupsResponse resp = client.SetLoadBalancerSecurityGroups(req)
      return "success"
    } catch (TencentCloudSDKException e) {
      throw new CtyunOperationException(e.toString())
    }
  }

  /*List<LoadBalancerHealth> getLBTargetHealth(List<String> loadBalancerIds) {
    def loadBalancerHealths = []
    try {
      *//*DescribeTargetHealthRequest req = new DescribeTargetHealthRequest()
      def totalCount = loadBalancerIds.size()
      def reqCount = totalCount
      def startIndex = 0
      def endIndex = DESCRIBE_TARGET_HEALTH_LIMIT
      while(reqCount > 0) {
        if (endIndex > totalCount) {
          endIndex = totalCount
        }
        def batchIds = loadBalancerIds[startIndex..(endIndex-1)]
        req.loadBalancerIds = batchIds.collect {
          return new String(it)
        }
        DescribeTargetHealthResponse resp = client.DescribeTargetHealth(req)
        loadBalancerHealths.addAll(resp.getLoadBalancers())
        reqCount -= DESCRIBE_TARGET_HEALTH_LIMIT
        startIndex += DESCRIBE_TARGET_HEALTH_LIMIT
        endIndex = startIndex + DESCRIBE_TARGET_HEALTH_LIMIT
      }*//*
      return loadBalancerHealths
    } catch (TencentCloudSDKException e) {
      throw new CtyunOperationException(e.toString())
    }
  }*/

}
