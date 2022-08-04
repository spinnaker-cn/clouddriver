package com.netflix.spinnaker.clouddriver.hecloud.deploy.description

class EnableDisableHeCloudServerGroupDescription extends AbstractHeCloudCredentialsDescription {
  String serverGroupName
  String region
  String accountName
}
