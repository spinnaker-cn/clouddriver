package com.netflix.spinnaker.clouddriver.hecloud.deploy.description

import com.netflix.spinnaker.clouddriver.security.resources.CredentialsNameable
import com.netflix.spinnaker.clouddriver.hecloud.security.HeCloudNamedAccountCredentials

class AbstractHeCloudCredentialsDescription implements CredentialsNameable {
  HeCloudNamedAccountCredentials credentials
}
