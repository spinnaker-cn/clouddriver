package com.netflix.spinnaker.clouddriver.ecloud.deploy.description;

import java.util.List;

/**
 * @author xu.dangling
 * @date 2024/4/12 @Description
 */
public abstract class ControlEcloudServerGroupInstancesDescription
    extends AbstractEcloudCredentialsDescription {

  private String serverGroupName;
  private List<String> instanceIds;
  private String region;

  public String getServerGroupName() {
    return serverGroupName;
  }

  public void setServerGroupName(String serverGroupName) {
    this.serverGroupName = serverGroupName;
  }

  public List<String> getInstanceIds() {
    return instanceIds;
  }

  public void setInstanceIds(List<String> instanceIds) {
    this.instanceIds = instanceIds;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }
}
