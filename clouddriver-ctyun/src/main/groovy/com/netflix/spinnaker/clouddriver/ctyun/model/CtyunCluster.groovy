package com.netflix.spinnaker.clouddriver.ctyun.model


import com.netflix.spinnaker.clouddriver.model.Cluster
import com.netflix.spinnaker.clouddriver.ctyun.CtyunCloudProvider
import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancer
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode

@CompileStatic
@EqualsAndHashCode(includes = ["name", "accountName"])
class CtyunCluster implements Cluster {
  final String type = CtyunCloudProvider.ID
  String name
  String accountName
  Set<CtyunServerGroup> serverGroups = Collections.synchronizedSet(new HashSet<CtyunServerGroup>())
  Set<CtyunLoadBalancer> loadBalancers = Collections.synchronizedSet(new HashSet<CtyunLoadBalancer>())
}
