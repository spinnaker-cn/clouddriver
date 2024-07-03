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
public class EcloudLoadBalancerL7Policy {
  private String modifiedTime;

  private String description;

  private String l7PolicyDomainName;

  private Boolean isMultiAz;

  private String l7RuleValue;

  private String listenerId;

  private String compareType;

  private Boolean deleted;

  private String l7PolicyUrl;

  private String l7PolicyName;

  private String ruleType;

  private String poolId;

  private String createdTime;

  private String l7PolicyId;

  private Integer position;

  private Boolean adminStateUp;

  private String multiAzUuid;

  private String poolName;
}
