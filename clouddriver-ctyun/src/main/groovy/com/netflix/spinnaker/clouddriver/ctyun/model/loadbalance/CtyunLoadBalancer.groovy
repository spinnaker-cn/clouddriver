package com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance


import com.netflix.spinnaker.clouddriver.model.LoadBalancer
import com.netflix.spinnaker.clouddriver.model.LoadBalancerServerGroup
import com.netflix.spinnaker.clouddriver.names.NamerRegistry
import com.netflix.spinnaker.clouddriver.ctyun.CtyunCloudProvider
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunBasicResource
import com.netflix.spinnaker.moniker.Moniker


class CtyunLoadBalancer implements LoadBalancer, CtyunBasicResource{
  final String cloudProvider = CtyunCloudProvider.ID
  final String type = CtyunCloudProvider.ID
  String application
  String accountName
  String region

  String id
  String name
  String loadBalancerId
  String loadBalancerName
  String loadBalancerType     //OPEN:公网, INTERNAL:内网
  Integer forwardType         //1:应用型,0:传统型
  String vpcId
  String subnetId
  Integer projectId
  String createTime
  List<String> loadBalacnerVips
  List<String> securityGroups
  List<CtyunLoadBalancerListener> listeners

  Set<LoadBalancerServerGroup> serverGroups = []

  @Override
  String getAccount() {
    accountName
  }

  @Override
  Moniker getMoniker() {
    return NamerRegistry.lookup()
      .withProvider(CtyunCloudProvider.ID)
      .withAccount(accountName)
      .withResource(CtyunBasicResource)
      .deriveMoniker(this)
  }

  @Override
  String getMonikerName() {
    name
  }

  @Override
  boolean equals(Object o) {
    if (!(o instanceof CtyunLoadBalancer)) {
      return false
    }
    CtyunLoadBalancer other = (CtyunLoadBalancer)o
    other.getAccount() == this.getAccount() && other.getName() == this.getName() && other.getType() == this.getType() && other.getId() == this.getId() && other.getRegion() == this.getRegion()
  }

  @Override
  int hashCode() {
    //getAccount().hashCode() + getId().hashCode() + getType().hashCode() + region.hashCode()
    getId().hashCode() + getType().hashCode()
  }
}
