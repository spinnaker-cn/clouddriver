package com.netflix.spinnaker.clouddriver.ctyun.deploy.description

class TerminateCtyunInstancesDescription extends AbstractCtyunCredentialsDescription {
  String serverGroupName
  List<String> instanceIds
  String region
  String accountName
}
