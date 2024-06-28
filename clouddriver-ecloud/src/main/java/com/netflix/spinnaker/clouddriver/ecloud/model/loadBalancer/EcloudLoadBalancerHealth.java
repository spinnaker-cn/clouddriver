package com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer;

import lombok.Getter;
import lombok.Setter;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-10
 */
@Getter
@Setter
public class EcloudLoadBalancerHealth {
  private Integer healthDelay;

  private String healthExpectedCode;

  private Integer healthMaxRetries;

  private String healthHttpMethod;

  private String healthId;

  private String healthType;

  private String healthUrlPath;

  private Integer healthTimeout;
}
