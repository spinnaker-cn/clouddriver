package com.netflix.spinnaker.clouddriver.ctyun.deploy.description

import groovy.transform.AutoClone
import groovy.transform.Canonical

@AutoClone
@Canonical
class DestroyCtyunServerGroupDescription extends AbstractCtyunCredentialsDescription {
  String serverGroupName
  String region
  String accountName
}
