package com.netflix.spinnaker.clouddriver.hecloud.model.loadbalance

import groovy.transform.Canonical

@Canonical
class HeCloudLoadBalancerRuleTarget {
  String poolId
  String domain
  String url
  List<HeCloudLoadBalancerTarget> targets
}
