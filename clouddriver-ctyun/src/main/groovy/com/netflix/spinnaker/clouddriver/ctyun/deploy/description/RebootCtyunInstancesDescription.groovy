package com.netflix.spinnaker.clouddriver.ctyun.deploy.description

class RebootCtyunInstancesDescription extends AbstractCtyunCredentialsDescription {
  String serverGroupName
  List<String> instanceIds
  String region
  String accountName
}
