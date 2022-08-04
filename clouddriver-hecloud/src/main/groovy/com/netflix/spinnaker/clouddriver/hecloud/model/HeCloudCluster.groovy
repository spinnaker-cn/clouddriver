package com.netflix.spinnaker.clouddriver.hecloud.model


import com.netflix.spinnaker.clouddriver.model.Cluster
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudProvider
import com.netflix.spinnaker.clouddriver.hecloud.model.loadbalance.HeCloudLoadBalancer
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode

@CompileStatic
@EqualsAndHashCode(includes = ["name", "accountName"])
class HeCloudCluster implements Cluster {
  final String type = HeCloudProvider.ID
  String name
  String accountName
  Set<HeCloudServerGroup> serverGroups = Collections.synchronizedSet(new HashSet<HeCloudServerGroup>())
  Set<HeCloudLoadBalancer> loadBalancers = Collections.synchronizedSet(new HashSet<HeCloudLoadBalancer>())
}
