package com.netflix.spinnaker.clouddriver.hecloud.model.loadbalance

class HeCloudLoadBalancerTargetHealth {
  String instanceId
  Integer port
  Boolean healthStatus
  String loadBalancerId
  String listenerId
  String poolId
}
