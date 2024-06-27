package com.netflix.spinnaker.clouddriver.ecloud.model;

import com.netflix.spinnaker.clouddriver.model.KeyPair;

/**
 * @author xu.dangling
 * @date 2024/5/16 @Description
 */
public class EcloudKeyPair implements KeyPair {

  private String account;
  private String region;
  private String keyId;
  private String keyName;
  private String keyFingerprint;

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

  public String getKeyId() {
    return keyId;
  }

  public void setKeyId(String keyId) {
    this.keyId = keyId;
  }

  public void setKeyName(String keyName) {
    this.keyName = keyName;
  }

  public void setKeyFingerprint(String keyFingerprint) {
    this.keyFingerprint = keyFingerprint;
  }

  @Override
  public String getKeyName() {
    return keyName;
  }

  @Override
  public String getKeyFingerprint() {
    return keyFingerprint;
  }
}
