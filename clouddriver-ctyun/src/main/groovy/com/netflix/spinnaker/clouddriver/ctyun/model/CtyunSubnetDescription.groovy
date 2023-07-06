package com.netflix.spinnaker.clouddriver.ctyun.model

class CtyunSubnetDescription {
  String subnetId
  String vpcId
  String subnetName
  String cidrBlock
  Boolean isDefault
  String zone
  List myAllZones
  List eips
}
