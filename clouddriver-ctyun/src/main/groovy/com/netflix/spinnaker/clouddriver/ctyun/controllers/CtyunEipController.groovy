package com.netflix.spinnaker.clouddriver.ctyun.controllers

import com.netflix.spinnaker.clouddriver.ctyun.client.CloudVirtualMachineClient
import com.netflix.spinnaker.clouddriver.ctyun.client.VirtualPrivateCloudClient
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.CtyunZoneProvider
import com.netflix.spinnaker.clouddriver.ctyun.security.CtyunNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

/*
弹性ip接口
*/

@RestController
@RequestMapping("/ctyun/{account}")
class CtyunEipController {

  @Autowired
  AccountCredentialsProvider accountCredentialsProvider

  @Autowired
  CtyunZoneProvider ctyunZoneProviders

  @RequestMapping(value = "/{region}/getEips", method = RequestMethod.GET)
  ResponseEntity getEips(
    @PathVariable String account,
    @PathVariable String region) {
    def credentials = accountCredentialsProvider.getCredentials(account)
    if (!(credentials instanceof CtyunNamedAccountCredentials)) {
      return new ResponseEntity(
        [message: "${account} is not ctyun credential type"],
        HttpStatus.BAD_REQUEST
      )
    }
    def client = new VirtualPrivateCloudClient(
      credentials.credentials.accessKey,
      credentials.credentials.securityKey,
      region
    )
    def eips = client.getEipsDownAll()
    return new ResponseEntity(eips, HttpStatus.OK)
  }
}
