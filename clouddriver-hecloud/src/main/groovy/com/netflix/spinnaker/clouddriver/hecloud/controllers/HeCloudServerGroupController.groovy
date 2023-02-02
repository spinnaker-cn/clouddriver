package com.netflix.spinnaker.clouddriver.hecloud.controllers

import com.netflix.spinnaker.clouddriver.security.AccountCredentialsProvider
import com.netflix.spinnaker.clouddriver.hecloud.client.HeCloudAutoScalingClient
import com.netflix.spinnaker.clouddriver.hecloud.provider.view.HeCloudClusterProvider
import com.netflix.spinnaker.clouddriver.hecloud.security.HeCloudNamedAccountCredentials
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/*
curl -X GET \
  'http://localhost:7002/applications/myapp/clusters/test/myapp-dev/heweicloud/serverGroups/myapp-dev-v007/scalingActivities?region=ap-guangzhou' \
  -H 'cache-control: no-cache'
*/

@RestController
@RequestMapping("/applications/{application}/clusters/{account}/{clusterName}/hecloud/serverGroups/{serverGroupName}")
class HeCloudServerGroupController {
  //final static int MAX_SCALING_ACTIVITIES = 500
  final static int MAX_SCALING_ACTIVITIES = 100

  @Autowired
  AccountCredentialsProvider accountCredentialsProvider

  @Autowired
  HeCloudClusterProvider heCloudClusterProvider

  @RequestMapping(value = "/scalingActivities", method = RequestMethod.GET)
  ResponseEntity getScalingActivities(
    @PathVariable String account,
    @PathVariable String serverGroupName,
    @RequestParam(value = "region", required = true) String region) {
    def credentials = accountCredentialsProvider.getCredentials(account)
    if (!(credentials instanceof HeCloudNamedAccountCredentials)) {
      return new ResponseEntity(
        [message: "${account} is not hecloud credential type"],
        HttpStatus.BAD_REQUEST
      )
    }
    def serverGroup = heCloudClusterProvider.getServerGroup(account, region, serverGroupName, false)
    String autoScalingGroupId = serverGroup.asg.autoScalingGroupId
    def client = new HeCloudAutoScalingClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region,
      account
    )
    def scalingActivities = client.getAutoScalingActivitiesByAsgId(autoScalingGroupId, MAX_SCALING_ACTIVITIES)
    return new ResponseEntity(scalingActivities, HttpStatus.OK)
  }
}
