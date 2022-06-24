package com.netflix.spinnaker.clouddriver.hecloud.model

import com.netflix.spinnaker.clouddriver.model.KeyPair

class HeCloudKeyPair implements KeyPair{
  String account
  String region
  String keyId
  String keyName
  String keyFingerprint
}
