package com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer;

import com.netflix.spinnaker.clouddriver.model.HealthState;
import lombok.Getter;
import lombok.Setter;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-18
 */
@Getter
@Setter
public class EcloudLoadBalancerHealthCheck {
  private String loadbalancerId;

  private String poolId;

  private String listenerId;

  private String instanceId;

  private String memberId;

  private Integer port;

  private HealthState healthStatus;

  private Integer healthDelay;

  private String healthExpectedCode;

  private Integer healthMaxRetries;

  private String healthHttpMethod;

  private String healthId;

  private String healthType;

  private String healthUrlPath;

  private Integer healthTimeout;
}
