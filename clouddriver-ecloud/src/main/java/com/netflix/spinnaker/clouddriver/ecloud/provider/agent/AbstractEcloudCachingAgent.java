package com.netflix.spinnaker.clouddriver.ecloud.provider.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.agent.CachingAgent;
import com.netflix.spinnaker.clouddriver.ecloud.provider.EcloudSearchableProvider;
import com.netflix.spinnaker.clouddriver.ecloud.security.EcloudCredentials;

/**
 * @author xu.dangling
 * @date 2024/4/3 @Description
 */
public abstract class AbstractEcloudCachingAgent implements CachingAgent {

  protected EcloudCredentials account;
  protected String region;
  protected ObjectMapper objectMapper;

  public AbstractEcloudCachingAgent(
      EcloudCredentials account, String region, ObjectMapper objectMapper) {
    this.account = account;
    this.region = region;
    this.objectMapper = objectMapper;
  }

  @Override
  public String getAgentType() {
    return account.getName() + "/" + region + "/" + this.getClass().getSimpleName();
  }

  @Override
  public String getProviderName() {
    return EcloudSearchableProvider.class.getName();
  }
}
