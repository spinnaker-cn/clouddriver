package com.netflix.spinnaker.clouddriver.hecloud.provider.agent

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.agent.AccountAware
import com.netflix.spinnaker.cats.agent.CachingAgent
import com.netflix.spinnaker.clouddriver.hecloud.provider.HeCloudInfrastructureProvider
import com.netflix.spinnaker.clouddriver.hecloud.security.HeCloudNamedAccountCredentials

abstract class AbstractHeCloudCachingAgent implements CachingAgent, AccountAware {
  final ObjectMapper objectMapper
  final String region
  final String accountName
  final HeCloudNamedAccountCredentials credentials
  final String providerName = HeCloudInfrastructureProvider.name

  final TypeReference<Map<String, Object>> ATTRIBUTES = new TypeReference<Map<String, Object>>() {}

  AbstractHeCloudCachingAgent(HeCloudNamedAccountCredentials credentials,
                              ObjectMapper objectMapper,
                              String region) {
    this.credentials = credentials
    this.objectMapper = objectMapper
    this.region = region
    this.accountName = credentials.name
  }

  @Override
  String getAgentType() {
    return "$accountName/$region/${this.class.simpleName}"
  }
}
