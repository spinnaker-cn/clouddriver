package com.netflix.spinnaker.clouddriver.hecloud.deploy.description

import groovy.transform.AutoClone
import groovy.transform.Canonical

@AutoClone
@Canonical
class ResizeHeCloudServerGroupDescription extends AbstractHeCloudCredentialsDescription {
  Capacity capacity
  String serverGroupName
  String region
  String accountName

  static class Capacity {
    Integer min
    Integer max
    Integer desired
  }
}
