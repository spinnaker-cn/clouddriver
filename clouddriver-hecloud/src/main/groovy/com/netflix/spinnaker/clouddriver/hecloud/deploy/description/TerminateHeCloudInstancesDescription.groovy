package com.netflix.spinnaker.clouddriver.hecloud.deploy.description

class TerminateHeCloudInstancesDescription extends AbstractHeCloudCredentialsDescription {
  String serverGroupName
  List<String> instanceIds
  String region
  String accountName
}
