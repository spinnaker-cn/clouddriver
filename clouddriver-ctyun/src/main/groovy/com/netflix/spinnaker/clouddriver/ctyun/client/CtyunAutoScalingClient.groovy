package com.netflix.spinnaker.clouddriver.ctyun.client

import cn.ctyun.ctapi.CTResponse
import cn.ctyun.ctapi.ctelb.CtelbClient
import cn.ctyun.ctapi.ctelb.createtarget.CreateTargetRequest
import cn.ctyun.ctapi.ctelb.createtarget.CreateTargetRequestBody
import cn.ctyun.ctapi.ctelb.createtarget.CreateTargetResponseData
import cn.ctyun.ctapi.ctelb.deletetarget.DeleteTargetRequest
import cn.ctyun.ctapi.ctelb.deletetarget.DeleteTargetRequestBody
import cn.ctyun.ctapi.ctelb.deletetarget.DeleteTargetResponseData
import cn.ctyun.ctapi.ctelb.listtarget.ListTargetRequest
import cn.ctyun.ctapi.ctelb.listtarget.ListTargetResponseData
import cn.ctyun.ctapi.scaling.ScalingClient
import cn.ctyun.ctapi.scaling.configcreate.ConfigCreateRequest
import cn.ctyun.ctapi.scaling.configcreate.ConfigCreateRequestBody
import cn.ctyun.ctapi.scaling.configcreate.ConfigCreateResponseData
import cn.ctyun.ctapi.scaling.configcreate.Tag
import cn.ctyun.ctapi.scaling.configcreate.Volume
import cn.ctyun.ctapi.scaling.configdelete.ConfigDeleteRequest
import cn.ctyun.ctapi.scaling.configdelete.ConfigDeleteRequestBody
import cn.ctyun.ctapi.scaling.configdelete.ConfigDeleteResponseData
import cn.ctyun.ctapi.scaling.groupcreate.GroupCreateRequest
import cn.ctyun.ctapi.scaling.groupcreate.GroupCreateRequestBody
import cn.ctyun.ctapi.scaling.groupcreate.GroupCreateResponseData
import cn.ctyun.ctapi.scaling.groupcreate.LbList
import cn.ctyun.ctapi.scaling.groupcreate.MazInfo
import cn.ctyun.ctapi.scaling.groupcreate.Volumes
import cn.ctyun.ctapi.scaling.groupdelete.GroupDeleteRequest
import cn.ctyun.ctapi.scaling.groupdelete.GroupDeleteRequestBody
import cn.ctyun.ctapi.scaling.groupdelete.GroupDeleteResponseData
import cn.ctyun.ctapi.scaling.groupdisable.GroupDisableRequest
import cn.ctyun.ctapi.scaling.groupdisable.GroupDisableRequestBody
import cn.ctyun.ctapi.scaling.groupdisable.GroupDisableResponseData
import cn.ctyun.ctapi.scaling.groupenable.GroupEnableRequest
import cn.ctyun.ctapi.scaling.groupenable.GroupEnableRequestBody
import cn.ctyun.ctapi.scaling.groupenable.GroupEnableResponseData
import cn.ctyun.ctapi.scaling.grouplist.GroupListRequest
import cn.ctyun.ctapi.scaling.grouplist.GroupListRequestBody
import cn.ctyun.ctapi.scaling.grouplist.GroupListResponseData
import cn.ctyun.ctapi.scaling.grouplist.ScalingGroup
import cn.ctyun.ctapi.scaling.grouplistconfig.GroupListConfigRequest
import cn.ctyun.ctapi.scaling.grouplistconfig.GroupListConfigRequestBody
import cn.ctyun.ctapi.scaling.grouplistconfig.GroupListConfigResponseData
import cn.ctyun.ctapi.scaling.grouplistconfig.GroupListConfigReturnObj
import cn.ctyun.ctapi.scaling.grouplistinstance.GroupListInstance
import cn.ctyun.ctapi.scaling.grouplistinstance.GroupListInstanceRequest
import cn.ctyun.ctapi.scaling.grouplistinstance.GroupListInstanceRequestBody
import cn.ctyun.ctapi.scaling.grouplistinstance.GroupListInstanceResponseData
import cn.ctyun.ctapi.scaling.groupqueryactivity.GroupQueryActivityRequest
import cn.ctyun.ctapi.scaling.groupqueryactivity.GroupQueryActivityRequestBody
import cn.ctyun.ctapi.scaling.groupqueryactivity.GroupQueryActivityResponseData
import cn.ctyun.ctapi.scaling.groupqueryactivitydetail.GroupQueryActivityDetailRequest
import cn.ctyun.ctapi.scaling.groupqueryactivitydetail.GroupQueryActivityDetailRequestBody
import cn.ctyun.ctapi.scaling.groupqueryactivitydetail.GroupQueryActivityDetailResponseData
import cn.ctyun.ctapi.scaling.groupqueryactivitydetail.GroupQueryActivityResultObj
import cn.ctyun.ctapi.scaling.groupupdate.GroupUpdateRequest
import cn.ctyun.ctapi.scaling.groupupdate.GroupUpdateRequestBody
import cn.ctyun.ctapi.scaling.groupupdate.GroupUpdateResponseData
import cn.ctyun.ctapi.scaling.instancemoveout.InstanceMoveOutRequest
import cn.ctyun.ctapi.scaling.instancemoveout.InstanceMoveOutRequestBody
import cn.ctyun.ctapi.scaling.instancemoveout.InstanceMoveOutResponseData
import cn.ctyun.ctapi.scaling.instancemoveoutrelease.InstanceMoveOutReleaseRequest
import cn.ctyun.ctapi.scaling.instancemoveoutrelease.InstanceMoveOutReleaseRequestBody
import cn.ctyun.ctapi.scaling.instancemoveoutrelease.InstanceMoveOutReleaseResponseData
import cn.ctyun.ctapi.scaling.listlabel.LabelDTO
import cn.ctyun.ctapi.scaling.listlabel.ListLabelRequest
import cn.ctyun.ctapi.scaling.listlabel.ListLabelRequestBody
import cn.ctyun.ctapi.scaling.listlabel.ListLabelResponseData
import cn.ctyun.ctapi.scaling.listloadbalancer.ListLoadBalancer
import cn.ctyun.ctapi.scaling.listloadbalancer.ListLoadBalancerRequest
import cn.ctyun.ctapi.scaling.listloadbalancer.ListLoadBalancerRequestBody
import cn.ctyun.ctapi.scaling.listloadbalancer.ListLoadBalancerResponseData
import cn.ctyun.ctapi.scaling.rulecreate.GroupCreateRuleRequest
import cn.ctyun.ctapi.scaling.rulecreate.GroupCreateRuleResponseData
import cn.ctyun.ctapi.scaling.rulecreatealarm.GroupCreateAlarmRuleRequest
import cn.ctyun.ctapi.scaling.rulecreatealarm.GroupCreateAlarmRuleResponseData
import cn.ctyun.ctapi.scaling.rulecreatecycle.GroupCreateCycleRuleRequest
import cn.ctyun.ctapi.scaling.rulecreatecycle.GroupCreateCycleRuleResponseData
import cn.ctyun.ctapi.scaling.rulecreatescheduled.GroupCreateScheduledRuleRequest
import cn.ctyun.ctapi.scaling.rulecreatescheduled.GroupCreateScheduledRuleResponseData
import cn.ctyun.ctapi.scaling.ruledelete.RuleDeleteRequest
import cn.ctyun.ctapi.scaling.ruledelete.RuleDeleteRequestBody
import cn.ctyun.ctapi.scaling.ruledelete.RuleDeleteResponseData
import cn.ctyun.ctapi.scaling.ruledeletealarm.RuleDeleteAlarmRequest
import cn.ctyun.ctapi.scaling.ruledeletealarm.RuleDeleteAlarmRequestBody
import cn.ctyun.ctapi.scaling.ruledeletealarm.RuleDeleteAlarmResponseData
import cn.ctyun.ctapi.scaling.ruleexecute.RuleExecuteRequest
import cn.ctyun.ctapi.scaling.ruleexecute.RuleExecuteResponseData
import cn.ctyun.ctapi.scaling.rulelist.GroupRuleListRequest
import cn.ctyun.ctapi.scaling.rulelist.GroupRuleListRequestBody
import cn.ctyun.ctapi.scaling.rulelist.GroupRuleListResponseData
import cn.ctyun.ctapi.scaling.rulelist.RuleInfo
import cn.ctyun.ctapi.scaling.rulestart.RuleStartRequest
import cn.ctyun.ctapi.scaling.rulestart.RuleStartRequestBody
import cn.ctyun.ctapi.scaling.rulestart.RuleStartResponseData
import cn.ctyun.ctapi.scaling.rulestop.RuleStopRequest
import cn.ctyun.ctapi.scaling.rulestop.RuleStopRequestBody
import cn.ctyun.ctapi.scaling.rulestop.RuleStopResponseData
import cn.ctyun.ctapi.scaling.ruleupdate.RuleUpdateRequest
import cn.ctyun.ctapi.scaling.ruleupdate.RuleUpdateRequestBody
import cn.ctyun.ctapi.scaling.ruleupdate.RuleUpdateResponseData
import cn.ctyun.ctapi.scaling.ruleupdate.TriggerInfo
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.CtyunDeployDescription
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.UpsertCtyunAlarmActionDescription
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.UpsertCtyunScheduledActionDescription
import com.netflix.spinnaker.clouddriver.ctyun.exception.CtyunOperationException
import groovy.util.logging.Slf4j
import org.springframework.beans.BeanUtils
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils

