package com.netflix.spinnaker.clouddriver.ctyun.model

import com.netflix.spinnaker.clouddriver.model.InstanceType
import groovy.transform.Canonical

@Canonical
class CtyunInstanceType implements InstanceType {
  String name
  String region
  String zone
  String account
  Integer cpu
  Integer mem
  String instanceFamily
}
