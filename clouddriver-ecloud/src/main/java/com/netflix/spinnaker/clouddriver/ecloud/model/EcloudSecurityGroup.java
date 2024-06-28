package com.netflix.spinnaker.clouddriver.ecloud.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.model.SecurityGroup;
import com.netflix.spinnaker.clouddriver.model.SecurityGroupSummary;
import com.netflix.spinnaker.clouddriver.model.securitygroups.Rule;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class EcloudSecurityGroup implements SecurityGroup {
  final String type = EcloudProvider.ID;
  final String cloudProvider = EcloudProvider.ID;

  private String id;
  private String name;
  private String description;
  private String application;
  private String accountName;
  private String region;
  private Set<Rule> inboundRules;
  private Set<Rule> outboundRules;
  private List<EcloudSecurityGroupRule> inRules;
  private List<EcloudSecurityGroupRule> outRules;

  public String getType() {
    return EcloudProvider.ID;
  }

  public String getCloudProvider() {
    return EcloudProvider.ID;
  }

  @Override
  public SecurityGroupSummary getSummary() {
    return new EcloudSecurityGroupSummary(name, id);
  }
}
