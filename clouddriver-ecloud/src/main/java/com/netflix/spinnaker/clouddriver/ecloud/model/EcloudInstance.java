package com.netflix.spinnaker.clouddriver.ecloud.model;

import com.netflix.spinnaker.clouddriver.model.HealthState;
import com.netflix.spinnaker.clouddriver.model.Instance;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EcloudInstance implements Instance {

  private String name;
  private Long launchTime;
  private String zone;
  private String providerType;
  private String cloudProvider;
  private HealthState healthState;
  private List<Map<String, Object>> health;
  private String vpcId;
  private String subnetId;
  private List<String> privateIpAddresses;
  private List<String> publicIpAddresses;
  private String instanceType;
  private String imageId;
  private List<String> securityGroupIds;
  private List<EcloudTag> tags;
  private String serverGroupName;
  private String asgNodeId;
  private Map<String, String> lbMemberMap;
}
