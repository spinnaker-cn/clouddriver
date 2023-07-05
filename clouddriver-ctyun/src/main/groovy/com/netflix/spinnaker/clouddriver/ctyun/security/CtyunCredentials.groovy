package com.netflix.spinnaker.clouddriver.ctyun.security

class CtyunCredentials {
  final String accessKey
  final String securityKey

  CtyunCredentials(String accessKey, String securityKey)
  {
    this.accessKey = accessKey
    this.securityKey = securityKey
  }
}
