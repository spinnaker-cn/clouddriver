package com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance

class CtyunLoadBalancerTargetHealth {
  String instanceId
  Integer port
  Boolean healthStatus
  String loadBalancerId
  String listenerId
  String locationId
  String targetId;
  String targetGroupID;
}
