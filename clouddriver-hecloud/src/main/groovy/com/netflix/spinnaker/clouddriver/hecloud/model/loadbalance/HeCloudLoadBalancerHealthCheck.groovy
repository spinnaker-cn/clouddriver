package com.netflix.spinnaker.clouddriver.hecloud.model.loadbalance

import groovy.transform.Canonical

@Canonical
class HeCloudLoadBalancerHealthCheck {
  Integer timeOut;
  Integer intervalTime;
  Integer maxRetries;
  String httpCheckPath;
  String httpCheckDomain;

  void copyHealthCheck(HeCloudLoadBalancerHealthCheck health) {
    if (health != null) {
      this.timeOut = health.timeOut
      this.intervalTime = health.intervalTime
      this.maxRetries = health.maxRetries
      this.httpCheckPath = health.httpCheckPath
      this.httpCheckDomain = health.httpCheckDomain
    }
  }
}
