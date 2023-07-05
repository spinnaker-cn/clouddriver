package com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance

import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunSecurityGroupRule
import groovy.transform.Canonical

@Canonical
class CtyunLoadBalancerRule {
  String locationId
  String domain;
  String url;
  Integer sessionExpireTime;
  CtyunLoadBalancerHealthCheck healthCheck;
  CtyunLoadBalancerCertificate certificate;
  String scheduler;
  List<CtyunLoadBalancerTarget> targets
  String ruleTargetGroupId
  String ruleTargetGroupName
}
