package com.netflix.spinnaker.clouddriver.ecloud.deploy.description;

public class UpsertEcloudScheduledActionDescription extends AbstractEcloudCredentialsDescription {
  String serverGroupName;
  String region;
  String accountName;

  OperationType operationType;

  String scalingPolicyId;
  Integer maxSize;
  Integer minSize;
  Integer desiredCapacity;

  String startTime;
  String endTime;
  String recurrence;

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

  public OperationType getOperationType() {
    return operationType;
  }

  public void setOperationType(OperationType operationType) {
    this.operationType = operationType;
  }

  public String getScalingPolicyId() {
    return scalingPolicyId;
  }

  public void setScalingPolicyId(String scalingPolicyId) {
    this.scalingPolicyId = scalingPolicyId;
  }

  public Integer getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(Integer maxSize) {
    this.maxSize = maxSize;
  }

  public Integer getMinSize() {
    return minSize;
  }

  public void setMinSize(Integer minSize) {
    this.minSize = minSize;
  }

  public Integer getDesiredCapacity() {
    return desiredCapacity;
  }

  public void setDesiredCapacity(Integer desiredCapacity) {
    this.desiredCapacity = desiredCapacity;
  }

  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public String getRecurrence() {
    return recurrence;
  }

  public void setRecurrence(String recurrence) {
    this.recurrence = recurrence;
  }

  public enum OperationType {
    CREATE,
    MODIFY
  }
}
