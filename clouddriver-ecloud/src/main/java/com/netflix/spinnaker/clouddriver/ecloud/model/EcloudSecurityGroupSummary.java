package com.netflix.spinnaker.clouddriver.ecloud.model;

import com.netflix.spinnaker.clouddriver.model.SecurityGroupSummary;
import lombok.Data;

@Data
public class EcloudSecurityGroupSummary implements SecurityGroupSummary {
  private String name;
  private String id;

  public EcloudSecurityGroupSummary(String name, String id) {
    this.name = name;
    this.id = id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getId() {
    return id;
  }
}
