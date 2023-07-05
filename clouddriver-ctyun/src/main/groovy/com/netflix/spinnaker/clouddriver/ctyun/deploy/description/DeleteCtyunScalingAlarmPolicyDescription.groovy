package com.netflix.spinnaker.clouddriver.ctyun.deploy.description

class DeleteCtyunScalingAlarmPolicyDescription extends AbstractCtyunCredentialsDescription {
  Integer scalingPolicyId
  //TODO YJS: 这里改为groupId，要确定是否能穿groupId进来
  Integer groupId
  String serverGroupName
  String region
  String accountName
}
