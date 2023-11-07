package com.netflix.spinnaker.clouddriver.ctyun.client

import cn.ctyun.ctapi.cteip.CteipClient
import cn.ctyun.ctapi.cteip.newlist.Eip
import cn.ctyun.ctapi.cteip.newlist.NewListEipRequest
import cn.ctyun.ctapi.cteip.newlist.NewListEipRequestBody
import cn.ctyun.ctapi.cteip.newlist.NewListEipResponseData
import cn.ctyun.ctapi.ctvpc.createsecuritygroup.CreateSecurityGroupRequest
import cn.ctyun.ctapi.ctvpc.createsecuritygroup.CreateSecurityGroupRequestBody
import cn.ctyun.ctapi.ctvpc.createsecuritygroup.CreateSecurityGroupResponseData
import cn.ctyun.ctapi.ctvpc.createsgengressrule.CreateSgEngressRule
import cn.ctyun.ctapi.ctvpc.createsgengressrule.CreateSgEngressRuleRequest
import cn.ctyun.ctapi.ctvpc.createsgengressrule.CreateSgEngressRuleRequestBody
import cn.ctyun.ctapi.ctvpc.createsgengressrule.CreateSgEngressRuleResponseData
import cn.ctyun.ctapi.ctvpc.createsgingressrule.CreateSgIngressRule
import cn.ctyun.ctapi.ctvpc.createsgingressrule.CreateSgIngressRuleRequest
import cn.ctyun.ctapi.ctvpc.createsgingressrule.CreateSgIngressRuleRequestBody
import cn.ctyun.ctapi.ctvpc.createsgingressrule.CreateSgIngressRuleResponseData
import cn.ctyun.ctapi.ctvpc.deletesecuritygroup.DeleteSecurityGroupRequest
import cn.ctyun.ctapi.ctvpc.deletesecuritygroup.DeleteSecurityGroupRequestBody
import cn.ctyun.ctapi.ctvpc.deletesecuritygroup.DeleteSecurityGroupResponseData
import cn.ctyun.ctapi.ctvpc.listsecuritygroup.ListSecurityGroupRequest
import cn.ctyun.ctapi.ctvpc.listsecuritygroup.ListSecurityGroupResponseData
import cn.ctyun.ctapi.ctvpc.listsecuritygroup.ReturnObj
import cn.ctyun.ctapi.ctvpc.listsubnet.ListSubNetRequest
import cn.ctyun.ctapi.ctvpc.listsubnet.ListSubNetResponseData
import cn.ctyun.ctapi.ctvpc.listsubnet.ReturnSubNetObject
import cn.ctyun.ctapi.ctvpc.listvpc.ListVpcRequest
import cn.ctyun.ctapi.ctvpc.listvpc.ListVpcResponseData
import cn.ctyun.ctapi.ctvpc.listvpc.ReturnVpcObject
import cn.ctyun.ctapi.ctvpc.revokesgingressrule.RevokeSgIngressRuleRequest
import cn.ctyun.ctapi.ctvpc.revokesgingressrule.RevokeSgIngressRuleRequestBody
import cn.ctyun.ctapi.ctvpc.revokesgingressrule.RevokeSgIngressRuleResponseData
import cn.ctyun.ctapi.ctvpc.showsecuritygroup.SecurityGroupRuleList
import cn.ctyun.ctapi.ctvpc.showsecuritygroup.ShowSecurityGroupRequest
import cn.ctyun.ctapi.ctvpc.showsecuritygroup.ShowSecurityGroupResponseData
import com.alibaba.fastjson.JSONObject
import com.netflix.spinnaker.clouddriver.ctyun.exception.CtyunOperationException
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunSecurityGroupRule
import com.tencentcloudapi.common.exception.TencentCloudSDKException
import cn.ctyun.ctapi.Credential;
import cn.ctyun.ctapi.CTResponse;
import cn.ctyun.ctapi.ctvpc.CtvpcClient;
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

@Component
@Slf4j
class CtyunVirtualPrivateCloudClient {
  private final int DEFAULT_LIMIT = 50
  private Credential cred
  CtvpcClient client
  private final String endingPoint = "ctvpc-global.ctapi.ctyun.cn"
  private String regionId

