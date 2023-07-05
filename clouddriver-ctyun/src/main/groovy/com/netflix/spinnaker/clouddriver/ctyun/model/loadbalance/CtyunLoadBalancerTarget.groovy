package com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance

import groovy.transform.Canonical

@Canonical
class CtyunLoadBalancerTarget {
  String instanceId;
  Integer port;
  String type;
  Integer weight;
  String targetId;
  String targetGroupID;
  String healthCheckStatus
  String healthCheckStatusIpv6
}
