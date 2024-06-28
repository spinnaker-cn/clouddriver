package com.netflix.spinnaker.clouddriver.ecloud.security;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.security.AccountCredentials;
import java.util.List;

public final class EcloudCredentials implements AccountCredentials<AccountCredentials> {

  private String name;

  private String accessKey;

  private String secretKey;

  private List<EcloudRegion> regions;

  public void setName(String name) {
    this.name = name;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public List<EcloudRegion> getRegions() {
    return regions;
  }

  public void setRegions(List<EcloudRegion> regions) {
    this.regions = regions;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getEnvironment() {
    return name;
  }

  @Override
  public String getAccountType() {
    return name;
  }

  @Override
  public AccountCredentials getCredentials() {
    return null;
  }

  @Override
  public String getCloudProvider() {
    return EcloudProvider.ID;
  }

  @Override
  public List<String> getRequiredGroupMembership() {
    return null;
  }

  public static class EcloudRegion {
    public final String name;

    public EcloudRegion(String name) {
      if (name == null) {
        throw new IllegalArgumentException("name must be specified.");
      }
      this.name = name;
    }

    public String getName() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      EcloudRegion region = (EcloudRegion) o;

      return name.equals(region.name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }
  }
}