  CtyunVirtualPrivateCloudClient(String accessKey, String securityKey, String region){
    cred = new Credential().withAk(accessKey).withSk(securityKey);
    client = new CtvpcClient();
    client.init(cred, endingPoint);
    regionId=region
  }
  List<ReturnObj.SecurityGroups> getSecurityGroupsAll() {
    log.info("getSecurityGroupsAll--获取所有安全组数据--start")
    List<ReturnObj.SecurityGroups> securityGroupAll = []
    try {
      int pageNumber=1;
      int totalCount = DEFAULT_LIMIT
      int getCount = DEFAULT_LIMIT
      while(totalCount==getCount){
        try {
          long startTime=System.currentTimeMillis();
          ListSecurityGroupRequest request = new ListSecurityGroupRequest().withRegionID(regionId).withPageNo(pageNumber).withPageSize(DEFAULT_LIMIT);
          CTResponse<ListSecurityGroupResponseData> response = client.listSecurityGroup(request);
          if (response.httpCode == 200 && response.getData() != null) {
            ListSecurityGroupResponseData listSecurityGroupResponseData = response.getData()
            if (listSecurityGroupResponseData.getStatusCode() == 800) {
              if (listSecurityGroupResponseData.getReturnObj() == null || listSecurityGroupResponseData.getReturnObj().getSecurityGroups() == null) {
                log.error("getSecurityGroupsAll--获取所有安全组数据--pageNum={} 返回结果为空！listSecurityGroupResponseData={}",pageNumber, JSONObject.toJSONString(listSecurityGroupResponseData))
                return null;
              }
              if (listSecurityGroupResponseData.getReturnObj().getSecurityGroups().size() > 0) {
                securityGroupAll.addAll(listSecurityGroupResponseData.getReturnObj().getSecurityGroups())
              }

              getCount = listSecurityGroupResponseData.getReturnObj().getSecurityGroups().size();
              log.info("getSecurityGroupsAll--获取所有安全组数据--成功！pageNum={} 用时={}",pageNumber,(System.currentTimeMillis()-startTime));
            } else {
              log.error("getSecurityGroupsAll--获取所有安全组数据--非800！pageNum={} 用时={},错误码={}，错误信息={}", pageNumber,(System.currentTimeMillis()-startTime), listSecurityGroupResponseData.getStatusCode(), listSecurityGroupResponseData.getMessage())
              return null;
            }
          } else {
            log.error("getSecurityGroupsAll--获取所有安全组数据--非200！pageNum={} message={}" , pageNumber,response.getMessage())
            return null;
          }
        }catch (Exception e) {
          log.error("getSecurityGroupsAll--第{}次 获取所有安全组数据--Exception",pageNumber,e)
          return null;
        }
        pageNumber++;
        sleep(500)
      }
      log.info("getSecurityGroupsAll--获取所有安全组数据--end,size={}",securityGroupAll.size())
      return securityGroupAll
    } catch (Exception e) {
      log.error("getSecurityGroupsAll--获取所有安全组数据--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
  //创建安全组
  String createSecurityGroup(String vpcId,String groupName, String groupDesc) {
    log.info("createSecurityGroup--创建安全组--start--vpcId--{}--groupName--{},groupDesc--{}",vpcId,groupName,groupDesc)
    try {
      CreateSecurityGroupRequestBody body = new CreateSecurityGroupRequestBody().withClientToken(UUID.randomUUID().toString()).withRegionID(this.regionId).withVpcID(vpcId).withName(groupName).withDescription(groupDesc);
      CreateSecurityGroupRequest request = new CreateSecurityGroupRequest().withBody(body);
      CTResponse<CreateSecurityGroupResponseData> response = client.createSecurityGroup(request);
      if(response.httpCode==200&&response.getData()!=null){
        CreateSecurityGroupResponseData createSecurityGroupResponseData=response.getData()
        if(createSecurityGroupResponseData.getStatusCode()==800){
          log.info("createSecurityGroup--创建安全组--end！{}", JSONObject.toJSONString(createSecurityGroupResponseData.getReturnObj()))
          return createSecurityGroupResponseData.getReturnObj().getSecurityGroupID()
        }else{
          log.error("createSecurityGroup--创建安全组--非800！{} {}",createSecurityGroupResponseData.getStatusCode(),createSecurityGroupResponseData.getMessage())
          throw new CtyunOperationException("createSecurityGroup--创建安全组--非800！"+createSecurityGroupResponseData.getStatusCode()+" "+createSecurityGroupResponseData.getMessage())
        }
      }else{
        log.error("createSecurityGroup--创建安全组--非200！message={}",response.getMessage())
        throw new CtyunOperationException("createSecurityGroup--创建安全组--非200！httpCode="+response.httpCode)
      }
    } catch (Exception e) {
      log.error("createSecurityGroup--创建安全组--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
  //创建安全组访问策略
  String createSecurityGroupRules(String groupId, List<CtyunSecurityGroupRule> inRules,
                                  List<CtyunSecurityGroupRule> outRules) {
    log.info("createSecurityGroupRules--创建安全组访问策略--start--groupId--{}--inRules--{},outRules--{}",groupId,inRules,outRules)

    try{
        //入站规则
        if(inRules!=null&&inRules.size()>0){
          List inputRules=new ArrayList<>();
          for(int i=0;i<inRules.size();i++){
            def inputRule=inRules.get(i);
            CreateSgIngressRule rule=new CreateSgIngressRule();
            rule.setDirection("ingress");
            rule.setEthertype(inputRule.getEthertype())
            if(inputRule.getEthertype()==null){
              rule.setEthertype("IPv4")
            }
            rule.setAction(inputRule.getAction()!=null?inputRule.getAction().toLowerCase():null);
            rule.setProtocol(inputRule.getProtocol()!=null?inputRule.getProtocol().toUpperCase():null);
            rule.setDestCidrIp(inputRule.getCidrBlock());

            rule.setPriority(i+1);
            rule.setRange(inputRule.getPort());
            inputRules.add(rule);
          }
          if(inputRules.size()>0){
            CreateSgIngressRuleRequestBody body = new CreateSgIngressRuleRequestBody().withRegionID(this.regionId)
              .withSecurityGroupID(groupId).withClientToken(UUID.randomUUID().toString()).withSecurityGroupRules(inputRules);
            CreateSgIngressRuleRequest request = new CreateSgIngressRuleRequest().withBody(body);
            CTResponse<CreateSgIngressRuleResponseData> response = client.createSgIngressRule(request);
            if(response.httpCode==200&&response.getData()!=null){
              CreateSgIngressRuleResponseData createSgIngressRuleResponseData=response.getData()
              if(createSgIngressRuleResponseData.getStatusCode()==800){
                log.info("createSecurityGroup--创建安全组访问策略--入站规则--end！{}", JSONObject.toJSONString(createSgIngressRuleResponseData))
              }else{
                log.error("createSecurityGroup--创建安全组访问策略--入站规则--非800！{} {}",createSgIngressRuleResponseData.getStatusCode(),createSgIngressRuleResponseData.getMessage())
                throw new CtyunOperationException("createSecurityGroup--创建安全组访问策略--入站规则--非800！"+createSgIngressRuleResponseData.getStatusCode()+" "+createSgIngressRuleResponseData.getMessage())
              }
            }else{
              log.error("createSecurityGroup--创建安全组访问策略--入站规则--非200！{}",response.getMessage())
              throw new CtyunOperationException("createSecurityGroup--创建安全组访问策略--入站规则--非200！httpCode="+response.httpCode)
            }
          }
        }
        //出站规则
        if(outRules!=null&&outRules.size()>0){
          List outputRules=new ArrayList<>();
          for(int i=0;i<outRules.size();i++){
            def outRule=outRules.get(i);
            CreateSgEngressRule rule=new CreateSgEngressRule();
            rule.setDirection("egress");
            rule.setEthertype(outRule.getEthertype())
            if(outRule.getEthertype()==null){
              rule.setEthertype("IPv4")
            }
            rule.setAction(outRule.getAction()!=null?outRule.getAction().toLowerCase():null);
            rule.setProtocol(outRule.getProtocol()!=null?outRule.getProtocol().toUpperCase():null);
            rule.setDestCidrIp(outRule.getCidrBlock());

            rule.setPriority(i+1);
            rule.setRange(outRule.getPort());
            outputRules.add(rule);
          }
          if(outputRules.size()>0){
            CreateSgEngressRuleRequestBody body = new CreateSgEngressRuleRequestBody().withRegionID(this.regionId)
              .withSecurityGroupID(groupId).withClientToken(UUID.randomUUID().toString()).withSecurityGroupRules(outputRules);
            CreateSgEngressRuleRequest request = new CreateSgEngressRuleRequest().withBody(body);
            CTResponse<CreateSgEngressRuleResponseData> response = client.createSgEngressRule(request);
            if(response.httpCode==200&&response.getData()!=null){
              CreateSgEngressRuleResponseData createSgEngressRuleResponseData=response.getData()
              if(createSgEngressRuleResponseData.getStatusCode()==800){
                log.info("createSecurityGroup--创建安全组访问策略--出站规则--end！{}", JSONObject.toJSONString(createSgEngressRuleResponseData))
              }else{
                log.error("createSecurityGroup--创建安全组访问策略--出站规则--非800！{} {}",createSgEngressRuleResponseData.getStatusCode(),createSgEngressRuleResponseData.getMessage())
                throw new CtyunOperationException("createSecurityGroup--创建安全组访问策略--出站规则--非800！"+createSgEngressRuleResponseData.getStatusCode()+" "+createSgEngressRuleResponseData.getMessage())
              }
            }else{
              log.error("createSecurityGroup--创建安全组访问策略--出站规则--非200！{}",response.getMessage())
              throw new CtyunOperationException("createSecurityGroup--创建安全组访问策略--出站规则--非200！httpCode="+response.httpCode)
            }
          }
        }

    } catch (Exception e) {
      log.error("createSecurityGroup--创建安全组访问策略--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
    return ""
  }
  //删除入站规则
  String deleteSecurityGroupInRules(String groupId, List<CtyunSecurityGroupRule> inRules) {
    log.info("deleteSecurityGroupInRules--删除入站规则--start--groupId--{}--inRules--{}",groupId,inRules)
    try {
      if(inRules!=null&&inRules.size()>0){
        for(int i=0;i<inRules.size();i++){
          CtyunSecurityGroupRule inRule=inRules.get(i);
          RevokeSgIngressRuleRequestBody body = new RevokeSgIngressRuleRequestBody().withRegionID(this.regionId)
            .withSecurityGroupID(groupId).withSecurityGroupRuleID(inRule.getId())
            .withClientToken(UUID.randomUUID().toString());
          RevokeSgIngressRuleRequest request = new RevokeSgIngressRuleRequest().withBody(body);
          CTResponse<RevokeSgIngressRuleResponseData> response = client.revokeSgIngressRule(request);
          if(response.httpCode==200&&response.getData()!=null){
            RevokeSgIngressRuleResponseData revokeSgIngressRuleResponseData=response.getData()
            if(revokeSgIngressRuleResponseData.getStatusCode()==800){
              log.info("deleteSecurityGroupInRules--删除入站规则--end！{}", JSONObject.toJSONString(revokeSgIngressRuleResponseData))
            }else{
              log.error("deleteSecurityGroupInRules--删除入站规则--非800！{} {}",revokeSgIngressRuleResponseData.getStatusCode(),revokeSgIngressRuleResponseData.getMessage())
              throw new CtyunOperationException("deleteSecurityGroupInRules--删除入站规则--非800！"+revokeSgIngressRuleResponseData.getStatusCode()+" "+revokeSgIngressRuleResponseData.getMessage())
            }
          }else{
            log.error("deleteSecurityGroupInRules--删除入站规则--非200！{}",response.getMessage())
            throw new CtyunOperationException("deleteSecurityGroupInRules--删除入站规则--非200！httpCode="+response.httpCode)
          }
        }
      }
    } catch (Exception e) {
      log.error("deleteSecurityGroupInRules--删除入站规则--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
    return ""
  }
  //通过安全组id获取详细信息
  cn.ctyun.ctapi.ctvpc.showsecuritygroup.ReturnObj getSecurityGroupById(String securityGroupId) {
    log.info("getSecurityGroupById--通过安全组id获取详细信息--start--securityGroupId--{}",securityGroupId)
    try {

      ShowSecurityGroupRequest request = new ShowSecurityGroupRequest().withRegionID(this.regionId).withSecurityGroupID(securityGroupId);
      CTResponse<ShowSecurityGroupResponseData> response = client.showSecurityGroup(request);
      if(response.httpCode==200&&response.getData()!=null){
        ShowSecurityGroupResponseData showSecurityGroupResponseData=response.getData()
        if(showSecurityGroupResponseData.getStatusCode()==800){
          log.info("getSecurityGroupById--通过安全组id获取详细信息--end,{}",showSecurityGroupResponseData.getReturnObj())
          return showSecurityGroupResponseData.getReturnObj()
        }else{
          log.error("getSecurityGroupById--通过安全组id获取详细信息--非800！错误码={}，错误信息={}",showSecurityGroupResponseData.getStatusCode(),showSecurityGroupResponseData.getMessage())
          return null;
        }
      }else{
        log.error("getSecurityGroupById--通过安全组id获取详细信息--非200！{}",response.getMessage())
        return null;
      }

    } catch (Exception e) {
      log.error("getSecurityGroupById--通过安全组id获取详细信息--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
//删除安全组
  void deleteSecurityGroup(String securityGroupId) {
    log.info("deleteSecurityGroup--删除安全组--start--securityGroupId--{}",securityGroupId)
    try {
      DeleteSecurityGroupRequestBody body = new DeleteSecurityGroupRequestBody().withClientToken(UUID.randomUUID().toString()).withRegionID(this.regionId)
        .withSecurityGroupID(securityGroupId);
      DeleteSecurityGroupRequest request = new DeleteSecurityGroupRequest().withBody(body);
      CTResponse<DeleteSecurityGroupResponseData> response = client.deleteSecurityGroup(request);
      if(response.httpCode==200&&response.getData()!=null){
        DeleteSecurityGroupResponseData deleteSecurityGroupResponseData=response.getData()
        if(deleteSecurityGroupResponseData.getStatusCode()==800){
          log.info("deleteSecurityGroup--删除安全组--end！{}", JSONObject.toJSONString(deleteSecurityGroupResponseData))
        }else{
          log.error("deleteSecurityGroup--删除安全组--非800！{} {}",deleteSecurityGroupResponseData.getStatusCode(),deleteSecurityGroupResponseData.getMessage())
          throw new CtyunOperationException("deleteSecurityGroup--删除安全组--非800！"+deleteSecurityGroupResponseData.getStatusCode()+" "+deleteSecurityGroupResponseData.getMessage())
        }
      }else{
        log.error("deleteSecurityGroup--删除安全组--非200！{}",response.getMessage())
        throw new CtyunOperationException("deleteSecurityGroup--删除安全组--非200！httpCode="+response.httpCode)
      }
    } catch (Exception e) {
      log.error("deleteSecurityGroup--删除安全组--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
  //通过安全组id获取安全组策略信息
  List<SecurityGroupRuleList> getSecurityGroupPolicies(String securityGroupId) {
    log.info("getSecurityGroupPolicies--通过安全组id获取安全组策略信息--start--securityGroupId--{}",securityGroupId)
    try{
      cn.ctyun.ctapi.ctvpc.showsecuritygroup.ReturnObj returnObj=this.getSecurityGroupById(securityGroupId);
      if(returnObj!=null&&returnObj.getSecurityGroupRuleList()!=null){
        log.info("getSecurityGroupPolicies--通过安全组id获取安全组策略信息--end！size={}",returnObj.getSecurityGroupRuleList().size())
        return returnObj.getSecurityGroupRuleList()
      }
    } catch (Exception e) {
      log.error("getSecurityGroupPolicies--通过安全组id获取安全组策略信息--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
    return null;
  }
//获取所有网络信息
  List<ReturnVpcObject> getNetworksAll() {
    log.info("getNetworksAll--获取所有网络信息--start")
    List<ReturnVpcObject> networkAll =[]
    try{
      int pageNumber=1;
      int totalCount = DEFAULT_LIMIT
      int getCount = DEFAULT_LIMIT
      while(totalCount==getCount){
        try {
          long startTime=System.currentTimeMillis();
          ListVpcRequest request = new ListVpcRequest().withRegionID(regionId).withPageNo(pageNumber).withPageSize(DEFAULT_LIMIT);
          CTResponse<ListVpcResponseData> response = client.listVpc(request);
          if (response.httpCode == 200 && response.getData() != null) {
            ListVpcResponseData listVpcResponseData = response.getData()
            if (listVpcResponseData.getStatusCode() == 800) {
              if (listVpcResponseData.getReturnObj() == null || listVpcResponseData.getReturnObj().getVpcs() == null) {
                log.error("getNetworksAll--获取所有网络信息--pageNum={} 返回结果为空！listVpcResponseData={}",pageNumber, JSONObject.toJSONString(listVpcResponseData))
                return null;
              }
              if (listVpcResponseData.getReturnObj().getVpcs().size() > 0) {
                networkAll.addAll(listVpcResponseData.getReturnObj().getVpcs())
              }
              getCount = listVpcResponseData.getReturnObj().getVpcs().size();
              log.info("getNetworksAll--获取所有网络信息--成功！pageNum={} 用时={}",pageNumber,(System.currentTimeMillis()-startTime));
            } else {
              log.error("getNetworksAll--获取所有网络信息--非800！pageNum={} 用时={},错误码={}，错误信息={}", pageNumber,(System.currentTimeMillis()-startTime), listVpcResponseData.getStatusCode(), listVpcResponseData.getMessage())
              return null;
            }
          } else {
            log.error("getNetworksAll--获取所有网络信息--非200！pageNum={} message={}",pageNumber, response.getMessage())
            return null;
          }
        }catch (Exception e) {
          log.error("getNetworksAll--第{}次 获取所有网络信息--Exception",pageNumber,e)
          return null;
        }
        pageNumber++;
        sleep(500)
      }
      log.info("getNetworksAll--获取所有网络信息--end,size={}",networkAll.size())
      return networkAll
    } catch (TencentCloudSDKException e) {
      log.error("getNetworksAll--获取所有网络信息--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
//通过vpcId获取子网信息
  List<ReturnSubNetObject> getSubnetsAll(String vpcId) {
    log.info("getSubnetsAll--通过vpcId获取子网信息--start--vpcId--{}",vpcId)
    List<ReturnSubNetObject> subnetAll = []
    try{
      int pageNumber=1;
      int totalCount = DEFAULT_LIMIT
      int getCount = DEFAULT_LIMIT
      while(totalCount==getCount){
        try {
          long startTime=System.currentTimeMillis();
          ListSubNetRequest request = new ListSubNetRequest().withRegionID(regionId).withVpcID(vpcId).withPageNo(pageNumber).withPageSize(DEFAULT_LIMIT);
          CTResponse<ListSubNetResponseData> response = client.listSubnet(request);
          if (response.httpCode == 200 && response.getData() != null) {
            ListSubNetResponseData listSubNetResponseData = response.getData()
            if (listSubNetResponseData.getStatusCode() == 800) {
              if (listSubNetResponseData.getReturnObj() == null || listSubNetResponseData.getReturnObj().getSubnets() == null) {
                log.error("getSubnetsAll--通过vpcId获取子网信息--pageNum={} 返回结果为空！listSubNetResponseData=={}",pageNumber, JSONObject.toJSONString(listSubNetResponseData))
                return null;
              }
              if (listSubNetResponseData.getReturnObj().getSubnets().size() > 0) {
                subnetAll.addAll(listSubNetResponseData.getReturnObj().getSubnets())
              }

              getCount = listSubNetResponseData.getReturnObj().getSubnets().size();
              log.info("getSubnetsAll--通过vpcId获取子网信息--成功！pageNum={} 用时={}",pageNumber,(System.currentTimeMillis()-startTime));
            } else {
              log.error("getSubnetsAll--通过vpcId获取子网信息--非800！pageNum={} 用时={},错误码={}，错误信息={}", pageNumber,(System.currentTimeMillis()-startTime), listSubNetResponseData.getStatusCode(), listSubNetResponseData.getMessage())
              return null;
            }
          } else {
            log.error("getSubnetsAll--通过vpcId获取子网信息--非200！pageNum={} message={}", pageNumber, response.getMessage())
            return null;
          }
        }catch (Exception e) {
          log.error("getSubnetsAll--第{}次 通过vpcId获取子网信息--Exception",pageNumber,e)
          return null;
        }
        pageNumber++;
        sleep(500)
      }
      log.info("getSubnetsAll--通过vpcId获取子网信息--end,size={}",subnetAll.size())
      return subnetAll
    } catch (TencentCloudSDKException e) {
      log.error("getSubnetsAll--通过vpcId获取子网信息--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }

  //获取未绑定的弹性ip信息
  List<Eip> getEipsDownAll() {
    log.info("getEipsDownAll--获取eip信息--start--")
    CteipClient eipClient = new CteipClient();
    eipClient.init(cred, endingPoint);
    List<Eip> eipAll = []
    try{
      int pageNumber=1;
      int totalCount = DEFAULT_LIMIT
      int getCount = DEFAULT_LIMIT
      while(totalCount==getCount){
        try {
          long startTime=System.currentTimeMillis();
          NewListEipRequestBody body = new NewListEipRequestBody()
            .withClientToken(UUID.randomUUID().toString())
            .withRegionID(regionId)
            .withPage(pageNumber)
            .withPageSize(DEFAULT_LIMIT)
            .withStatus("DOWN");
          NewListEipRequest request = new NewListEipRequest().withBody(body);
          CTResponse<NewListEipResponseData> response = eipClient.newListEip(request);
          if (response.httpCode == 200 && response.getData() != null) {
            NewListEipResponseData newListEipResponseData = response.getData()
            if (newListEipResponseData.getStatusCode() == 800) {
              if (newListEipResponseData.getReturnObj() == null || newListEipResponseData.getReturnObj().getEips() == null) {
                log.error("getEipsDownAll--获取未绑定eip信息--pageNum={} 返回结果为空！newListEipResponseData={}",pageNumber, JSONObject.toJSONString(newListEipResponseData))
                return null;
              }
              if (newListEipResponseData.getReturnObj().getEips().size() > 0) {
                eipAll.addAll(newListEipResponseData.getReturnObj().getEips())
              }
              getCount = newListEipResponseData.getReturnObj().getEips().size();
              log.info("getEipsDownAll--获取未绑定eip信息--成功！pageNum={} 用时={}",pageNumber,(System.currentTimeMillis()-startTime));
            } else {
              log.error("getEipsDownAll--获取未绑定eip信息--非800！pageNum={} 用时={},错误码={}，错误信息={}，描述={}", pageNumber,(System.currentTimeMillis()-startTime), newListEipResponseData.getStatusCode(), newListEipResponseData.getMessage(), newListEipResponseData.getDescription())
              return null;
            }
          } else {
            log.error("getEipsDownAll--获取未绑定eip信息--非200！pageNum={} message={}", pageNumber, response.getMessage())
            return null;
          }
        }catch (Exception e) {
          log.error("getEipsDownAll--第{}次 获取未绑定eip信息--Exception",pageNumber,e)
          return null;
        }
        pageNumber++;
        sleep(500)
      }
      log.info("getEipsDownAll--获取未绑定eip信息--end,size={}",eipAll.size())
      return eipAll
    } catch (CtyunOperationException e) {
      log.error("getEipsDownAll--获取未绑定eip信息--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }

}
