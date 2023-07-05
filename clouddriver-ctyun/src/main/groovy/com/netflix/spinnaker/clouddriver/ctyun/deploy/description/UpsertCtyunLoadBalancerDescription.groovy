package com.netflix.spinnaker.clouddriver.ctyun.deploy.description

import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancerListener
import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancerRule
import groovy.transform.AutoClone
import groovy.transform.Canonical

@AutoClone
@Canonical
class UpsertCtyunLoadBalancerDescription extends AbstractCtyunCredentialsDescription {
  String application
  String accountName
  String region

  String loadBalancerId
  String loadBalancerName
  String loadBalancerType     //OPEN:公网, INTERNAL:内网
  Integer forwardType     //1:应用型,0:传统型
  String vpcId
  String subnetId
  Integer projectId
  List<String> securityGroups

  //listener, rule, target
  List<CtyunLoadBalancerListener> listener  //listeners
  String eipId
}
