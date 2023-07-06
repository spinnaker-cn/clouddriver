package com.netflix.spinnaker.clouddriver.ctyun.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.netflix.spinnaker.clouddriver.model.SecurityGroup
import com.netflix.spinnaker.clouddriver.model.SecurityGroupSummary
import com.netflix.spinnaker.clouddriver.model.securitygroups.Rule
import com.netflix.spinnaker.clouddriver.ctyun.CtyunCloudProvider
import com.netflix.spinnaker.moniker.Moniker
import groovy.transform.Immutable


@Immutable
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class CtyunSecurityGroup implements SecurityGroup{
  final String type = CtyunCloudProvider.ID
  final String cloudProvider = CtyunCloudProvider.ID
  final String id             //securityGroupId
  final String name           //securityGroupName
  final String description
  final String application
  final String accountName
  final String region
  final String vpcId
  final Set<Rule> inboundRules = []
  final Set<Rule> outboundRules = []

  List<CtyunSecurityGroupRule> inRules
  List<CtyunSecurityGroupRule> outRules

  void setMoniker(Moniker _ignored) {}

  @Override
  SecurityGroupSummary getSummary() {
    new CtyunSecurityGroupSummary(name: name, id: id,vpcId: vpcId)
  }
}
