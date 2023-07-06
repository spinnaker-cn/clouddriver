package com.netflix.spinnaker.clouddriver.ctyun.deploy.description

class EnableDisableCtyunServerGroupDescription extends AbstractCtyunCredentialsDescription {
  String serverGroupName
  String region
  String accountName
}
