package com.netflix.spinnaker.clouddriver.ctyun.client

import cn.ctyun.ctapi.CTResponse
import cn.ctyun.ctapi.ctvm.CtvmClient
import cn.ctyun.ctapi.ctvm.availabilityzonesdetails.AvailabilityZonesDetails
import cn.ctyun.ctapi.ctvm.availabilityzonesdetails.AvailabilityZonesRequest
import cn.ctyun.ctapi.ctvm.availabilityzonesdetails.AvailabilityZonesResponseData
import cn.ctyun.ctapi.ctvm.batchrebootinstances.BatchRebootInstancesBody
import cn.ctyun.ctapi.ctvm.batchrebootinstances.BatchRebootInstancesData
import cn.ctyun.ctapi.ctvm.batchrebootinstances.BatchRebootInstancesRequest
import cn.ctyun.ctapi.ctvm.batchstartinstances.BatchStartInstancesBody
import cn.ctyun.ctapi.ctvm.batchstartinstances.BatchStartInstancesData
import cn.ctyun.ctapi.ctvm.batchstartinstances.BatchStartInstancesRequest
import cn.ctyun.ctapi.ctvm.batchstopinstances.BatchStopInstancesBody
import cn.ctyun.ctapi.ctvm.batchstopinstances.BatchStopInstancesData
import cn.ctyun.ctapi.ctvm.batchstopinstances.BatchStopInstancesRequest
import cn.ctyun.ctapi.ctvm.getzones.GetZonesDetails
import cn.ctyun.ctapi.ctvm.getzones.GetZonesRequest
import cn.ctyun.ctapi.ctvm.getzones.GetZonesResponseData
import cn.ctyun.ctapi.ctvm.listflavor.ListFlavor
import cn.ctyun.ctapi.ctvm.listflavor.ListFlavorRequest
import cn.ctyun.ctapi.ctvm.listflavor.ListFlavorRequestBody
import cn.ctyun.ctapi.ctvm.listflavor.ListFlavorResponseData
import cn.ctyun.ctapi.ctvm.listkeypair.ListKeypair
import cn.ctyun.ctapi.ctvm.listkeypair.ListKeypairRequest
import cn.ctyun.ctapi.ctvm.listkeypair.ListKeypairRequestBody
import cn.ctyun.ctapi.ctvm.listkeypair.ListKeypairResponseData
import cn.ctyun.ctapi.ctvm.regionsdetails.RegionsDetails
import cn.ctyun.ctapi.ctvm.regionsdetails.RegionsDetailsRequest
import cn.ctyun.ctapi.ctvm.regionsdetails.RegionsDetailsResponseData
import cn.ctyun.ctapi.image.ImageClient
import cn.ctyun.ctapi.image.listimage.ListImageRequest
import cn.ctyun.ctapi.image.listimage.ListImageResponseData
import cn.ctyun.ctapi.image.listimage.ListImagesInfo
import cn.ctyun.ctapi.scaling.ScalingClient
import cn.ctyun.ctapi.scaling.listinstances.ListInstance
import cn.ctyun.ctapi.scaling.listinstances.ListInstanceRequest
import cn.ctyun.ctapi.scaling.listinstances.ListInstanceRequestBody
import cn.ctyun.ctapi.scaling.listinstances.ListInstanceResponseData
import com.alibaba.fastjson.JSONObject
import com.netflix.spinnaker.clouddriver.ctyun.exception.CtyunOperationException
import groovy.util.logging.Slf4j

@Slf4j
class CloudVirtualMachineClient extends AbstractCtyunServiceClient {
  private final DEFAULT_LIMIT = 50
  final String endPoint = "ctecs-global.ctapi.ctyun.cn"//云主机相关
  final String endPointImage="ctimage-global.ctapi.ctyun.cn"//镜像相关
  private ImageClient imageClient
  private ScalingClient scalingClient;
  private CtvmClient ctvmClient;
  private String regionId

