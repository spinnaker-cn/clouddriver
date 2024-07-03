package com.netflix.spinnaker.clouddriver.ecloud.model;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.model.Subnet;
import lombok.Data;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-09
 */
@Data
public class EcloudSubnet implements Subnet {

  private String id;

  private String account;

  private String type;

  private String region;

  private String cidr;

  private String cidrBlock;

  private String createdTime;

  private Boolean deleted;

  private Boolean edge;

  private String gatewayIp;

  private String ipVersion;

  private String name;

  private String networkId;

  private String networkType;

  private String vPoolId;

  private String provider;

  private String vpcId;

  private String zone;

  private String zoneName;

  @Override
  public String getType() {
    return EcloudProvider.ID;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public String getPurpose() {
    return null;
  }
}
