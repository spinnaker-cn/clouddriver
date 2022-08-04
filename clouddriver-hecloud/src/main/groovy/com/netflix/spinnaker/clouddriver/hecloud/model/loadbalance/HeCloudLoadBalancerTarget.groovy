package com.netflix.spinnaker.clouddriver.hecloud.model.loadbalance

import groovy.transform.Canonical

@Canonical
class HeCloudLoadBalancerTarget {
  String instanceId
  Integer port
  Integer weight
}
