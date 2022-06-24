package com.netflix.spinnaker.clouddriver.hecloud.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.netflix.spinnaker.clouddriver.model.SecurityGroup
import com.netflix.spinnaker.clouddriver.model.SecurityGroupSummary
import com.netflix.spinnaker.clouddriver.model.securitygroups.Rule
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudProvider
import com.netflix.spinnaker.moniker.Moniker
import groovy.transform.Immutable


@Immutable
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class HeCloudSecurityGroup implements SecurityGroup{
  final String type = HeCloudProvider.ID
  final String cloudProvider = HeCloudProvider.ID
  final String id             //securityGroupId
  final String name           //securityGroupName
  final String description    //securityGroupDesc
  final String application
  final String accountName
  final String region

  final Set<Rule> inboundRules = []
  final Set<Rule> outboundRules = []

  List<HeCloudSecurityGroupRule> inRules
  List<HeCloudSecurityGroupRule> outRules

  void setMoniker(Moniker _ignored) {}

  @Override
  SecurityGroupSummary getSummary() {
    new HeCloudSecurityGroupSummary(name: name, id: id)
  }
}
