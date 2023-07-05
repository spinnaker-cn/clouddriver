package com.netflix.spinnaker.clouddriver.ctyun.controllers

import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunAutoScalingClient
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.CtyunClusterProvider
import com.netflix.spinnaker.clouddriver.ctyun.security.CtyunNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/*
curl -X GET \
  'http://localhost:7002/applications/myapp/clusters/test/myapp-dev/tencent/serverGroups/myapp-dev-v007/scalingActivities?region=ap-guangzhou' \
  -H 'cache-control: no-cache'
*/

@RestController
@RequestMapping("/applications/{application}/clusters/{account}/{clusterName}/ctyun/serverGroups/{serverGroupName}")
class CtyunServerGroupController {
  final static int MAX_SCALING_ACTIVITIES = 100

  @Autowired
  AccountCredentialsProvider accountCredentialsProvider

  @Autowired
  CtyunClusterProvider ctyunClusterProvider

  @RequestMapping(value = "/scalingActivities", method = RequestMethod.GET)
  ResponseEntity getScalingActivities(
    @PathVariable String account,
    @PathVariable String serverGroupName,
    @RequestParam(value = "region", required = true) String region) {
    def credentials = accountCredentialsProvider.getCredentials(account)
    if (!(credentials instanceof CtyunNamedAccountCredentials)) {
      return new ResponseEntity(
        [message: "${account} is not tencent credential type"],
        HttpStatus.BAD_REQUEST
      )
    }
    def serverGroup = ctyunClusterProvider.getServerGroup(account, region, serverGroupName, false)
    Integer autoScalingGroupId = serverGroup.asg.groupID
    def client = new CtyunAutoScalingClient(
      credentials.credentials.accessKey,
      credentials.credentials.securityKey,
      region
    )
    def scalingActivities = client.getAutoScalingActivitiesByAsgId(autoScalingGroupId, MAX_SCALING_ACTIVITIES)
    return new ResponseEntity(scalingActivities, HttpStatus.OK)
  }
}
