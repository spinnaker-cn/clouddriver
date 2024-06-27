package com.netflix.spinnaker.clouddriver.ecloud.deploy.description;

public class ResizeEcloudServerGroupDescription extends AbstractEcloudCredentialsDescription {

  Capacity capacity;
  String serverGroupName;
  String region;
  String accountName;

  public Capacity getCapacity() {
    return capacity;
  }

  public void setCapacity(Capacity capacity) {
    this.capacity = capacity;
  }

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

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public static class Capacity {
    Integer min;
    Integer max;
    Integer desired;

    public Integer getMin() {
      return min;
    }

    public void setMin(Integer min) {
      this.min = min;
    }

    public Integer getMax() {
      return max;
    }

    public void setMax(Integer max) {
      this.max = max;
    }

    public Integer getDesired() {
      return desired;
    }

    public void setDesired(Integer desired) {
      this.desired = desired;
    }
  }
}
