package com.netflix.spinnaker.clouddriver.ctyun.model

import com.netflix.spinnaker.clouddriver.model.Subnet
import com.netflix.spinnaker.clouddriver.ctyun.CtyunCloudProvider

class CtyunSubnet implements Subnet {
  final String type = CtyunCloudProvider.ID
  String name
  String id
  String account
  String region
  String vpcId
  String cidrBlock
  Boolean isDefault
  String zone
  List myAllZones
  String purpose
  List eips
}
