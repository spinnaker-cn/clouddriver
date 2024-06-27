package com.netflix.spinnaker.clouddriver.ecloud.deploy.description;

import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerListener;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerPool;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-11
 */
@Getter
@Setter
public class UpsertEcloudLoadBalancerDescription extends AbstractEcloudCredentialsDescription {
  private String application;

  private String accountName;

  private String region;

  private String loadBalancerId;

  private String loadBalancerName;

  private String loadBalancerType;

  private String forwardType;

  private String vpcId;

  private String subnetId;

  private Integer projectId;

  // listener, rule, target
  private List<EcloudLoadBalancerListener> listeners; // listeners

  private List<EcloudLoadBalancerPool> pools;

  private Integer duration;

  private Integer flavor;

  private String chargePeriod;

  private String ipId;

  private String ipAddress;

  private Boolean autoRenew;

  private String returnUrl;

  private String productType;
}
