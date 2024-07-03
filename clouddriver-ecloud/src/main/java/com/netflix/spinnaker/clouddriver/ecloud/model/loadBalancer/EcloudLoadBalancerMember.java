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
public class EcloudLoadBalancerMember {
  private String subnetId;

  private String vmName;

  private Integer isDelete;

  private String proposer;

  private String ip;

  private String description;

  private Integer weight;

  private Integer type;

  private Boolean isMultiAz;

  private String healthStatus;

  private Integer port;

  private String poolId;

  private String createdTime;

  private String id;

  private String vmHostId;

  private String region;

  private String multiAzUuid;

  private String status;
}
