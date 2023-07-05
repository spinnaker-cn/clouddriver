package com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance

import groovy.transform.Canonical

@Canonical
class CtyunLoadBalancerHealthCheck {
  Integer healthSwitch;
  Integer timeOut;
  Integer intervalTime;
  Integer healthNum;
  Integer unHealthNum;
  String httpCode;
  String httpCheckPath;
  String httpCheckDomain;
  String httpCheckMethod;

  void copyHealthCheck(CtyunLoadBalancerHealthCheck health) {
    if (health != null) {
      this.healthSwitch = health.healthSwitch
      this.timeOut = health.timeOut
      this.intervalTime = health.intervalTime
      this.healthNum = health.healthNum
      this.unHealthNum = health.unHealthNum
      this.httpCode = health.httpCode
      this.httpCheckPath = health.httpCheckPath
      this.httpCheckDomain = health.httpCheckDomain
      this.httpCheckMethod = health.httpCheckMethod
    }
  }
}
