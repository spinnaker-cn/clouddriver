package com.netflix.spinnaker.clouddriver.ecloud.deploy.description;

import com.netflix.spinnaker.clouddriver.ecloud.security.EcloudCredentials;
import com.netflix.spinnaker.clouddriver.security.resources.CredentialsNameable;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-11
 */
public abstract class AbstractEcloudCredentialsDescription implements CredentialsNameable {

  private EcloudCredentials credentials;

  @Override
  public EcloudCredentials getCredentials() {
    return credentials;
  }

  @Override
  public String getAccount() {
    return credentials.getName();
  }

  public void setCredentials(EcloudCredentials credentials) {
    this.credentials = credentials;
  }
}