import java.text.SimpleDateFormat

@Component
@Slf4j
class CtyunAutoScalingClient extends AbstractCtyunServiceClient {
  static String defaultServerGroupTagKey = "spinnaker:server-group-name"
  private ScalingClient client
  private ScalingClient labelClient
  private CtelbClient clbClient // todo move to load balancer client ?
  private String regionId
  private final String endingPointElb = "ctelb-global.ctapi.ctyun.cn"
  private final String endingPointLabel = "bss-global.ctapi.ctyun.cn"
  @Override
  String getEndPoint() {
    return "scaling-global.ctapi.ctyun.cn"
  }


  CtyunAutoScalingClient(String accessKey, String securityKey, String regionId) {
    super(accessKey, securityKey)
    clbClient = new CtelbClient();
    clbClient.init(cred, endingPointElb);

    client = new ScalingClient()
    client.init(cred, getEndPoint())

    labelClient=new ScalingClient()
    labelClient.init(cred,endingPointLabel)

    this.regionId = regionId

  }

  Integer deploy(CtyunDeployDescription description) {
    log.info("deploy--创建弹性伸缩组--start")
      try {
        // create ctyun auto scaling group
        GroupCreateRequest createAutoScalingGroupRequest = buildCtyunAutoScalingGroupRequest(description, this.regionId)
        log.info("deploy--创建弹性伸缩组--createAutoScalingGroupRequest={}",createAutoScalingGroupRequest)
        CTResponse<GroupCreateResponseData> response = client.groupCreate(createAutoScalingGroupRequest)
        if(response.httpCode==200&&response.getData()!=null){
          GroupCreateResponseData groupCreateResponseData=response.getData()
          if(groupCreateResponseData.getStatusCode()==800){
            log.info("deploy--创建弹性伸缩组--end！groupCreateResponseData={}",JSONObject.toJSONString(groupCreateResponseData))
            groupCreateResponseData.getReturnObj().getGroupID()
          }else{
            log.info("deploy--创建弹性伸缩组--非800！,错误码={}，错误信息={}",groupCreateResponseData.getErrorCode(),groupCreateResponseData.getDescription())
            throw new CtyunOperationException(groupCreateResponseData.getDescription())
          }
        }else{
          log.info("deploy--创建弹性伸缩组--非200！{}",response)
          throw new CtyunOperationException(response.getMessage())
        }

      } catch (Exception e) {
        log.error("deploy--创建弹性伸缩组--Exception",e)
        throw new CtyunOperationException(e)
      }
  }

  //构建创建天翼云弹性伸缩请求体
  private def buildCtyunAutoScalingGroupRequest(CtyunDeployDescription description, String regionId) {
    //多可用区资源池的实例可用区及子网信息。mazInfo和subnetIDList 2个参数互斥，如果资源池为多可用区时使用mazInfo则不传subnetIDList 参数
    List<MazInfo> mazInfoList=null
    if(description.mazInfoList){
      mazInfoList=new ArrayList<>()
      description.mazInfoList.collect {
        MazInfo mazInfo = new MazInfo()
        mazInfo.setAzName(it.azName)
        mazInfo.setMasterId(it.masterId)
        List<String> optionIdList=it.optionId==null?null:it.optionId.collect {ss->ss.toString()}
        mazInfo.setOptionId(optionIdList?.toArray(new String[0]))
        mazInfoList.add(mazInfo)
      }
    }
    //负载均衡列表，useLb=1必填
    List<LbList> lbList = null
    if (description.forwardLoadBalancers) {
      lbList=new ArrayList<>();
      description.forwardLoadBalancers.each {
        def lb = new LbList()
        lb.port = it.port
        lb.weight = it.weight
        lb.lbID = it.lbID
        //TODO YJS: 负载均衡需要传主机组ID
        lb.hostGroupID = it.hostGroupID
       // lb.hostGroupID ='tg-bfxi8r6wql'
        lbList.add(lb)
      }
    }
    //如果是没有页面进行的复制执行，天翼云要求的必要参数是有问题的，因此采用现有被复制源的伸缩配置id，进行创建
    ConfigCreateRequestBody configObj =null
    if(description.configId==null){
      List<Volume> volumeList = new ArrayList<>()
      if (description.systemDisk) {
        Volumes systemVolume = new Volumes()
        systemVolume.volumeSize = description.systemDisk.diskSize as Integer
        systemVolume.volumeType = description.systemDisk.diskType
        systemVolume.flag = 1
        volumeList.add(systemVolume)
      }
      if (description.dataDisks) {
        volumeList.addAll(description.dataDisks.collect {
          def dataVolume = new Volumes()
          dataVolume.volumeType = it.diskType
          dataVolume.volumeSize = it.diskSize as Integer
          dataVolume.flag = 2
          dataVolume
        })
      }

      List<Tag> tagList = new ArrayList<>()
      if (description.instanceTags) {
        tagList.addAll(description.instanceTags.collect {
          Tag tag2 = new Tag()
          tag2.key = it.key
          tag2.value = it.value
          tag2
        })
      }
      Integer useFloatings = description.internetAccessible.publicIpAssigned?2:1
      Integer loginMode = description.loginSettings.loginMode
      //2是按流量1是按带宽
      Integer billingMode=description.internetAccessible.internetChargeType
      configObj=new ConfigCreateRequestBody().withRegionID(regionId).withName("c-"+description.serverGroupName)
        .withSpecName(description.instanceType)
        .withImageID(description.imageId)
        .withUseFloatings(useFloatings)
        .withBillingMode(billingMode)
        .withBandWidth(description.internetAccessible.internetMaxBandwidthOut as Integer)
        .withLoginMode(loginMode)
        .withVolumes(volumeList)
        .withTags(tagList)
      if (1 == loginMode) {
        configObj.withUsername(description.loginSettings.userName).withPassword(description.loginSettings.password)
      } else if(2 == loginMode){
        configObj.withKeyPairID(description.loginSettings.keyIds)
      }
      //安全组ID列表，非多可用区资源池该参数为必填
      if(mazInfoList==null){
        configObj.withSecurityGroupIDList(description.securityGroupIds?.toArray(new String[0]))
      }
      description.configId=this.createConfig(configObj)
    }else{
      log.info("buildCtyunAutoScalingGroupRequest 采用configId={}创建伸缩组",description.configId)
    }

    //TODO YJS: recoveryMode, healthMode, healthPeriod, azName, masterId, optionId需要添加到description中
    GroupCreateRequestBody body = new GroupCreateRequestBody().withRegionID(regionId)
      .withRecoveryMode(description.recoveryMode==null?1:description.recoveryMode)
      .withName(description.serverGroupName)
      .withHealthMode(description.healthMode==null?1:description.healthMode)
      .withMoveOutStrategy(description.terminationPolicies[0])
      .withVpcID(description.vpcId)
      .withMinCount(description.minSize)
      .withMaxCount(description.maxSize)
      .withExpectedCount(description.desiredCapacity)
      .withHealthPeriod(description.healthPeriod==null?300:description.healthPeriod)
      .withConfigID(description.configId)
    if (description.forwardLoadBalancers) {
      body.withUseLb(1).withLbList(lbList)
    } else{
      body.withUseLb(2)
    }
    if(mazInfoList!=null){
      body.withMazInfo(mazInfoList).withSecurityGroupIDList(description.securityGroupIds?.toArray(new String[0]))
    }else{
      body.withSubnetIDList(description.subnetIds.size()==0?null:description.subnetIds)
    }
    log.info("body={}",JSONObject.toJSONString(body))
    GroupCreateRequest request = new GroupCreateRequest().withBody(body)
    request
  }

