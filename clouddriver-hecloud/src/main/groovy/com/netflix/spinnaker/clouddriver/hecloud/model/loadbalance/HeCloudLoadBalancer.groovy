package com.netflix.spinnaker.clouddriver.hecloud.model.loadbalance


import com.netflix.spinnaker.clouddriver.model.LoadBalancer
import com.netflix.spinnaker.clouddriver.model.LoadBalancerServerGroup
import com.netflix.spinnaker.clouddriver.names.NamerRegistry
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudProvider
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudBasicResource
import com.netflix.spinnaker.moniker.Moniker


class HeCloudLoadBalancer implements LoadBalancer, HeCloudBasicResource{
  final String cloudProvider = HeCloudProvider.ID
  final String type = HeCloudProvider.ID
  String application
  String accountName
  String region

  String id
  String name
  String loadBalancerId
  String loadBalancerName
  String subnetId
  String vpcId
  String createTime
  String loadBalancerVip
  List<HeCloudLoadBalancerListener> listeners
  List<HeCloudLoadBalancerPool> pools

  Set<LoadBalancerServerGroup> serverGroups = []

  @Override
  String getAccount() {
    accountName
  }

  @Override
  Moniker getMoniker() {
    return NamerRegistry.lookup()
      .withProvider(HeCloudProvider.ID)
      .withAccount(accountName)
      .withResource(HeCloudBasicResource)
      .deriveMoniker(this)
  }

  @Override
  String getMonikerName() {
    name
  }

  @Override
  boolean equals(Object o) {
    if (!(o instanceof HeCloudLoadBalancer)) {
      return false
    }
    HeCloudLoadBalancer other = (HeCloudLoadBalancer)o
    other.getAccount() == this.getAccount() && other.getName() == this.getName() && other.getType() == this.getType() && other.getId() == this.getId() && other.getRegion() == this.getRegion()
  }

  @Override
  int hashCode() {
    //getAccount().hashCode() + getId().hashCode() + getType().hashCode() + region.hashCode()
    getId().hashCode() + getType().hashCode()
  }
}
