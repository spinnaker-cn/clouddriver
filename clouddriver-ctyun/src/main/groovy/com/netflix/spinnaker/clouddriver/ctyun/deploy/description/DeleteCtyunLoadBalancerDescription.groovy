package com.netflix.spinnaker.clouddriver.ctyun.deploy.description

import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancerListener
import groovy.transform.AutoClone
import groovy.transform.Canonical

@AutoClone
@Canonical
class DeleteCtyunLoadBalancerDescription extends AbstractCtyunCredentialsDescription {
  String application
  String accountName
  String region
  String loadBalancerId
  List<CtyunLoadBalancerListener> listener  //listeners
}
