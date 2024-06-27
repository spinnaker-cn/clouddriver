package com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer;

import lombok.Getter;
import lombok.Setter;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-22
 */
@Getter
@Setter
public class EcloudLoadBalancerRule {
  private String l7PolicyId;

  private String domain;

  private String url;

  private String listenerId;
}
