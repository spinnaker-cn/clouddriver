package com.netflix.spinnaker.clouddriver.ecloud.model;

import lombok.Data;

@Data
public class EcloudSecurityGroupRule {
  private String protocol;
  /*private String port;*/
  private Integer maxPortRange;
  private Integer minPortRange;
  private String cidrBlock;
  private String direction;
  private String remoteType;
  private String id;

  public EcloudSecurityGroupRule(String id) {
    this.id = id;
  }

  public EcloudSecurityGroupRule() {}
}
