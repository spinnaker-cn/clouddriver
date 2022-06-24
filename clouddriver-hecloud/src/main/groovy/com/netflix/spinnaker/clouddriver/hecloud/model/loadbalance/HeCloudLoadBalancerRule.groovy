package com.netflix.spinnaker.clouddriver.hecloud.model.loadbalance

import groovy.transform.Canonical

@Canonical
class HeCloudLoadBalancerRule {
  String policyId
  String poolId
  String domain
  String url
  HeCloudLoadBalancerHealthCheck healthCheck
  List<HeCloudLoadBalancerTarget> targets
}
