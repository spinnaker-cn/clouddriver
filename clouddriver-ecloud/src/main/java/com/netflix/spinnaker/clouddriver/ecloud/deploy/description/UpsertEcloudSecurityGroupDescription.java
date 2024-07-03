package com.netflix.spinnaker.clouddriver.ecloud.deploy.description;

import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudSecurityGroupRule;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpsertEcloudSecurityGroupDescription extends AbstractEcloudCredentialsDescription {

  private String region;
  private String securityGroupId;
  private String securityGroupName;
  private String securityGroupDesc;
  /** * 安全组是否为有状态：true-有状态（默认）；false-无状态 */
  private Boolean stateful;
  /** * 安全组类型：VM-云主机（默认）；IRONIC-裸金属；NAS-NAS网卡；EW-东西向网卡 */
  private String type;

  private List<EcloudSecurityGroupRule> inRules;
  private List<EcloudSecurityGroupRule> outRules;
}
