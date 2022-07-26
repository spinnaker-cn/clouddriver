package com.netflix.spinnaker.clouddriver.hecloud.client

import com.netflix.spinnaker.clouddriver.hecloud.constants.HeCloudConstants
import com.netflix.spinnaker.clouddriver.hecloud.exception.HeCloudOperationException

import com.huaweicloud.sdk.core.auth.BasicCredentials
import com.huaweicloud.sdk.core.exception.ServiceResponseException
import com.huaweicloud.sdk.core.http.HttpConfig
import com.huaweicloud.sdk.core.region.Region
import com.huaweicloud.sdk.ces.v1.CesClient
import com.huaweicloud.sdk.ces.v1.model.*
import groovy.util.logging.Slf4j

@Slf4j
class HeCloudEyeClient {
  CesClient client

  HeCloudEyeClient(String accessKeyId, String accessSecretKey, String region){
    def auth = new BasicCredentials().withAk(accessKeyId).withSk(accessSecretKey).withIamEndpoint(HeCloudConstants.Region.getIamEndPoint(region))
    def regionId = new Region(region, "https://ces." + region + "." + HeCloudConstants.END_POINT_SUFFIX)
    def config = HttpConfig.getDefaultHttpConfig()
    client = CesClient.newBuilder()
        .withHttpConfig(config)
        .withCredential(auth)
        .withRegion(regionId)
        .build()
  }

  MetricAlarms getAlarm(String alarmId) {
    try {
      def req = new ShowAlarmRequest()
      req.setAlarmId(alarmId)
      def resp = client.showAlarm(req)
      def alarms = resp.getMetricAlarms()
      if (alarms.size() == 0) {
        throw new HeCloudOperationException("alarm $alarmId  not found")
      }
      alarms[0]
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }

  def createAlarm(MetricAlarms sourceAlarm, String asgId) {
    try {
      def request = new CreateAlarmRequest()
      def body = new CreateAlarmRequestBody()

      def alarmName = "spinnaker-" + new Date().time.toString() + "-" + asgId
      body.setAlarmName(alarmName)
      body.setAlarmDescription(sourceAlarm.getAlarmDescription())
      def metric = sourceAlarm.getMetric()
      def demintions = metric.getDimensions()
      if (demintions.size() > 0) {
        def dimention = demintions[0]
        dimention.setValue(asgId)
        demintions = [dimention]
        metric.setDimensions(demintions)
      }
      body.setMetric(metric)
      body.setCondition(sourceAlarm.getCondition())
      body.setAlarmActions(sourceAlarm.getAlarmActions())
      request.setBody(body)
      def response = client.createAlarm request
      response
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.toString())
    }
  }

}
