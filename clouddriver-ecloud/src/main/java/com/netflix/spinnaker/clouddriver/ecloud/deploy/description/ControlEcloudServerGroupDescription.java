package com.netflix.spinnaker.clouddriver.ecloud.deploy.description;

/**
 * @author xu.dangling
 * @date 2024/4/11 @Description
 */
public abstract class ControlEcloudServerGroupDescription
    extends AbstractEcloudCredentialsDescription {

  private String serverGroupName;

  private String region;

  public String getServerGroupName() {
    return serverGroupName;
  }

  public void setServerGroupName(String serverGroupName) {
    this.serverGroupName = serverGroupName;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }
}
