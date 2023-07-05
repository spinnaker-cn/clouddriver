package com.netflix.spinnaker.clouddriver.ctyun.model

class CtyunSecurityGroupDescription {
  String securityGroupId
  String securityGroupName
  String securityGroupDesc
  List<CtyunSecurityGroupRule> inRules
  List<CtyunSecurityGroupRule> outRules
  long lastReadTime
  String vpcId
}
