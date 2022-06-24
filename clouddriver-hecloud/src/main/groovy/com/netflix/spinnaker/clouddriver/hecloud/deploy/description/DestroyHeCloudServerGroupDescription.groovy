package com.netflix.spinnaker.clouddriver.hecloud.deploy.description

import groovy.transform.AutoClone
import groovy.transform.Canonical

@AutoClone
@Canonical
class DestroyHeCloudServerGroupDescription extends AbstractHeCloudCredentialsDescription {
  String serverGroupName
  String region
  String accountName
}
