package com.netflix.spinnaker.clouddriver.hecloud.model.loadbalance

import groovy.transform.Canonical


@Canonical
class HeCloudLoadBalancerPool {
  String poolId
  String poolName

  void copyPool(HeCloudLoadBalancerPool pool) {
    if (pool == null) {
      return
    }
    this.poolId = pool.poolId
    this.poolName = pool.poolName
  }
}