  CloudVirtualMachineClient(String secretId, String secretKey, String region) {
    super(secretId, secretKey)
   // client = new CvmClient(cred, region, clientProfile)
    scalingClient=new ScalingClient()
    scalingClient.init(cred, endPoint);
    imageClient=new ImageClient()
    imageClient.init(cred, endPointImage);
    ctvmClient=new CtvmClient()
    ctvmClient.init(cred, endPoint);
    regionId=region
  }
  //获取镜像
  List<ListImagesInfo> getImages(visibility) {
    log.info("getImages--获取所有公共镜像数据--start")
    List<ListImagesInfo> imageAll = []
    try {
      def pageNumber=1;
      def totalCount = DEFAULT_LIMIT
      def getCount = DEFAULT_LIMIT
      while(totalCount==getCount){
        ListImageRequest request = new ListImageRequest().withRegionID(regionId).withPageNo(pageNumber).withPageSize(DEFAULT_LIMIT).withVisibility(visibility);
        CTResponse<ListImageResponseData> response = imageClient.listImage(request);
        if(response.httpCode==200&&response.getData()!=null){
          ListImageResponseData listImageResponseData=response.getData()
          if(listImageResponseData.getStatusCode()==800){
            if(listImageResponseData.getReturnObj().getImages().size()>0){
              imageAll.addAll(listImageResponseData.getReturnObj().getImages())
            }
            pageNumber++;
            getCount = listImageResponseData.getReturnObj().getImages().size();
          }else{
            log.info("getImages--获取所有公共镜像数据--非800！pageNum={},错误码={}，错误信息={}",(pageNumber-1),listImageResponseData.getErrorCode(),listImageResponseData.getMessage())
          }
        }else{
          log.info("getImages--获取所有公共镜像数据--非200！{}",response.getMessage())
          //throw new CtyunOperationException(response.getMessage())
        }
      }
      log.info("getImages--获取所有公共镜像数据--end,size={}",imageAll.size())
      return imageAll
    } catch (Exception e) {
      log.error("getImages--获取所有公共镜像数据--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
  //获取所有实例规格
  def getInstanceTypes() {
    log.info("getInstanceTypes--获取所有实例规格--start")
    List<ListFlavor> instanceTypeAll = []
    try {
        ListFlavorRequestBody requestBody = new ListFlavorRequestBody().withRegionID(regionId);
        ListFlavorRequest request = new ListFlavorRequest().withBody(requestBody);
        CTResponse<ListFlavorResponseData> response = ctvmClient.listFlavor(request);
        if(response.httpCode==200&&response.getData()!=null){
          ListFlavorResponseData listFlavorResponseData=response.getData()
          if(listFlavorResponseData.getStatusCode()==800){
            if(listFlavorResponseData.getReturnObj().getFlavorList().size()>0){
              instanceTypeAll.addAll(listFlavorResponseData.getReturnObj().getFlavorList())
            }
          }else{
            log.info("getInstanceTypes--获取所有实例规格--非800！错误码={}，错误信息={}",listFlavorResponseData.getErrorCode(),listFlavorResponseData.getMessage())
          }
        }else{
          log.info("getInstanceTypes--获取所有实例规格--非200！{}",response.getMessage())
          //throw new CtyunOperationException(response.getMessage())
        }
      log.info("getInstanceTypes--获取所有实例规格--end,size={}",instanceTypeAll.size())
      return instanceTypeAll
    } catch (Exception e) {
      log.error("getInstanceTypes--获取所有实例规格--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
  //通过id获取实例
  def getInstanceById(String instanceId){
    log.info("getInstanceById--通过id获取实例--start--instanceId={}",instanceId)
    try {
        ListInstanceRequestBody requestBody = new ListInstanceRequestBody().withRegionID(regionId).withInstanceIDList(instanceId).withPageNo(1).withPageSize(1);
        ListInstanceRequest request = new ListInstanceRequest().withBody(requestBody);
        CTResponse<ListInstanceResponseData> response = scalingClient.listInstance(request);
        if(response.httpCode==200&&response.getData()!=null){
          ListInstanceResponseData listInstanceResponseData=response.getData()
          if(listInstanceResponseData.getStatusCode()==800){
            if(listInstanceResponseData.getReturnObj().getResults().size()>0){
              listInstanceResponseData.getReturnObj().getResults().get(0)
            }
          }else{
            log.info("getInstanceById--通过id获取实例--非800！,错误码={}，错误信息={}",listInstanceResponseData.getErrorCode(),listInstanceResponseData.getMessage())
          }
        }else{
          log.info("getInstanceById--通过id获取实例--非200！{}",response.getMessage())
          //throw new CtyunOperationException(response.getMessage())
        }
    } catch (Exception e) {
      log.error("getInstanceById--通过id获取实例--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
//获取所有实例，如果有id列表，按照id列表获取
  def getInstances(List<String> instanceIds=[]) {
    log.info("getInstances--通过instanceIds获取实例--start--instanceIds={}",instanceIds)
    List<ListInstance> instanceAll = []
    try {
      def pageNumber=1;
      def totalCount = DEFAULT_LIMIT
      def getCount = DEFAULT_LIMIT
      while(totalCount==getCount){
        ListInstanceRequestBody requestBody = new ListInstanceRequestBody().withRegionID(regionId).withInstanceIDList(instanceIds?.join(",")).withPageNo(pageNumber).withPageSize(DEFAULT_LIMIT);
        ListInstanceRequest request = new ListInstanceRequest().withBody(requestBody);
        CTResponse<ListInstanceResponseData> response = scalingClient.listInstance(request);
        if(response.httpCode==200&&response.getData()!=null){
          ListInstanceResponseData listInstanceResponseData=response.getData()
          if(listInstanceResponseData.getStatusCode()==800){
            if(listInstanceResponseData.getReturnObj().getResults().size()>0){
              instanceAll.addAll(listInstanceResponseData.getReturnObj().getResults())
            }
            pageNumber++;
            getCount = listInstanceResponseData.getReturnObj().getResults().size();
          }else{
            log.info("getInstances--通过instanceIds获取实例--非800！pageNum={},错误码={}，错误信息={}",(pageNumber-1),listInstanceResponseData.getErrorCode(),listInstanceResponseData.getMessage())
          }
        }else{
          log.info("getInstances--通过instanceIds获取实例--非200！{}",response.getMessage())
          //throw new CtyunOperationException(response.getMessage())
        }
      }
      log.info("getInstances--通过instanceIds获取实例--end,size={}",instanceAll.size())
      return instanceAll
    } catch (Exception e) {
      log.error("getInstances--通过instanceIds获取实例--Exception",e)
      throw new CtyunOperationException(e.toString())
    }

  }
//获取密钥对
  List<ListKeypair> getKeyPairs() {
    log.info("getKeyPairs--获取密钥对--start")
    List<ListKeypair> keyPairAll = []
    try {
      def pageNumber=1;
      def totalCount = DEFAULT_LIMIT
      def getCount = DEFAULT_LIMIT
      while(totalCount==getCount){
        ListKeypairRequestBody requestBody = new ListKeypairRequestBody().withRegionID(regionId).withPageNo(pageNumber).withPageSize(DEFAULT_LIMIT);
        ListKeypairRequest request = new ListKeypairRequest().withBody(requestBody);
        CTResponse<ListKeypairResponseData> response = ctvmClient.listKeypair(request);
        if(response.httpCode==200&&response.getData()!=null){
          ListKeypairResponseData listKeypairResponseData=response.getData()
          if(listKeypairResponseData.getStatusCode()==800){
            if(listKeypairResponseData.getReturnObj().getResults().size()>0){
              keyPairAll.addAll(listKeypairResponseData.getReturnObj().getResults())
            }
            pageNumber++;
            getCount = listKeypairResponseData.getReturnObj().getResults().size();
          }else{
            log.info("getKeyPairs--获取密钥对--非800！pageNum={},错误码={}，错误信息={}",(pageNumber-1),listKeypairResponseData.getErrorCode(),listKeypairResponseData.getMessage())
          }
        }else{
          log.info("getKeyPairs--获取密钥对--非200！{}",response.getMessage())
          //throw new CtyunOperationException(response.getMessage())
        }
      }
      log.info("getKeyPairs--获取密钥对--end,size={}",keyPairAll.size())
      return keyPairAll
    } catch (Exception e) {
      log.error("getKeyPairs--获取密钥对--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
  //批量终止主机实例
  def terminateInstances(List<String> instanceIds) {
    log.info("terminateInstances--批量终止主机实例--start--instanceIds={}",instanceIds)
    try {
      BatchStopInstancesBody body = new BatchStopInstancesBody().withRegionID(regionId).withInstanceIDList(instanceIds?.join(","));
      BatchStopInstancesRequest request = new BatchStopInstancesRequest().withBody(body);
      CTResponse<BatchStopInstancesData> response = ctvmClient.batchStopInstances(request);
      if(response.httpCode==200&&response.getData()!=null){
        BatchStopInstancesData batchStopInstancesData=response.getData()
        if(batchStopInstancesData.getStatusCode()==800){
          log.info("terminateInstances--批量终止主机实例--end "+ JSONObject.toJSONString(batchStopInstancesData.getReturnObj())+" is ok!")
          return batchStopInstancesData.getReturnObj().getJobIDList()
        }else{
          log.info("terminateInstances--批量终止主机实例--非800！{}",batchStopInstancesData.getMessage())
          //throw new CtyunOperationException(batchStopInstancesData.getMessage())
        }
      }else{
        log.info("terminateInstances--批量终止主机实例--非200！{}",response.getMessage())
        throw new CtyunOperationException(response.getMessage())
      }
    } catch (Exception e) {
      log.error("terminateInstances--批量终止主机实例--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
//批量启动主机实例
  def startInstances(List<String> instanceIds) {
    log.info("startInstances--批量启动主机实例--start--instanceIds={}",instanceIds)
    try {
      BatchStartInstancesBody body = new BatchStartInstancesBody().withRegionID(regionId).withInstanceIDList(instanceIds?.join(","));
      BatchStartInstancesRequest request = new BatchStartInstancesRequest().withBody(body);
      CTResponse<BatchStartInstancesData> response = ctvmClient.batchStartInstances(request);

      if(response.httpCode==200&&response.getData()!=null){
        BatchStartInstancesData batchStartInstancesData=response.getData()
        if(batchStartInstancesData.getStatusCode()==800){
          log.info("startInstances--批量启动主机实例--end "+ JSONObject.toJSONString(batchStartInstancesData.getReturnObj())+" is ok!")
          return batchStartInstancesData.getReturnObj().getJobIDList()
        }else{
          log.info("startInstances--批量启动主机实例--非800！{}",batchStartInstancesData.getMessage())
          //throw new CtyunOperationException(batchStopInstancesData.getMessage())
        }
      }else{
        log.info("startInstances--批量启动主机实例--非200！{}",response.getMessage())
        throw new CtyunOperationException(response.getMessage())
      }
    } catch (Exception e) {
      log.error("startInstances--批量启动主机实例--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
//批量重启主机实例
  def rebootInstances(List<String> instanceIds) {
    log.info("rebootInstances--批量重启主机实例--start--instanceIds={}",instanceIds)
    try {

      BatchRebootInstancesBody body = new BatchRebootInstancesBody().withRegionID(regionId).withInstanceIDList(instanceIds?.join(","));
      BatchRebootInstancesRequest request = new BatchRebootInstancesRequest().withBody(body);
      CTResponse<BatchRebootInstancesData> response = ctvmClient.batchRebootInstances(request);
      if(response.httpCode==200&&response.getData()!=null){
        BatchRebootInstancesData batchRebootInstancesData=response.getData()
        if(batchRebootInstancesData.getStatusCode()==800){
          log.info("rebootInstances--批量重启主机实例--end "+ JSONObject.toJSONString(batchRebootInstancesData.getReturnObj())+" is ok!")
        }else{
          log.info("rebootInstances--批量重启主机实例--非800！{}",batchRebootInstancesData.getMessage())
          //throw new CtyunOperationException(batchRebootInstancesData.getMessage())
        }
      }else{
        log.info("rebootInstances--批量重启主机实例--非200！{}",response.getMessage())
        //throw new CtyunOperationException(response.getMessage())
      }
    } catch (Exception e) {
      log.error("rebootInstances--批量重启主机实例--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }


//获取资源池
  List<RegionsDetails> getRegionsDetails() {
    log.info("getRegionsDetails--获取资源池--start")
    try {

      RegionsDetailsRequest request = new RegionsDetailsRequest();
      CTResponse<RegionsDetailsResponseData> response = ctvmClient.getRegionsDetails(request);
      if(response.httpCode==200&&response.getData()!=null){
        RegionsDetailsResponseData regionsDetailsResponseData=response.getData()
        if(regionsDetailsResponseData.getStatusCode()==800){
           log.info("getRegionsDetails--获取资源池--end,size={}",regionsDetailsResponseData.getReturnObj().getRegionList().size())
           return regionsDetailsResponseData.getReturnObj().getRegionList()
        }else{
          log.info("getRegionsDetails--获取资源池--非800！,错误码={}，错误信息={}",regionsDetailsResponseData.getErrorCode(),regionsDetailsResponseData.getMessage())
        }
      }else{
        log.info("getRegionsDetails--获取资源池--非200！{}",response.getMessage())
        //throw new CtyunOperationException(response.getMessage())
      }
    } catch (Exception e) {
      log.error("getRegionsDetails--获取资源池--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
//获取区域
  List<GetZonesDetails> getZones() {
    log.info("getZones--获取区域--start")
    try {
      GetZonesRequest request = new GetZonesRequest().withRegionID(regionId);
      CTResponse<GetZonesResponseData> response = ctvmClient.getZondes(request);
      if(response.httpCode==200&&response.getData()!=null){
        GetZonesResponseData getZonesResponseData=response.getData()
        if(getZonesResponseData.getStatusCode()==800){
          log.info("getZones--获取区域--end,size={}",getZonesResponseData.getReturnObj().getZoneList().size())
          return getZonesResponseData.getReturnObj().getZoneList()
        }else{
          log.info("getZones--获取区域--非800！,错误码={}，错误信息={}",getZonesResponseData.getErrorCode(),getZonesResponseData.getMessage())
        }
      }else{
        log.info("getZones--获取区域--非200！{}",response.getMessage())
        //throw new CtyunOperationException(response.getMessage())
      }
    } catch (Exception e) {
      log.error("getZones--获取区域--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }

//获取我的账户下能用得区域
  List<AvailabilityZonesDetails> getMyZones() {
    log.info("getMyZones--获取我的账户下能用得区域--start")
    try {

      AvailabilityZonesRequest request = new AvailabilityZonesRequest().withRegionID(regionId);
      CTResponse<AvailabilityZonesResponseData> response = ctvmClient.getAvailabilityZones(request);
      if(response.httpCode==200&&response.getData()!=null){
        AvailabilityZonesResponseData availabilityZonesResponseData=response.getData()
        if(availabilityZonesResponseData.getStatusCode()==800){
          log.info("getMyZones--获取我的账户下能用得区域--end,size={}",availabilityZonesResponseData.getReturnObj().getAzList().size())
          return availabilityZonesResponseData.getReturnObj().getAzList()
        }else{
          log.info("getMyZones--获取我的账户下能用得区域--非800！,错误码={}，错误信息={}",availabilityZonesResponseData.getErrorCode(),availabilityZonesResponseData.getMessage())
        }
      }else{
        log.info("getMyZones--获取我的账户下能用得区域--非200！{}",response.getMessage())
        //throw new CtyunOperationException(response.getMessage())
      }
    } catch (Exception e) {
      log.error("getMyZones--获取我的账户下能用得区域--Exception",e)
      throw new CtyunOperationException(e.toString())
    }
  }
}