  //创建配置信息
  Integer createConfig(ConfigCreateRequestBody configObj) {
    log.info("createConfig--resize弹性组--start--configObj--{}",JSONObject.toJSONString(configObj))
    try {
      ConfigCreateRequest request = new ConfigCreateRequest().withBody(configObj);
      CTResponse<ConfigCreateResponseData> response= client.createConfig(request);
      if (response.httpCode == 200 && response.getData() != null) {
        ConfigCreateResponseData configCreateResponseData = response.getData()
        if (configCreateResponseData.getStatusCode() == 800) {
          log.info("createConfig--resize弹性组--成功--end--{}", JSONObject.toJSONString(configCreateResponseData.getReturnObj()))
          return configCreateResponseData.getReturnObj().getId()
        } else {
          log.info("createConfig--resize弹性组--非800！错误码={}，错误信息={}",  configCreateResponseData.getErrorCode(), configCreateResponseData.getDescription())
          throw new CtyunOperationException(configCreateResponseData.getDescription())
        }
      } else {
        log.info("createConfig--resize弹性组--非200！{}",response)
        throw new CtyunOperationException(response.getMessage())
      }
    } catch (Exception e) {
      log.error("createConfig--resize弹性组--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }

  //查询所有弹性伸缩组--缓存用
  List<ScalingGroup> getAllAutoScalingGroups() {
    log.info("getAllAutoScalingGroups--查询所有弹性伸缩组--start")
    List<ScalingGroup> autoScalingGroupAll = []
    try {
      def pageNumber=1;
      def totalCount = DEFAULT_LIMIT
      def getCount = DEFAULT_LIMIT
      while(totalCount==getCount){
        try {
          GroupListRequestBody requestBody = new GroupListRequestBody().withRegionID(regionId).withPageNo(pageNumber).withPageSize(DEFAULT_LIMIT);
          GroupListRequest request = new GroupListRequest().withBody(requestBody);
          CTResponse<GroupListResponseData> response = client.groupList(request);
          if (response.httpCode == 200 && response.getData() != null) {
            GroupListResponseData groupListResponseData = response.getData()
            if (groupListResponseData.getStatusCode() == 800) {
              if (groupListResponseData.getReturnObj() == null || groupListResponseData.getReturnObj().getScalingGroups() == null) {
                throw new CtyunOperationException("返回结果为空！groupListResponseData="+JSONObject.toJSONString(groupListResponseData))
              }
              if (groupListResponseData.getReturnObj().getScalingGroups().size() > 0) {
                autoScalingGroupAll.addAll(groupListResponseData.getReturnObj().getScalingGroups())
              }
              getCount = groupListResponseData.getReturnObj().getScalingGroups().size();
            } else {
              log.error("getAllAutoScalingGroups--查询所有弹性伸缩组--非800！pageNum={},错误码={}，错误信息={}", pageNumber, groupListResponseData.getErrorCode(), groupListResponseData.getDescription())
            }
          } else {
            log.error("getAllAutoScalingGroups--查询所有弹性伸缩组--非200！{}", response)
          }
        }catch (Exception e) {
          log.error("getAllAutoScalingGroups--第{}次 查询所有弹性伸缩组--Exception",pageNumber,e)
        }
        pageNumber++;
      }
      log.info("getAllAutoScalingGroups--查询所有弹性伸缩组--end,size={}",autoScalingGroupAll.size())
      return autoScalingGroupAll
    } catch (Exception e) {
      log.error("getAllAutoScalingGroups--查询所有弹性伸缩组--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
  //查询所有弹性伸缩组--其他接口遍历用
  List<ScalingGroup> getAllAutoScalingGroups2() {
    log.info("getAllAutoScalingGroups2--查询所有弹性伸缩组--start")
    List<ScalingGroup> autoScalingGroupAll = []
    try {
      def pageNumber=1;
      def totalCount = DEFAULT_LIMIT
      def getCount = DEFAULT_LIMIT
      while(totalCount==getCount){
        try {
          GroupListRequestBody requestBody = new GroupListRequestBody().withRegionID(regionId).withPageNo(pageNumber).withPageSize(DEFAULT_LIMIT);
          GroupListRequest request = new GroupListRequest().withBody(requestBody);
          CTResponse<GroupListResponseData> response = client.groupList(request);
          if (response.httpCode == 200 && response.getData() != null) {
            GroupListResponseData groupListResponseData = response.getData()
            if (groupListResponseData.getStatusCode() == 800) {
              if (groupListResponseData.getReturnObj() == null || groupListResponseData.getReturnObj().getScalingGroups() == null) {
                throw new CtyunOperationException("返回结果为空！groupListResponseData=" + JSONObject.toJSONString(groupListResponseData))
              }
              if (groupListResponseData.getReturnObj().getScalingGroups().size() > 0) {
                autoScalingGroupAll.addAll(groupListResponseData.getReturnObj().getScalingGroups())
              }
              getCount = groupListResponseData.getReturnObj().getScalingGroups().size();
            } else {
              log.info("getAllAutoScalingGroups2--查询所有弹性伸缩组--非800！pageNum={},错误码={}，错误信息={}", pageNumber, groupListResponseData.getErrorCode(), groupListResponseData.getDescription())
            }
          } else {
            log.info("getAllAutoScalingGroups2--查询所有弹性伸缩组--非200！{}", response)
          }
        }catch (Exception e) {
          log.error("getAllAutoScalingGroups2--第{}次 查询所有弹性伸缩组--Exception",pageNumber,e)
        }
        pageNumber++;
      }
      log.info("getAllAutoScalingGroups2--查询所有弹性伸缩组--end,size={}",autoScalingGroupAll.size())
      return autoScalingGroupAll
    } catch (Exception e) {
      log.error("getAllAutoScalingGroups2--查询所有弹性伸缩组--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
  //通过groupId查询所有弹性伸缩组
  ScalingGroup getAutoScalingGroupByGroupId(Integer groupId) {
    log.info("getAutoScalingGroupByGroupId--通过groupId查询所有弹性伸缩组--start--groupId--{}",groupId)
    ScalingGroup autoScalingGroup
    try {
        GroupListRequestBody requestBody = new GroupListRequestBody().withRegionID(regionId).withGroupID(groupId);
        GroupListRequest request = new GroupListRequest().withBody(requestBody);
        CTResponse<GroupListResponseData> response = client.groupList(request);
        if(response.httpCode==200&&response.getData()!=null){
          GroupListResponseData groupListResponseData=response.getData()
          if(groupListResponseData.getStatusCode()==800){
            if(groupListResponseData.getReturnObj()?.getScalingGroups()?.size()>0){
              autoScalingGroup=groupListResponseData.getReturnObj().getScalingGroups()[0]
            }
          }else{
            log.info("getAutoScalingGroupByGroupId--通过groupId查询所有弹性伸缩组--非800！,错误码={}，错误信息={}",groupListResponseData.getErrorCode(),groupListResponseData.getDescription())
          }
        }else{
          log.info("getAutoScalingGroupByGroupId--通过groupId查询所有弹性伸缩组--非200！{}",response)
          //throw new CtyunOperationException(response.getDescription())
        }
      log.info("getAutoScalingGroupByGroupId--通过groupId查询所有弹性伸缩组--end")
      return autoScalingGroup
    } catch (Exception e) {
      log.error("getAutoScalingGroupByGroupId--通过groupId查询所有弹性伸缩组--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
  //通过groupId查询所有负载均衡器
  List<ListLoadBalancer> getLoadBalancerListByGroupId(Integer groupId) {
    log.info("getLoadBalancerListByGroupId--通过groupId查询所有负载均衡器--start--groupId--{}",groupId)
    List<ListLoadBalancer> list
    try {
      ListLoadBalancerRequestBody requestBody = new ListLoadBalancerRequestBody().withRegionID(regionId).withGroupID(groupId);
      ListLoadBalancerRequest request = new ListLoadBalancerRequest().withBody(requestBody);
      CTResponse<ListLoadBalancerResponseData> response = client.listLoadBalancer(request);
      if(response.httpCode==200&&response.getData()!=null){
        ListLoadBalancerResponseData listLoadBalancerResponseData=response.getData()
        if(listLoadBalancerResponseData.getStatusCode()==800){
          if(listLoadBalancerResponseData.getReturnObj()?.getLoadBalancers()?.size()>0){
            list=listLoadBalancerResponseData.getReturnObj().getLoadBalancers()
          }
        }else{
          log.info("getLoadBalancerListByGroupId--通过groupId查询所有负载均衡器--非800！,错误码={}，错误信息={}",listLoadBalancerResponseData.getErrorCode(),listLoadBalancerResponseData.getDescription())
        }
      }else{
        log.info("getLoadBalancerListByGroupId--通过groupId查询所有负载均衡器--非200！{}",response.getMessage())
        //throw new CtyunOperationException(response.getDescription())
      }
      log.info("getLoadBalancerListByGroupId--通过groupId查询所有负载均衡器--end")
      return list
    } catch (Exception e) {
      log.error("getLoadBalancerListByGroupId--通过groupId查询所有负载均衡器--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
  //通过伸缩组名称获取伸缩组
  List<ScalingGroup> getAutoScalingGroupsByName(String name) {
    log.info("getAutoScalingGroupsByName--通过伸缩组名称获取伸缩组--start--name--{}",name)
    try {
      List<ScalingGroup> allAutoScalingGroups= this.getAllAutoScalingGroups2()
      List<ScalingGroup> newAutoScalingGroups=allAutoScalingGroups.findAll {
        it.name == name
      }
      log.info("getAutoScalingGroupsByName--通过伸缩组名称获取伸缩组--end,size={}",newAutoScalingGroups.size())
      return newAutoScalingGroups
    } catch (Exception e) {
      log.error("getAutoScalingGroupsByName--通过伸缩组名称获取伸缩组--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
  //通过配置id集合获取配置信息
  GroupListConfigReturnObj getLaunchConfiguration(launchConfigurationId,groupId) {
    log.info("getLaunchConfiguration--通过配置id集合获取配置信息--start--launchConfigurationId--{},groupId--{}",launchConfigurationId,groupId)
    try {
        GroupListConfigRequestBody requestBody = new GroupListConfigRequestBody().withRegionID(regionId).withConfigID(launchConfigurationId);
        GroupListConfigRequest request = new GroupListConfigRequest().withBody(requestBody);
        CTResponse<GroupListConfigResponseData> response = client.groupConfigList(request);
        if (response.httpCode == 200 && response.getData() != null) {
          GroupListConfigResponseData groupListConfigResponseData = response.getData()
          if (groupListConfigResponseData.getStatusCode() == 800) {
            GroupListConfigReturnObj obj=groupListConfigResponseData.getReturnObj()?.get(0);
            log.info("getLaunchConfiguration--通过配置id集合获取配置信息--end")
            return obj;
          } else {
            log.info("getLaunchConfiguration--通过配置id集合获取配置信息--非800！错误码={}，错误信息={}",  groupListConfigResponseData.getErrorCode(), groupListConfigResponseData.getDescription())
          }
        } else {
          log.info("getLaunchConfiguration--通过配置id集合获取配置信息--非200！{}",response)
          //throw new CtyunOperationException(response.getDescription())
        }

    } catch (Exception e) {
      log.error("getLaunchConfiguration--通过配置id集合获取配置信息--Exception",e)
      throw new CtyunOperationException(e.toString())
    }

  }
  //获取所有伸缩组关联的主机
  Map<String,Object> getAllAutoScalingInstances() {
    log.info("getAllAutoScalingInstances--获取所有伸缩组关联的主机--start")
    List<GroupListInstance> allList=new ArrayList<>();
    List<Map<String,String>> groupNameList=new ArrayList<>();
    try {
      List<ScalingGroup> allScalingGroupList=this.getAllAutoScalingGroups2();
      for(ScalingGroup scalingGroup:allScalingGroupList){
        List<GroupListInstance> groupListInstanceList=this.getAutoScalingInstances(scalingGroup.getGroupID())
        for(GroupListInstance groupListInstance:groupListInstanceList){
          if(groupListInstance.instanceID!=null){
            Map<String,String> groupMap=new HashMap()
            groupMap.put("groupName",scalingGroup.getName())
            groupMap.put("instanceID",groupListInstance.getInstanceID())
            groupNameList.add(groupMap)
            allList.add(groupListInstance)
          }
        }
      }
    } catch (Exception e) {
      log.error("getAllAutoScalingInstances--获取所有伸缩组关联的主机--Exception",e)
      //throw new CtyunOperationException(e.toString())
    }
    Map<String,Object> map=new HashMap();
    map.put("groupNameList",groupNameList);
    map.put("groupListInstanceList",allList);
    log.info("getAllAutoScalingInstances--获取所有伸缩组关联的主机--end,groupNameList.size:{} groupListInstanceList.size:{}",groupNameList.size(),allList.size())
    return map;
  }
//获取伸缩组实例，通过伸缩组id获取
  List<GroupListInstance> getAutoScalingInstances(Integer asgId) {
    log.info("getAutoScalingInstances--通过伸缩组id获取主机实例--start--asgId--{}",asgId)
    try {
      GroupListInstanceRequestBody body = new GroupListInstanceRequestBody().withGroupID(asgId).withRegionID(regionId);
      GroupListInstanceRequest request = new GroupListInstanceRequest().withBody(body);
      CTResponse<GroupListInstanceResponseData> response = client.groupInstanceListByGroupId(request);
      if (response.httpCode == 200 && response.getData() != null) {
        GroupListInstanceResponseData groupListInstanceResponseData = response.getData()
        if (groupListInstanceResponseData.getStatusCode() == 800) {
          log.info("getAutoScalingInstances--通过伸缩组id获取主机实例--end--size--{}",groupListInstanceResponseData.getReturnObj()?.getInstanceList()?.size())
          return groupListInstanceResponseData.getReturnObj()?.getInstanceList();
        } else {
          log.info("getAutoScalingInstances--通过伸缩组id获取主机实例--非800！错误码={}，错误信息={}",  groupListInstanceResponseData.getErrorCode(), groupListInstanceResponseData.getDescription())
        }
      } else {
        log.info("getLaunchConfiguration--通过配置id集合获取配置信息--非200！{}",response)
       // throw new CtyunOperationException(response.getDescription())
      }
    } catch (Exception e) {
      log.error("getAutoScalingInstances--通过伸缩组id获取主机实例--Exception",e)
      throw new CtyunOperationException(e)
    }

  }

  //获取标签
  List<LabelDTO> getLabels(List<String> asgInstanceIds) {
    log.info("getLabels--标签--start asgInstanceIds={}",asgInstanceIds)
    List<LabelDTO> labelAll = []
    try {
      def pageNumber=1;
      def totalCount = DEFAULT_LIMIT
      def getCount = DEFAULT_LIMIT
      while(totalCount==getCount){
        try {
          Map<String, Object> queryParam = new HashMap();
          queryParam.put("pageNum", pageNumber);
          queryParam.put("pageSize", DEFAULT_LIMIT);
          ListLabelRequestBody body = new ListLabelRequestBody().withBaseResourceIds(asgInstanceIds);
          ListLabelRequest request = new ListLabelRequest().withBody(body).withQueryParam(queryParam);
          CTResponse<ListLabelResponseData> response = labelClient.listlabels(request);
          if (response.httpCode == 200 && response.getData() != null) {
            ListLabelResponseData listLabelResponseData = response.getData()
            if (listLabelResponseData.getList() != null) {
              if (listLabelResponseData.getList().size() > 0) {
                labelAll.addAll(listLabelResponseData.getList())
              }
              getCount = listLabelResponseData.getList().size();
            } else {
              log.info("getLabels--标签--无数据！listLabelResponseData.getList()={}", listLabelResponseData.getList())
              break
            }

          } else {
            log.info("getLabels--标签--非200！{}", response.getMessage())
            //throw new CtyunOperationException(response.getMessage())
          }
        }catch (Exception e) {
          log.error("getLabels--第{}次 标签--Exception",pageNumber,e)
        }
        pageNumber++;
      }
      log.info("getLabels--标签--end,size={}",labelAll.size())
      return labelAll
    } catch (Exception e) {
      log.error("getLabels--标签--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }

  //获取伸缩组告警策略
  List<RuleInfo> getScalingPolicies(Integer asgId = null) {
    log.info("getScalingPolicies--获取伸缩组告警策略--start--asgId--{}",asgId)
    List<RuleInfo> autoScalingPoliciesAll = []
    try {
      def pageNumber=1;
      def totalCount = DEFAULT_LIMIT
      def getCount = DEFAULT_LIMIT
      while(totalCount==getCount){
        try {
          GroupRuleListRequestBody requestBody = new GroupRuleListRequestBody().withGroupID(asgId).withRegionID(regionId).withPage(pageNumber).withPageSize(DEFAULT_LIMIT);
          GroupRuleListRequest request = new GroupRuleListRequest().withBody(requestBody);
          CTResponse<GroupRuleListResponseData> response = client.groupRuleList(request);
          if (response.httpCode == 200 && response.getData() != null) {
            GroupRuleListResponseData groupRuleListResponseData = response.getData()
            if (groupRuleListResponseData.getStatusCode() == 800) {
              if (groupRuleListResponseData.getReturnObj() == null || groupRuleListResponseData.getReturnObj().getRuleList() == null) {
                throw new CtyunOperationException("返回结果为空！groupRuleListResponseData=" + JSONObject.toJSONString(groupRuleListResponseData))
              }
              if (groupRuleListResponseData.getReturnObj().getRuleList().size() > 0) {
                autoScalingPoliciesAll.addAll(groupRuleListResponseData.getReturnObj().getRuleList())
              }
              getCount = groupRuleListResponseData.getReturnObj().getRuleList().size();
            } else {
              log.info("getScalingPolicies--获取伸缩组告警策略--非800！pageNum={},错误码={}，错误信息={}", pageNumber, groupRuleListResponseData.getErrorCode(), groupRuleListResponseData.getDescription())
            }
          } else {
            log.info("getScalingPolicies--获取伸缩组告警策略--非200！{}", response)
            // throw new CtyunOperationException(response.getDescription())
          }
        }catch (Exception e) {
          log.error("getScalingPolicies--第{}次 获取伸缩组告警策略--Exception",pageNumber,e)
        }
        pageNumber++;
      }

      def resultSet=autoScalingPoliciesAll?.findAll({
        it.ruleType==1
      })
      log.info("getScalingPolicies--获取伸缩组告警策略--end--size--{}",resultSet?.size())
      return resultSet
    } catch (Exception e) {
      log.error("getScalingPolicies--获取伸缩组告警策略--Exception",e)
      throw new CtyunOperationException(e.toString())
    }

  }
  //根据伸缩组获取定时和周期策略
  List<RuleInfo> getScheduledAction(Integer asgId = null) {
    log.info("getScheduledAction--根据伸缩组获取定时和周期策略--start--asgId--{}",asgId)
    List<RuleInfo> autoScalingPoliciesAll = []
    try {
      def pageNumber=1;
      def totalCount = DEFAULT_LIMIT
      def getCount = DEFAULT_LIMIT
      while(totalCount==getCount){
        try {
          GroupRuleListRequestBody requestBody = new GroupRuleListRequestBody().withGroupID(asgId).withRegionID(regionId).withPage(pageNumber).withPageSize(DEFAULT_LIMIT);
          GroupRuleListRequest request = new GroupRuleListRequest().withBody(requestBody);
          CTResponse<GroupRuleListResponseData> response = client.groupRuleList(request);
          if (response.httpCode == 200 && response.getData() != null) {
            GroupRuleListResponseData groupRuleListResponseData = response.getData()
            if (groupRuleListResponseData.getStatusCode() == 800) {
              if (groupRuleListResponseData.getReturnObj() == null || groupRuleListResponseData.getReturnObj().getRuleList() == null) {
                throw new CtyunOperationException("返回结果为空！groupRuleListResponseData=" + JSONObject.toJSONString(groupRuleListResponseData))
              }
              if (groupRuleListResponseData.getReturnObj().getRuleList().size() > 0) {
                autoScalingPoliciesAll.addAll(groupRuleListResponseData.getReturnObj().getRuleList())
              }

              getCount = groupRuleListResponseData.getReturnObj().getRuleList().size();
            } else {
              log.info("getScheduledAction--根据伸缩组获取定时和周期策略--非800！pageNum={},错误码={}，错误信息={}", (pageNumber - 1), groupRuleListResponseData.getErrorCode(), groupRuleListResponseData.getDescription())
            }
          } else {
            log.info("getScheduledAction--根据伸缩组获取定时和周期策略--非200！{}", response)
            //throw new CtyunOperationException(response.getDescription())
          }
        }catch (Exception e) {
          log.error("getScheduledAction--第{}次 根据伸缩组获取定时和周期策略--Exception",pageNumber,e)
        }
        pageNumber++;
      }
      def resultSet= autoScalingPoliciesAll?.findAll({
        it.ruleType!=1
      })
      log.info("getScheduledAction--根据伸缩组获取定时和周期策略--end--size--{}",resultSet?.size())
      return resultSet
    } catch (Exception e) {
      log.error("getScheduledAction--根据伸缩组获取定时和周期策略--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
  //获取所有伸缩活动日志数据
  List<GroupQueryActivityResultObj> getAutoScalingActivitiesByAsgId(Integer asgId, Integer maxActivityNum = 100) {
    log.info("getAutoScalingActivitiesByAsgId--获取所有伸缩活动日志数据--start--asgId--{}",asgId)
    List<GroupQueryActivityResultObj> autoScalingActivitiesAll = []
    try {
        GroupQueryActivityRequestBody requestBody = new GroupQueryActivityRequestBody().withGroupID(asgId).withRegionID(regionId);
        GroupQueryActivityRequest request = new GroupQueryActivityRequest().withBody(requestBody);
        CTResponse<GroupQueryActivityResponseData> response = client.groupQueryActivity(request);
        if(response.httpCode==200&&response.getData()!=null){
          GroupQueryActivityResponseData groupQueryActivityResponseData=response.getData()
          if(groupQueryActivityResponseData.getStatusCode()==800){
            for(Integer activeID:groupQueryActivityResponseData.getReturnObj().getActiveIDList()){
              GroupQueryActivityResultObj groupQueryActivityResultObj= this.getAutoScalingActivitiesDetailByActiveId(asgId,activeID)
              autoScalingActivitiesAll.add(groupQueryActivityResultObj);
            }
          }else{
            log.info("getAutoScalingActivitiesByAsgId--获取所有伸缩活动日志数据--非800,错误码={}，错误信息={}",groupQueryActivityResponseData.getErrorCode(),groupQueryActivityResponseData.getDescription())
          }
        }else{
          log.info("getAutoScalingActivitiesByAsgId--获取所有伸缩活动日志数据--非200！{}",response)
         // throw new CtyunOperationException(response.getDescription())
        }
      log.info("getAutoScalingActivitiesByAsgId--获取所有伸缩活动日志数据--end--size--{}",autoScalingActivitiesAll.size())
      return autoScalingActivitiesAll
    } catch (Exception e) {
      log.error("getAutoScalingActivitiesByAsgId--获取所有伸缩活动日志数据--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
//通过活动id获取活动详情
  GroupQueryActivityResultObj getAutoScalingActivitiesDetailByActiveId(Integer asgId, Integer activeID){
    log.info("getAutoScalingActivitiesDetailByActiveId--通过活动id获取活动详情--start--asgId--{}--activeID--{}",asgId,activeID)
    try {
      GroupQueryActivityDetailRequestBody body = new GroupQueryActivityDetailRequestBody().withGroupID(asgId).withActiveID(activeID).withRegionID(regionId);
      GroupQueryActivityDetailRequest request = new GroupQueryActivityDetailRequest().withBody(body);
      CTResponse<GroupQueryActivityDetailResponseData> response = client.groupQueryActivityDetail(request);
      if (response.httpCode == 200 && response.getData() != null) {
        GroupQueryActivityDetailResponseData groupQueryActivityDetailResponseData = response.getData()
        if (groupQueryActivityDetailResponseData.getStatusCode() == 800) {
          log.info("getAutoScalingActivitiesDetailByActiveId--通过活动id获取活动详情--end--{}",groupQueryActivityDetailResponseData.getReturnObj())
          return groupQueryActivityDetailResponseData.getReturnObj();
        } else {
          log.info("getAutoScalingActivitiesDetailByActiveId--通过活动id获取活动详情--非800！错误码={}，错误信息={}",  groupQueryActivityDetailResponseData.getErrorCode(), groupQueryActivityDetailResponseData.getDescription())
        }
      } else {
        log.info("getAutoScalingActivitiesDetailByActiveId--通过活动id获取活动详情--非200！{}",response)
        //throw new CtyunOperationException(response.getDescription())
      }
    } catch (Exception e) {
      log.error("getAutoScalingActivitiesDetailByActiveId--通过活动id获取活动详情--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }

  //启用
  void enableAutoScalingGroup(Integer asgId) {
    log.info("enableAutoScalingGroup--启用弹性组--start--asgId--{}",asgId)
    try {
      GroupEnableRequestBody body = new GroupEnableRequestBody().withRegionID(regionId).withGroupID(asgId);
      GroupEnableRequest request = new GroupEnableRequest().withBody(body);
      CTResponse<GroupEnableResponseData> response=client.groupEnable(request);
      if (response.httpCode == 200 && response.getData() != null) {
        GroupEnableResponseData groupEnableResponseData = response.getData()
        if (groupEnableResponseData.getStatusCode() == 800) {
          log.info("enableAutoScalingGroup--启用弹性组--成功--end--{}",groupEnableResponseData.getReturnObj())
        } else {
          log.info("enableAutoScalingGroup--启用弹性组--非800！错误码={}，错误信息={}",  groupEnableResponseData.getErrorCode(), groupEnableResponseData.getDescription())
        }
      } else {
        log.info("enableAutoScalingGroup--启用弹性组--非200！{}",response)
        //throw new CtyunOperationException(response.getDescription())
      }
    } catch (Exception e) {
      log.error("enableAutoScalingGroup--启用弹性组--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
//停用
  void disableAutoScalingGroup(Integer asgId) {
    log.info("disableAutoScalingGroup--停用弹性组--start--asgId--{}",asgId)
    try {
      GroupDisableRequestBody body = new GroupDisableRequestBody().withRegionID(regionId).withGroupID(asgId);
      GroupDisableRequest request = new GroupDisableRequest().withBody(body);
      CTResponse<GroupDisableResponseData> response=client.groupDisable(request);
      if (response.httpCode == 200 && response.getData() != null) {
        GroupDisableResponseData groupDisableResponseData = response.getData()
        if (groupDisableResponseData.getStatusCode() == 800) {
          log.info("disableAutoScalingGroup--停用弹性组--成功--end--{}",groupDisableResponseData.getReturnObj())
        } else {
          log.info("disableAutoScalingGroup--停用弹性组--非800！错误码={}，错误信息={}",  groupDisableResponseData.getErrorCode(), groupDisableResponseData.getDescription())
        }
      } else {
        log.info("disableAutoScalingGroup--停用弹性组--非200！{}",response)
        //throw new CtyunOperationException(response.getDescription())
      }
    } catch (Exception e) {
      log.error("disableAutoScalingGroup--停用弹性组--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
  //resize
  Integer resizeAutoScalingGroup(Integer asgId, def capacity) {
    log.info("resizeAutoScalingGroup--resize弹性组--start--asgId--{}--capacity--{}",asgId,capacity)
    try {
      GroupUpdateRequestBody body = new GroupUpdateRequestBody().withRegionID(regionId).withGroupID(asgId).withMinCount(capacity.min).withMaxCount(capacity.max).withExpectedCount(capacity.desired);
      GroupUpdateRequest request = new GroupUpdateRequest().withBody(body);
      CTResponse<GroupUpdateResponseData> response= client.groupUpdate(request);
      if (response.httpCode == 200 && response.getData() != null) {
        GroupUpdateResponseData groupUpdateResponseData = response.getData()
        if (groupUpdateResponseData.getStatusCode() == 800) {
          log.info("resizeAutoScalingGroup--resize弹性组--成功--end--{}", JSONObject.toJSONString(groupUpdateResponseData.getReturnObj()))
          return groupUpdateResponseData.getReturnObj().getGroupID()
        } else {
          log.info("resizeAutoScalingGroup--resize弹性组--非800！错误码={}，错误信息={}",  groupUpdateResponseData.getErrorCode(), groupUpdateResponseData.getDescription())
        }
      } else {
        log.info("resizeAutoScalingGroup--resize弹性组--非200！{}",response)
        //throw new CtyunOperationException(response.getDescription())
      }
    } catch (Exception e) {
      log.error("resizeAutoScalingGroup--resize弹性组--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }


//删除伸缩组
  void deleteAutoScalingGroup(Integer asgId) {
    log.info("deleteAutoScalingGroup--删除伸缩组--start--asgId--{}",asgId)
    try {
      GroupDeleteRequestBody body = new GroupDeleteRequestBody().withRegionID(regionId).withGroupID(asgId);
      GroupDeleteRequest request = new GroupDeleteRequest().withBody(body);
      CTResponse<GroupDeleteResponseData> response= client.groupDelete(request);
      if (response.httpCode == 200 && response.getData() != null) {
        GroupDeleteResponseData groupDeleteResponseData = response.getData()
        if (groupDeleteResponseData.getStatusCode() == 800) {
          log.info("resizeAutoScalingGroup--resize弹性组--成功--end--{}",groupDeleteResponseData.getReturnObj())
        } else {
          log.info("resizeAutoScalingGroup--resize弹性组--非800！错误码={}，错误信息={}",  groupDeleteResponseData.getErrorCode(), groupDeleteResponseData.getDescription())
          throw new CtyunOperationException(groupDeleteResponseData.getDescription())
        }
      } else {
        log.info("resizeAutoScalingGroup--resize弹性组--非200！{}",response)
        throw new CtyunOperationException(response.getDescription())
      }
    } catch (Exception e) {
      log.error("deleteAutoScalingGroup--删除伸缩组--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
//删除配置
  void deleteLaunchConfiguration(Integer ascId) {
    log.info("deleteLaunchConfiguration--删除配置--start--asgId--{}",ascId)
    try {
      ConfigDeleteRequestBody body = new ConfigDeleteRequestBody().withRegionID(regionId).withConfigID(ascId);
      ConfigDeleteRequest request = new ConfigDeleteRequest().withBody(body);
      CTResponse<ConfigDeleteResponseData> response= client.configDelete(request);
      if (response.httpCode == 200 && response.getData() != null) {
        ConfigDeleteResponseData gonfigDeleteResponseData = response.getData()
        if (gonfigDeleteResponseData.getStatusCode() == 800) {
          log.info("deleteLaunchConfiguration--删除配置--成功--end--{}",gonfigDeleteResponseData.getReturnObj())
        } else {
          log.info("deleteLaunchConfiguration--删除配置--非800！错误码={}，错误信息={}",  gonfigDeleteResponseData.getErrorCode(), gonfigDeleteResponseData.getDescription())
          throw new CtyunOperationException(gonfigDeleteResponseData.getDescription())
        }
      } else {
        log.info("deleteLaunchConfiguration--删除配置--非200！{}",response)
        throw new CtyunOperationException(response.getDescription())
      }
    } catch (Exception e) {
      log.error("deleteLaunchConfiguration--删除配置--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }

  //移除并释放，instanceIds实际是实例的ID字段，integer类型
  void removeInstances(def asgId, def instanceIds) {
    log.info("removeInstances--移除并释放实例--start--asgId--{}--instanceIds--{}",asgId,instanceIds)
    try {
      InstanceMoveOutReleaseRequestBody body = new InstanceMoveOutReleaseRequestBody().withRegionID(regionId).withGroupID(asgId).withInstanceIDList(instanceIds);
      InstanceMoveOutReleaseRequest request = new InstanceMoveOutReleaseRequest().withBody(body);
      CTResponse<InstanceMoveOutReleaseResponseData> response=  client.instanceMoveOutRelease(request);
      if (response.httpCode == 200 && response.getData() != null) {
        InstanceMoveOutReleaseResponseData instanceMoveOutReleaseResponseData = response.getData()
        if (instanceMoveOutReleaseResponseData.getStatusCode() == 800) {
          log.info("removeInstances--移除并释放实例--成功--end--{}",instanceMoveOutReleaseResponseData.getReturnObj())
        } else {
          log.info("removeInstances--移除并释放实例--非800！错误码={}，错误信息={}",  instanceMoveOutReleaseResponseData.getErrorCode(), instanceMoveOutReleaseResponseData.getDescription())
          throw new CtyunOperationException(instanceMoveOutReleaseResponseData.getDescription())
        }
      } else {
        log.info("removeInstances--移除并释放实例--非200！{}",response)
        throw new CtyunOperationException(response.getMessage())
      }
    } catch (Exception e) {
      log.error("removeInstances--移除并释放实例--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }

  //创建弹性伸缩告警策略
  CTResponse<GroupCreateAlarmRuleResponseData> createGroupAlarmRules(GroupCreateAlarmRuleRequest request) {
    return client.createGroupAlarmRules(request);
  }
  //停用策略
  def stopRule(Integer groupId,Integer ruleId){
    log.info("stopRule--停用策略--停用--start--groupId--{},ruleId--{}",groupId,ruleId)
    RuleStopRequestBody body = new RuleStopRequestBody().withRegionID(regionId).withGroupID(groupId).withRuleID(ruleId)
    RuleStopRequest request = new RuleStopRequest().withBody(body);
    CTResponse<RuleStopResponseData> response = client.stopRule(request);
    if(response.httpCode==200&&response.getData()!=null){
      RuleStopResponseData ruleStopResponseData=response.getData()
      if(ruleStopResponseData.getStatusCode()==800){
        log.info("stopRule--停用策略--停用--完成！")
        return true//停用成功后可以修改
      }else{
        log.info("stopRule--停用策略--停用--非800,错误码={}，错误信息={}",ruleStopResponseData.getErrorCode(),ruleStopResponseData.getDescription())
        //throw new CtyunOperationException(ruleStopResponseData.getDescription())
      }
    }else{
      log.info("stopRule--停用策略--停用--非200！{}",response)
      //throw new CtyunOperationException(response)
    }
    return false
  }
  //启用策略
  def startRule(Integer groupId,Integer ruleId){
    log.info("stopRule--停用策略--停用--start--groupId--{},ruleId--{}",groupId,ruleId)
    RuleStartRequestBody body = new RuleStartRequestBody().withRegionID(regionId).withGroupID(groupId).withRuleID(ruleId)
    RuleStartRequest request = new RuleStartRequest().withBody(body);
    CTResponse<RuleStartResponseData> response = client.startRule(request);
    if(response.httpCode==200&&response.getData()!=null){
      RuleStartResponseData ruleStartResponseData=response.getData()
      if(ruleStartResponseData.getStatusCode()==800){
        log.info("startRule--启用策略--重启--完成--end")
      }else{
        log.info("startRule--启用策略--重启--非800,错误码={}，错误信息={}",ruleStartResponseData.getErrorCode(),ruleStartResponseData.getDescription())
        //throw new CtyunOperationException(ruleStartResponseData.getDescription())
      }
    }else{
      log.info("startRule--启用策略--重启--非200！{}",response)
     // throw new CtyunOperationException(response.getDescription())
    }
  }
  //修改告警策略
  def modifyAlarmAction(UpsertCtyunAlarmActionDescription description) {
    log.info("modifyAlarmAction--修改告警策略--start")
    try {
      //启动中，先停用再启动
      Integer oldStatus=2//默认停用状态
      boolean canModify=true//是否可以修改

     if(description.status==1){
       oldStatus=1//设置原状态为启用状态
       canModify=false
       canModify= stopRule(description.getGroupID(),description.getRuleID())
      }

      if(canModify) {
        TriggerInfo triggerInfo = new TriggerInfo()
        //再把入参中的trigger信息复制到triggerInfo中，因为类型不通，上面复制会导致报错，单独处理
        BeanUtils.copyProperties(description.getTriggerObj(), triggerInfo)
        RuleUpdateRequestBody requestBody = new RuleUpdateRequestBody().withRegionID(description.getRegionID())
          .withGroupId(description.getGroupID()).withRuleID(description.getRuleID())
          .withOperateUnit(description.getOperateUnit()).withOperateCount(description.getOperateCount())
          .withAction(description.getAction()).withExecutionTime(description.getExecutionTime()).withEffectiveFrom(description.getEffectiveFrom())
          .withEffectiveTill(description.getEffectiveTill()).withCooldown(description.getCooldown()).withCycle(description.getCycle())
          .withDay(description.getDay()).withTriggerObj(triggerInfo)
        RuleUpdateRequest request = new RuleUpdateRequest().withBody(requestBody)
        CTResponse<RuleUpdateResponseData> response = client.ruleUpdate request
        if(response.httpCode==200&&response.getData()!=null){
          RuleUpdateResponseData ruleUpdateResponseData=response.getData()
          if(ruleUpdateResponseData.getStatusCode()==800){
            log.info("modifyAlarmAction--修改告警策略--修改--完成！")
          }else{
            log.info("modifyAlarmAction--修改告警策略--修改--非800,错误码={}，错误信息={}",ruleUpdateResponseData.getErrorCode(),ruleUpdateResponseData.getDescription())
            //throw new CtyunOperationException(ruleUpdateResponseData.getDescription())
          }
        }else{
          log.info("modifyAlarmAction--修改告警策略--修改--非200！{}",response)
          //throw new CtyunOperationException(response.getDescription())
        }
      }
      //如果原先启动状态，需要再重启，如果原先停用，现在就不用管了
      if(oldStatus==1){
        startRule(description.getGroupID(),description.getRuleID())
      }
    } catch (Exception e) {
      log.error("modifyAlarmAction--修改告警策略--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
  //删除告警策略
  def deleteScalingPolicy(Integer ruleId, Integer groupId) {
    log.info("deleteScalingPolicy--删除告警策略--start--ruleId--{}--groupId--{}",ruleId,groupId)
    try {
      RuleDeleteAlarmRequestBody requestBody = new RuleDeleteAlarmRequestBody().withRegionID(regionId)
        .withGroupId(groupId).withRuleID(ruleId)
      RuleDeleteAlarmRequest request = new RuleDeleteAlarmRequest().withBody(requestBody)
      CTResponse<RuleDeleteAlarmResponseData> response = client.ruleDeleteAlarm(request)
      log.info("deleteScalingPolicy--删除告警策略--end--返回:{}", JSON.toJSONString(response))
      response

    } catch (Exception e) {
      log.error("deleteScalingPolicy--删除告警策略--Exception",e)
      throw new CtyunOperationException(e)
    }
  }
  //创建弹性伸缩策略，周期和定时使用
  CTResponse<GroupCreateRuleResponseData> createGroupRules(GroupCreateRuleRequest request) {
    return client.createGroupRules(request);
  }
  //修改定时策略，周期和定时使用
  def modifyScheduledAction(UpsertCtyunScheduledActionDescription description) {
    log.info("modifyScheduledAction--修改定时策略--start")
    try {
      //启动中，先停用再启动
      Integer oldStatus=2//默认停用状态
      boolean canModify=true//是否可以修改

      if(description.status==1){
        oldStatus=1//设置原状态为启用状态
        canModify=false
        canModify= stopRule(description.getGroupID(),description.getRuleID())
      }
      if(canModify) {
        description.setEffectiveFrom(!StringUtils.isEmpty(description.getEffectiveFrom())?description.getEffectiveFrom().replaceAll("/","-"):null)
        description.setEffectiveTill(!StringUtils.isEmpty(description.getEffectiveTill())?description.getEffectiveTill().replaceAll("/","-"):null)
        description.setExecutionTime(!StringUtils.isEmpty(description.getExecutionTime())?description.getExecutionTime().replaceAll("/","-"):null)
        if(description.getRuleType()==3){
          SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          String fromDate=description.getEffectiveFrom().substring(0,10)
          Long fromTime=df.parse(description.getEffectiveFrom()).getTime()
          Long fromExecutionTime=df.parse(fromDate+" "+description.getExecutionTime()).getTime()
          Long theExecutionTime=fromExecutionTime//执行时间,默认今天
          //如果取开始日期+执行时间不在范围内，就取第二天的这个时间，如果还不对，时间范围有问题
          if(fromTime>theExecutionTime){
            theExecutionTime=theExecutionTime+86400000//开始时间的第二天
          }
          log.info("采用时间={}",df.format(theExecutionTime))
          description.setExecutionTime(df.format(theExecutionTime))
        }

        RuleUpdateRequestBody requestBody = new RuleUpdateRequestBody().withRegionID(description.getRegionID())
          .withGroupId(description.getGroupID()).withRuleID(description.getRuleID())//.withName(description.getName())
          .withOperateUnit(description.getOperateUnit()).withOperateCount(description.getOperateCount())
          .withAction(description.getAction()).withExecutionTime(description.getExecutionTime()).withEffectiveFrom(description.getEffectiveFrom())
          .withEffectiveTill(description.getEffectiveTill()).withCooldown(description.getCooldown()).withCycle(description.getCycle())
          .withDay(description.getDay()).withTriggerObj(description.getTriggerInfo())
        RuleUpdateRequest request = new RuleUpdateRequest().withBody(requestBody)
        CTResponse<RuleUpdateResponseData> response = client.ruleUpdate request
        if(response.httpCode==200&&response.getData()!=null){
          RuleUpdateResponseData ruleUpdateResponseData=response.getData()
          if(ruleUpdateResponseData.getStatusCode()==800){
            log.info("modifyScheduledAction--修改定时策略，周期和定时使用--修改--完成！")
          }else{
            log.info("modifyScheduledAction--修改定时策略，周期和定时使用--修改--非800,错误码={}，错误信息={}",ruleUpdateResponseData.getErrorCode(),ruleUpdateResponseData.getDescription())
            throw new CtyunOperationException(ruleUpdateResponseData.getDescription())
          }
        }else{
          log.info("modifyScheduledAction--修改定时策略，周期和定时使用--修改--非200！{}",response)
          throw new CtyunOperationException(response.getMessage())
        }
      }
      //如果原先启动状态，需要再重启，如果原先停用，现在就不用管了
      if(oldStatus==1){
        startRule(description.getGroupID(),description.getRuleID())
      }
    } catch (Exception e) {
      log.error("modifyScheduledAction--修改定时策略，周期和定时使用--Exception",e)
      throw new CtyunOperationException(e)
    }
  }
//删除定时策略
  CTResponse<RuleDeleteResponseData> deleteScheduledAction(Integer ruleId, Integer groupId) {
    log.info("deleteScheduledAction--删除定时策略--start--ruleId--{}--groupId--{}",ruleId,groupId)
    try {
      RuleDeleteRequestBody requestBody = new RuleDeleteRequestBody()
        .withRegionID(regionId).withRuleID(ruleId).withGroupID(groupId)
      RuleDeleteRequest request = new RuleDeleteRequest().withBody(requestBody)
      CTResponse<RuleDeleteResponseData> response = client.ruleDeleteScheduled(request)
      log.info("deleteScheduledAction--删除定时策略--end--返回：{}", JSON.toJSONString(response))
      response
    } catch (Exception e) {
      log.error("deleteScheduledAction--删除定时策略--Exception",e)
      throw new CtyunOperationException(e)
    }
  }
  //创建弹性伸缩定时策略
  Integer createGroupScheduledRules(GroupCreateScheduledRuleRequest request) {
    log.info("createGroupScheduledRules--创建弹性伸缩定时策略--start")
    try {
      CTResponse<GroupCreateScheduledRuleResponseData> response= client.createGroupScheduledRules(request);
      if(response.httpCode==200&&response.getData()!=null){
        GroupCreateScheduledRuleResponseData groupCreateScheduledRuleResponseData=response.getData()
        if(groupCreateScheduledRuleResponseData.getStatusCode()==800){
          log.info("createGroupScheduledRules--创建弹性伸缩定时策略--完成--end")
          return response.getData().getReturnObj().getRuleID()
        }else{
          log.info("createGroupScheduledRules--创建弹性伸缩定时策略--非800,错误码={}，错误信息={}",groupCreateScheduledRuleResponseData.getErrorCode(),groupCreateScheduledRuleResponseData.getDescription())
          throw new CtyunOperationException(groupCreateScheduledRuleResponseData.getDescription())
        }
      }else{
        log.info("createGroupScheduledRules--创建弹性伸缩定时策略--非200！{}",response)
        throw new CtyunOperationException(response.getMessage())
      }
    } catch (Exception e) {
      log.error("deleteScheduledAction--创建弹性伸缩定时策略--Exception",e)
      throw new CtyunOperationException(e)
    }

  }
  //创建弹性伸缩周期策略
  Integer createGroupCycleRules(GroupCreateCycleRuleRequest request) {
    log.info("createGroupCycleRules--创建弹性伸缩周期策略--start")
    try {
      CTResponse<GroupCreateCycleRuleResponseData> response= client.createGroupCycleRules(request);
      if(response.httpCode==200&&response.getData()!=null){
        GroupCreateScheduledRuleResponseData groupCreateCycleRuleResponseData=response.getData()
        if(groupCreateCycleRuleResponseData.getStatusCode()==800){
          log.info("createGroupCycleRules--创建弹性伸缩周期策略--完成--end")
          return response.getData().getReturnObj().getRuleID()
        }else{
          log.info("createGroupCycleRules--创建弹性伸缩周期策略--非800,错误码={}，错误信息={}",groupCreateCycleRuleResponseData.getErrorCode(),groupCreateCycleRuleResponseData.getDescription())
          throw new CtyunOperationException(groupCreateCycleRuleResponseData.getDescription())
        }
      }else{
        log.info("createGroupCycleRules--创建弹性伸缩周期策略--非200！{}",response)
        throw new CtyunOperationException(response.getMessage())
      }
    } catch (Exception e) {
      log.error("createGroupCycleRules--创建弹性伸缩周期策略--Exception",e)
      throw new CtyunOperationException(e)
    }
  }
}
