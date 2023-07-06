package com.netflix.spinnaker.clouddriver.ctyun.model

import com.netflix.spinnaker.clouddriver.model.KeyPair

class CtyunKeyPair implements KeyPair{
  String account
  String region
  String keyId
  String keyName
  String keyFingerprint
}
