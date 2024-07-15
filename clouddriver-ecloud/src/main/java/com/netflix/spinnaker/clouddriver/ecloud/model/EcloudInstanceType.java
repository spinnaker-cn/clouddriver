package com.netflix.spinnaker.clouddriver.ecloud.model;

import com.netflix.spinnaker.clouddriver.model.InstanceType;

/**
 * @author xu.dangling
 * @date 2024/4/8 @Description
 */
public class EcloudInstanceType implements InstanceType {

  private String name;
  private Integer cpu;
  private Integer mem;
  private String account;
  private String region;
  private String zone;

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getCpu() {
    return cpu;
  }

  public void setCpu(Integer cpu) {
    this.cpu = cpu;
  }

  public Integer getMem() {
    return mem;
  }

  public void setMem(Integer mem) {
    this.mem = mem;
  }

  public String getZone() {
    return zone;
  }

  public void setZone(String zone) {
    this.zone = zone;
  }

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }
}
