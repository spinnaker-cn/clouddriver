package com.netflix.spinnaker.clouddriver.hecloud.security

class HeCloudCredentials {
  final String accessKeyId
  final String accessSecretKey

  HeCloudCredentials(String accessKeyId, String accessSecretKey)
  {
    this.accessKeyId = accessKeyId
    this.accessSecretKey = accessSecretKey
  }
}
