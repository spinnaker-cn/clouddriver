package com.netflix.spinnaker.clouddriver.ctyun.deploy.description

import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunSecurityGroupRule
import groovy.transform.AutoClone
import groovy.transform.Canonical

@AutoClone
@Canonical
class UpsertCtyunSecurityGroupDescription extends AbstractCtyunCredentialsDescription {
  String application
  String accountName
  String region
  //新增加入参，关联的vpcID
  String vpcId

  String securityGroupId
  String securityGroupName
  String securityGroupDesc
  List<CtyunSecurityGroupRule> inRules
  List<CtyunSecurityGroupRule> outRules
}
