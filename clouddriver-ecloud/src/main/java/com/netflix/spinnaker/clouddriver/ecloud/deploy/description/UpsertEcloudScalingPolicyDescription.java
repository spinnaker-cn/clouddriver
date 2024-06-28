package com.netflix.spinnaker.clouddriver.ecloud.deploy.description;

public class UpsertEcloudScalingPolicyDescription extends AbstractEcloudCredentialsDescription {
  private String serverGroupName;
  private String region;
  private String accountName;
  private String policyName;
  private String adjustmentType;
  private Integer adjustmentValue;
  private Integer minAdjustmentValue;
  private Integer cooldown;

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

  public String getPolicyName() {
    return policyName;
  }

  public void setPolicyName(String policyName) {
    this.policyName = policyName;
  }

  public String getAdjustmentType() {
    return adjustmentType;
  }

  public void setAdjustmentType(String adjustmentType) {
    this.adjustmentType = adjustmentType;
  }

  public Integer getAdjustmentValue() {
    return adjustmentValue;
  }

  public void setAdjustmentValue(Integer adjustmentValue) {
    this.adjustmentValue = adjustmentValue;
  }

  public Integer getMinAdjustmentValue() {
    return minAdjustmentValue;
  }

  public void setMinAdjustmentValue(Integer minAdjustmentValue) {
    this.minAdjustmentValue = minAdjustmentValue;
  }

  public Integer getCooldown() {
    return cooldown;
  }

  public void setCooldown(Integer cooldown) {
    this.cooldown = cooldown;
  }
}
