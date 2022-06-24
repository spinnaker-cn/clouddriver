package com.netflix.spinnaker.clouddriver.hecloud.model

import com.netflix.spinnaker.clouddriver.model.Network
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudProvider

class HeCloudNetwork implements Network{
  String cloudProvider = HeCloudProvider.ID
  String id
  String name
  String account
  String region
  String cidrBlock
}
