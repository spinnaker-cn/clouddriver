package com.netflix.spinnaker.clouddriver.ctyun.deploy.description

import groovy.transform.AutoClone
import groovy.transform.Canonical

@AutoClone
@Canonical
class DeleteCtyunSecurityGroupDescription extends AbstractCtyunCredentialsDescription {
  String accountName
  String region
  String securityGroupId
}
