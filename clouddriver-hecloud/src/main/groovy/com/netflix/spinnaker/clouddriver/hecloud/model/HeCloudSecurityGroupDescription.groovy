package com.netflix.spinnaker.clouddriver.hecloud.model

class HeCloudSecurityGroupDescription {
  String securityGroupId
  String securityGroupName
  String securityGroupDesc
  List<HeCloudSecurityGroupRule> inRules
  List<HeCloudSecurityGroupRule> outRules
  long lastReadTime
}
