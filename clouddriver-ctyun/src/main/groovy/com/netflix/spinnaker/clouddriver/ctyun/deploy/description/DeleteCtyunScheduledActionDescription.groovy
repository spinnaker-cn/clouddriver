package com.netflix.spinnaker.clouddriver.ctyun.deploy.description

class DeleteCtyunScheduledActionDescription extends AbstractCtyunCredentialsDescription {
  String scalingPolicyId
  Integer scheduledActionId
  String serverGroupName
  String ruleType
  Integer groupId
  String region
  String accountName
}
