package com.netflix.spinnaker.clouddriver.ctyun.provider.agent

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.agent.AccountAware
import com.netflix.spinnaker.cats.agent.CachingAgent
import com.netflix.spinnaker.clouddriver.ctyun.provider.CtyunInfrastructureProvider
import com.netflix.spinnaker.clouddriver.ctyun.security.CtyunNamedAccountCredentials

abstract class AbstractCtyunCachingAgent implements CachingAgent, AccountAware {
  final ObjectMapper objectMapper
  final String region
  final String accountName
  final CtyunNamedAccountCredentials credentials
  final String providerName = CtyunInfrastructureProvider.name

  final TypeReference<Map<String, Object>> ATTRIBUTES = new TypeReference<Map<String, Object>>() {}

  AbstractCtyunCachingAgent(CtyunNamedAccountCredentials credentials,
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
