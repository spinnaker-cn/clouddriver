package com.netflix.spinnaker.clouddriver.hecloud.model.loadbalance

import groovy.transform.Canonical


@Canonical
class HeCloudLoadBalancerListener {
  String listenerId
  String listenerName
  String protocol
  Integer port

  //target, tcp/udp 4 layer
  String poolId
  HeCloudLoadBalancerHealthCheck healthCheck
  List<HeCloudLoadBalancerTarget> targets

  //rule, http/https 7 layer
  HeCloudLoadBalancerCertificate certificate
  List<HeCloudLoadBalancerRule> rules

  void copyListener(HeCloudLoadBalancerListener listener) {
    if (listener == null) {
      return
    }
    this.listenerId = listener.listenerId
    this.listenerName = listener.listenerName
    this.protocol = listener.protocol
    this.port = listener.port
    this.poolId = listener.poolId
    this.healthCheck = new HeCloudLoadBalancerHealthCheck()
    this.healthCheck.copyHealthCheck(listener.healthCheck)
    this.certificate = new HeCloudLoadBalancerCertificate()
    this.certificate.copyCertificate(listener.certificate)
    this.targets = listener.targets.collect {
      def target = new HeCloudLoadBalancerTarget()
      target.instanceId = it.instanceId
      target.port = it.port
      target.weight = it.weight
      target
    }
    this.rules = listener.rules.collect {
      def rule = new HeCloudLoadBalancerRule()
      rule.policyId = it.policyId
      rule.poolId = it.poolId
      rule.domain = it.domain
      rule.url = it.url
      rule.healthCheck = new HeCloudLoadBalancerHealthCheck()
      rule.healthCheck.copyHealthCheck(it.healthCheck)
      rule.targets = it.targets.collect {
        def target = new HeCloudLoadBalancerTarget()
        target.instanceId = it.instanceId
        target.port = it.port
        target.weight = it.weight
        target
      }
      rule
    }
  }

}
