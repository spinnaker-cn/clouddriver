package com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EcloudLoadBalancerMemberHealth {

  private String healthStatus;

  private String poolId;

  private String id;
}
