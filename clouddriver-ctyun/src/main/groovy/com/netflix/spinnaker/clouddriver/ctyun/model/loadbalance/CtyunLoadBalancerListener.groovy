package com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance

import groovy.transform.Canonical


@Canonical
class CtyunLoadBalancerListener {
  String listenerId
  String listenerName
  String protocol
  Integer port
  CtyunLoadBalancerHealthCheck healthCheck
  CtyunLoadBalancerCertificate certificate
  Integer sessionExpireTime
  String scheduler
  Integer sniSwitch
  String targetGroupId
  String targetGroupName

  //target, tcp/udp 4 layer
  List<CtyunLoadBalancerTarget> targets

  //rule, http/https 7 layer
  List<CtyunLoadBalancerRule> rules

  void copyListener(CtyunLoadBalancerListener listener) {
    if (listener == null) {
      return
    }
    this.listenerId = listener.listenerId
    this.listenerName = listener.listenerName
    this.protocol = listener.protocol
    this.port = listener.port
    this.sessionExpireTime = listener.sessionExpireTime
    this.scheduler = listener.scheduler
    this.sniSwitch = listener.sniSwitch
    this.targetGroupId = listener.targetGroupId
    this.targetGroupName = listener.targetGroupName
    this.healthCheck = new CtyunLoadBalancerHealthCheck()
    this.healthCheck.copyHealthCheck(listener.healthCheck)
    this.certificate = new CtyunLoadBalancerCertificate()
    this.certificate.copyCertificate(listener.certificate)
    this.targets = listener.targets.collect {
      def target = new CtyunLoadBalancerTarget()
      target.instanceId = it.instanceId
      target.port = it.port
      target.weight = it.weight
      target.type = it.type
      target.targetId = it.targetId
      target.targetGroupID =it.targetGroupID
      target.healthCheckStatus = it.healthCheckStatus
      target.healthCheckStatusIpv6 = it.healthCheckStatusIpv6
      target
    }
    this.rules = listener.rules.collect {
      def rule = new CtyunLoadBalancerRule()
      rule.locationId = it.locationId
      rule.domain = it.domain
      rule.url = it.url
      rule.sessionExpireTime = it.sessionExpireTime
      rule.scheduler = it.scheduler
      rule.healthCheck = new CtyunLoadBalancerHealthCheck()
      rule.healthCheck.copyHealthCheck(it.healthCheck)
      rule.certificate = new CtyunLoadBalancerCertificate()
      rule.certificate.copyCertificate(it.certificate)
      rule.targets = it.targets.collect {
        def target = new CtyunLoadBalancerTarget()
        target.instanceId = it.instanceId
        target.port = it.port
        target.weight = it.weight
        target.type = it.type
        target.targetId = it.targetId
        target.targetGroupID =it.targetGroupID
        target.healthCheckStatus = it.healthCheckStatus
        target.healthCheckStatusIpv6 = it.healthCheckStatusIpv6
        target
      }
      rule.ruleTargetGroupId = it.ruleTargetGroupId
      rule.ruleTargetGroupName = it.ruleTargetGroupName
      rule
    }

  }
}
