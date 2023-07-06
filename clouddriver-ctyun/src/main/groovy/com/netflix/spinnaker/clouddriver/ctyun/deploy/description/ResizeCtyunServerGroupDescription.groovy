package com.netflix.spinnaker.clouddriver.ctyun.deploy.description

import groovy.transform.AutoClone
import groovy.transform.Canonical

@AutoClone
@Canonical
class ResizeCtyunServerGroupDescription extends AbstractCtyunCredentialsDescription {
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
