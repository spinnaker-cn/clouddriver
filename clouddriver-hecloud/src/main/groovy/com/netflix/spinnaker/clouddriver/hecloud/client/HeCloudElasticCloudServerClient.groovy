package com.netflix.spinnaker.clouddriver.hecloud.client

import com.netflix.spinnaker.clouddriver.hecloud.constants.HeCloudConstants
import com.netflix.spinnaker.clouddriver.hecloud.exception.HeCloudOperationException

import com.huaweicloud.sdk.core.auth.BasicCredentials
import com.huaweicloud.sdk.core.exception.ServiceResponseException
import com.huaweicloud.sdk.core.http.HttpConfig
import com.huaweicloud.sdk.core.region.Region
import com.huaweicloud.sdk.ecs.v2.EcsClient
import com.huaweicloud.sdk.ecs.v2.model.*
import groovy.util.logging.Slf4j

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

@Slf4j
class HeCloudElasticCloudServerClient {
  private final DEFAULT_LIMIT = 100
  String region
  String account
  EcsClient client

  HeCloudElasticCloudServerClient(String accessKeyId, String accessSecretKey, String region, String account) {
    def auth = new BasicCredentials().withAk(accessKeyId).withSk(accessSecretKey).withIamEndpoint(HeCloudConstants.Region.getIamEndPoint(region))
    def regionId = new Region(region, "https://ecs." + region + "." + HeCloudConstants.END_POINT_SUFFIX)
    def config = HttpConfig.getDefaultHttpConfig()
    this.region = region
    this.account = account
    client = EcsClient.newBuilder()
      .withHttpConfig(config)
      .withCredential(auth)
      .withRegion(regionId)
      .build()
  }

  def terminateInstances(List<String> instanceIds) {
    try {
      def request = new BatchStopServersRequest()
      def body = new BatchStopServersRequestBody()
      def ops = new BatchStopServersOption()
      ops.setType(BatchStopServersOption.TypeEnum.SOFT)
      def servers = instanceIds.collect {
        def server = new ServerId().withId(it)
        server
      }
      ops.setServers(servers)
      body.setOsStop(ops)
      request.setBody(body)
      client.batchStopServers(request)
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.toString())
    }
  }

  def rebootInstances(List<String> instanceIds) {
    try {
      def request = new BatchRebootServersRequest()
      def body = new BatchRebootServersRequestBody()
      def ops = new BatchRebootSeversOption()
      ops.setType(BatchRebootSeversOption.TypeEnum.SOFT)
      def servers = instanceIds.collect {
        def server = new ServerId().withId(it)
        server
      }
      ops.setServers(servers)
      body.setReboot(ops)
      request.setBody(body)
      client.batchRebootServers(request)
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.toString())
    }
  }

  def getInstanceTypes() {
    try {
      def request = new ListFlavorsRequest()
      def response = client.listFlavors(request)
      response.getFlavors()
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }

  def getKeyPairs() {
    try {
      def request = new NovaListKeypairsRequest()
      def response = client.novaListKeypairs(request)
      response.getKeypairs()
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }

  def getInstances() {
    def startNumber = 0
    List<ServerDetail> instanceAll = []
    while (true) {
      def req = new ListServersDetailsRequest().withLimit(DEFAULT_LIMIT).withOffset(startNumber)
      def resp
      try {
        resp = client.listServersDetails(req)
      } catch (ServiceResponseException e) {
        log.error(
          "Unable to listServersDetails (limit: {}, startNumber: {}, region: {}, account: {})",
          String.valueOf(DEFAULT_LIMIT),
          String.valueOf(startNumber),
          region,
          account,
          e
        )
      }
      if (resp == null || resp.getServers() == null || resp.getServers().size() == 0) {
        break
      }
      instanceAll.addAll(resp.getServers())
      startNumber += DEFAULT_LIMIT

    }
    return instanceAll
  }


  def getInstancesByIp(String ip) {
    def startNumber = 0
    List<ServerDetail> instanceAll = []
    while (true) {
      def req = new ListServersDetailsRequest().withIp(ip).withLimit(DEFAULT_LIMIT).withOffset(startNumber)
      def resp
      try {
        resp = client.listServersDetails(req)
      } catch (ServiceResponseException e) {
        log.error(
          "Unable to listServersDetails (limit: {}, startNumber: {}, region: {}, account: {})",
          String.valueOf(DEFAULT_LIMIT),
          String.valueOf(startNumber),
          region,
          account,
          e
        )
      }
      if (resp == null || resp.getServers() == null || resp.getServers().size() == 0) {
        break
      }
      instanceAll.addAll(resp.getServers())
      startNumber += DEFAULT_LIMIT
    }
    return instanceAll
  }

  def getInstanceTags(String instanceId) {
    try {
      def request = new ShowServerTagsRequest().withServerId(instanceId)
      def response = client.showServerTags(request)
      response.getTags()
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }

  static Date ConvertIsoDateTime(String isoDateTime) {
    Date date = null;
    try {
      DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME
      TemporalAccessor accessor = timeFormatter.parse(isoDateTime)
      date = Date.from(Instant.from(accessor))
    } catch (Exception e) {
      log.warn "convert string time error ${e.toString()}"
    }
    if (date == null) {
      try {
        isoDateTime = isoDateTime.substring(0, 11).replaceAll("\\.", "");
        date = new Date(Long.valueOf(isoDateTime) * 1000);
      } catch (Exception e) {
        log.warn "convert timeStamp time error ${e.toString()}"
      }
    }
    return date
  }

}
