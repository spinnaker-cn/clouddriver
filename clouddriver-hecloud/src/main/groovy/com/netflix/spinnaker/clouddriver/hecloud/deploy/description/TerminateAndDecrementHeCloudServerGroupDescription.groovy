package com.netflix.spinnaker.clouddriver.hecloud.deploy.description

class TerminateAndDecrementHeCloudServerGroupDescription extends AbstractHeCloudCredentialsDescription {
  String serverGroupName
  String region
  String instance
}
