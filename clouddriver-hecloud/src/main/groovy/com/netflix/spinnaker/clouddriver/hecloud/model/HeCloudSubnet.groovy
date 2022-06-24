package com.netflix.spinnaker.clouddriver.hecloud.model

import com.netflix.spinnaker.clouddriver.model.Subnet
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudProvider

class HeCloudSubnet implements Subnet {
  final String type = HeCloudProvider.ID
  String name
  String id
  String account
  String region
  String vpcId
  String cidrBlock
  String purpose
}
