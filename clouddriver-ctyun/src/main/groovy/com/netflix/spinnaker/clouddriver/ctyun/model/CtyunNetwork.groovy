package com.netflix.spinnaker.clouddriver.ctyun.model

import com.netflix.spinnaker.clouddriver.model.Network
import com.netflix.spinnaker.clouddriver.ctyun.CtyunCloudProvider

class CtyunNetwork implements Network{
  String cloudProvider = CtyunCloudProvider.ID
  String id
  String name
  String account
  String region
  String cidrBlock
  Boolean isDefault
}
