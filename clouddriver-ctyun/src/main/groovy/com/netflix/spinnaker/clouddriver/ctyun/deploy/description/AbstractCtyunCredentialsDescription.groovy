package com.netflix.spinnaker.clouddriver.ctyun.deploy.description

import com.netflix.spinnaker.clouddriver.security.resources.CredentialsNameable
import com.netflix.spinnaker.clouddriver.ctyun.security.CtyunNamedAccountCredentials

class AbstractCtyunCredentialsDescription implements CredentialsNameable {
  CtyunNamedAccountCredentials credentials
}
