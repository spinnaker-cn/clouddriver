package com.netflix.spinnaker.clouddriver.ctyun.controllers

import com.netflix.spinnaker.clouddriver.ctyun.client.CloudVirtualMachineClient
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunAutoScalingClient
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.CtyunClusterProvider
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.CtyunZoneProvider
import com.netflix.spinnaker.clouddriver.ctyun.security.CtyunNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.model.KeyPair
import com.netflix.spinnaker.clouddriver.model.KeyPairProvider
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
@RequestMapping("/ctyun/{account}")
class CtyunRegionsAndZonesController {

  @Autowired
  AccountCredentialsProvider accountCredentialsProvider

  @Autowired
  CtyunZoneProvider ctyunZoneProviders

  @RequestMapping(value = "/getRegions", method = RequestMethod.GET)
  ResponseEntity getRegions(
    @PathVariable String account) {
    def credentials = accountCredentialsProvider.getCredentials(account)
    if (!(credentials instanceof CtyunNamedAccountCredentials)) {
      return new ResponseEntity(
        [message: "${account} is not tencent credential type"],
        HttpStatus.BAD_REQUEST
      )
    }
    def client = new CloudVirtualMachineClient(
      credentials.credentials.accessKey,
      credentials.credentials.securityKey,
      null
    )
    def regionDetails = client.getRegionsDetails()
    return new ResponseEntity(regionDetails, HttpStatus.OK)
  }
  @RequestMapping(value = "/{region}/getZones", method = RequestMethod.GET)
  ResponseEntity getRegions(
    @PathVariable String account,
    @PathVariable String region) {
    /*def credentials = accountCredentialsProvider.getCredentials(account)
    if (!(credentials instanceof CtyunNamedAccountCredentials)) {
      return new ResponseEntity(
        [message: "${account} is not tencent credential type"],
        HttpStatus.BAD_REQUEST
      )
    }
    def client = new CloudVirtualMachineClient(
      credentials.credentials.accessKey,
      credentials.credentials.securityKey,
      region
    )
    def myZones = client.getMyZones()
    def zones=client.getZones()
    List list=new ArrayList()
    myZones.each {s->
      Map<String,String> map=new HashMap()
      zones.each {
        if(s.getAzName().equals(it.name)){
          map.put("azID",s.getAzID())
          map.put("azName",s.getAzName())
          map.put("azDisplayName",it.azDisplayName)
          list.add(map)
        }
      }
    }*/


    def list=ctyunZoneProviders.getAll(account,region)

    return new ResponseEntity(list, HttpStatus.OK)
  }
}
