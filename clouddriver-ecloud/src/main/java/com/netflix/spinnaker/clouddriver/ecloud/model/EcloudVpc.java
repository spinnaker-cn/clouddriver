package com.netflix.spinnaker.clouddriver.ecloud.model;

import com.netflix.spinnaker.clouddriver.model.Network;
import lombok.Getter;
import lombok.Setter;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-08
 */
@Getter
@Setter
public class EcloudVpc implements Network {
  private String cloudProvider;

  private String id;

  private String vpcId;

  private String name;

  private String vpcName;

  private String account;

  private String region;

  private String orderType;

  private String ecStatus;

  private String vpoolId;

  private String description;

  private String scale;

  private String userName;

  private String userId;

  private String vaz;

  private String special;

  private Boolean edge;

  private Boolean deleted;

  private String routerId;

  private String createdTime;

  private String vpcExtraSpecification;

  private Boolean adminStateUp;

  @Override
  public String getCloudProvider() {
    return this.cloudProvider;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getAccount() {
    return this.account;
  }

  @Override
  public String getRegion() {
    return this.region;
  }
}
