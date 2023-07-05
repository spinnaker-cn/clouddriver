package com.netflix.spinnaker.clouddriver.ctyun.deploy.description

class TerminateAndDecrementCtyunServerGroupDescription extends AbstractCtyunCredentialsDescription {
  String serverGroupName
  String region
  Integer instance //id
  List<String> instanceIds //instanceId
}
