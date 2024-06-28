package com.netflix.spinnaker.clouddriver.ecloud.model;

import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancer;
import com.netflix.spinnaker.clouddriver.model.Cluster;
import com.netflix.spinnaker.clouddriver.model.LoadBalancer;
import java.util.Set;

/**
 * @author xu.dangling
 * @date 2024/4/9 @Description
 */
public class EcloudCluster implements Cluster {

  private String name;
  private String type;
  private String accountName;
  private Set<EcloudServerGroup> serverGroups;
  private Set<EcloudLoadBalancer> loadBalancers;

  public void setName(String name) {
    this.name = name;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public void setServerGroups(Set<EcloudServerGroup> serverGroups) {
    this.serverGroups = serverGroups;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String getAccountName() {
    return accountName;
  }

  @Override
  public Set<EcloudServerGroup> getServerGroups() {
    return serverGroups;
  }

  @Override
  public Set<? extends LoadBalancer> getLoadBalancers() {
    return loadBalancers;
  }

  public void setLoadBalancers(Set<EcloudLoadBalancer> loadBalancers) {
    this.loadBalancers = loadBalancers;
  }
}
