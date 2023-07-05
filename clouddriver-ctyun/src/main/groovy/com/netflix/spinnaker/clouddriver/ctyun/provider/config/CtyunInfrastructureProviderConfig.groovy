package com.netflix.spinnaker.clouddriver.ctyun.provider.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.CachingAgent
import com.netflix.spinnaker.clouddriver.ctyun.provider.agent.CtyunZoneCachingAgent
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository
import com.netflix.spinnaker.clouddriver.ctyun.provider.CtyunInfrastructureProvider
import com.netflix.spinnaker.clouddriver.ctyun.provider.agent.CtyunImageCachingAgent
import com.netflix.spinnaker.clouddriver.ctyun.provider.agent.CtyunInstanceCachingAgent
import com.netflix.spinnaker.clouddriver.ctyun.provider.agent.CtyunInstanceTypeCachingAgent
import com.netflix.spinnaker.clouddriver.ctyun.provider.agent.CtyunKeyPairCachingAgent
import com.netflix.spinnaker.clouddriver.ctyun.provider.agent.CtyunLoadBalancerInstanceStateCachingAgent
import com.netflix.spinnaker.clouddriver.ctyun.provider.agent.CtyunNetworkCachingAgent
import com.netflix.spinnaker.clouddriver.ctyun.provider.agent.CtyunServerGroupCachingAgent
import com.netflix.spinnaker.clouddriver.ctyun.provider.agent.CtyunLoadBalancerCachingAgent
import com.netflix.spinnaker.clouddriver.ctyun.provider.agent.CtyunSecurityGroupCachingAgent
import com.netflix.spinnaker.clouddriver.ctyun.provider.agent.CtyunSubnetCachingAgent
import com.netflix.spinnaker.clouddriver.ctyun.security.CtyunNamedAccountCredentials
import com.netflix.spinnaker.config.CtyunConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Import

@Configuration
@Import(CtyunConfiguration)
@EnableConfigurationProperties
class CtyunInfrastructureProviderConfig {
  @Autowired
  Registry registry

  @Bean
  @DependsOn('CtyunNamedAccountCredentials')
  CtyunInfrastructureProvider ctyunInfrastructureProvider(
    AccountCredentialsRepository accountCredentialsRepository,
    ObjectMapper objectMapper,
    Registry registry) {

    List<CachingAgent> agents = []
    def allAccounts = accountCredentialsRepository.all.findAll {
      it instanceof CtyunNamedAccountCredentials
    } as Collection<CtyunNamedAccountCredentials>

    // enable multiple accounts and multiple regions in each account
    allAccounts.each { CtyunNamedAccountCredentials credential ->
      credential.regions.each { region ->
        agents << new CtyunServerGroupCachingAgent(
          credential,
          objectMapper,
          registry,
          region.name)

        agents << new CtyunInstanceTypeCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new CtyunKeyPairCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new CtyunZoneCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new CtyunImageCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new CtyunInstanceCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new CtyunLoadBalancerCachingAgent(
          credential,
          objectMapper,
          registry,
          region.name
        )

        agents << new CtyunSecurityGroupCachingAgent(
          credential,
          objectMapper,
          registry,
          region.name
        )

        agents << new CtyunNetworkCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new CtyunSubnetCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new CtyunLoadBalancerInstanceStateCachingAgent(
          credential,
          objectMapper,
          region.name
        )
      }
    }
    return new CtyunInfrastructureProvider(agents)
  }
}
