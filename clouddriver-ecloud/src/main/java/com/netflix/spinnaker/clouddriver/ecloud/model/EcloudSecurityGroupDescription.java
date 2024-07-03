package com.netflix.spinnaker.clouddriver.ecloud.model;

import java.util.List;
import lombok.Data;

@Data
public class EcloudSecurityGroupDescription {
  String securityGroupId;
  String securityGroupName;
  String securityGroupDesc;
  List<EcloudSecurityGroupRule> inRules;
  List<EcloudSecurityGroupRule> outRules;
  long lastReadTime;

  public EcloudSecurityGroupDescription() {}

  public EcloudSecurityGroupDescription(String id, String name, long currentTime) {
    this.securityGroupId = id;
    this.securityGroupName = name;
    this.lastReadTime = currentTime;
  }
}
