package com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudBasicResource;
import com.netflix.spinnaker.clouddriver.model.LoadBalancer;
import com.netflix.spinnaker.clouddriver.model.LoadBalancerServerGroup;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-08
 */
@Getter
@Setter
public class EcloudLoadBalancer implements LoadBalancer, EcloudBasicResource {
  private String application;

  private String loadBalancerId;

  private String subnetId;

  private String vpcId;

  private String vpcName;

  private String orderId;

  private String privateIp;

  private String description;

  private String nodeIp;

  private String vipPortId;

  private String subnetName;

  private Boolean isMultiAz;

  private Boolean isExclusive;

  private String ipId;

  private String provider;

  private String routerId;

  private String createTime;

  private String id;

  private Boolean adminStateUp;

  private String measureType;

  private String ecStatus;

  private Boolean visible;

  private String proposer;

  private String publicIp;

  private String userName;

  private Integer flavor;

  private Boolean deleted;

  private Integer ipVersion;

  private String name;

  private String loadBalancerName;

  private String opStatus;

  private String region;

  private String accountName;

  private String loadBalancerSpec;

  private List<EcloudLoadBalancerListener> listeners;

  private List<EcloudLoadBalancerPool> pools;

  private Set<LoadBalancerServerGroup> serverGroups = new HashSet<>();

  @Override
  public String getType() {
    return EcloudProvider.ID;
  }

  @Override
  public String getCloudProvider() {
    return EcloudProvider.ID;
  }

  @Override
  public String getAccount() {
    return this.accountName;
  }

  @Override
  public Set<LoadBalancerServerGroup> getServerGroups() {
    return this.serverGroups;
  }

  @Override
  public String getMonikerName() {
    return this.getName();
  }
}
