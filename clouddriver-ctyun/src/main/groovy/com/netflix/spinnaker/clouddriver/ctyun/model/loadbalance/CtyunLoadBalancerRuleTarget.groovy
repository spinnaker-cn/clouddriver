package com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance

import groovy.transform.Canonical

@Canonical
class CtyunLoadBalancerRuleTarget {
  String locationId;
  String domain;
  String url;
  List<CtyunLoadBalancerTarget> targets;
}
