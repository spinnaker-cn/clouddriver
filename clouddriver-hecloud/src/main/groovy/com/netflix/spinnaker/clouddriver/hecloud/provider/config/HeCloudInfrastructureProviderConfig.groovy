package com.netflix.spinnaker.clouddriver.hecloud.provider.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.CachingAgent
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository
import com.netflix.spinnaker.clouddriver.hecloud.provider.HeCloudInfrastructureProvider
import com.netflix.spinnaker.clouddriver.hecloud.provider.agent.HeCloudImageCachingAgent
import com.netflix.spinnaker.clouddriver.hecloud.provider.agent.HeCloudInstanceCachingAgent
import com.netflix.spinnaker.clouddriver.hecloud.provider.agent.HeCloudInstanceTypeCachingAgent
import com.netflix.spinnaker.clouddriver.hecloud.provider.agent.HeCloudKeyPairCachingAgent
import com.netflix.spinnaker.clouddriver.hecloud.provider.agent.HeCloudLoadBalancerInstanceStateCachingAgent
import com.netflix.spinnaker.clouddriver.hecloud.provider.agent.HeCloudNetworkCachingAgent
import com.netflix.spinnaker.clouddriver.hecloud.provider.agent.HeCloudServerGroupCachingAgent
import com.netflix.spinnaker.clouddriver.hecloud.provider.agent.HeCloudLoadBalancerCachingAgent
import com.netflix.spinnaker.clouddriver.hecloud.provider.agent.HeCloudSecurityGroupCachingAgent
import com.netflix.spinnaker.clouddriver.hecloud.provider.agent.HeCloudSubnetCachingAgent
import com.netflix.spinnaker.clouddriver.hecloud.security.HeCloudNamedAccountCredentials
import com.netflix.spinnaker.config.HeCloudConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Import

@Configuration
@Import(HeCloudConfiguration)
@EnableConfigurationProperties
class HeCloudInfrastructureProviderConfig {
  @Autowired
  Registry registry

  @Bean
  @DependsOn('heCloudNamedAccountCredentials')
  HeCloudInfrastructureProvider hecloudInfrastructureProvider(
    AccountCredentialsRepository accountCredentialsRepository,
    ObjectMapper objectMapper,
    Registry registry) {

    List<CachingAgent> agents = []
    def allAccounts = accountCredentialsRepository.all.findAll {
      it instanceof HeCloudNamedAccountCredentials
    } as Collection<HeCloudNamedAccountCredentials>

    // enable multiple accounts and multiple regions in each account
    allAccounts.each { HeCloudNamedAccountCredentials credential ->
      credential.regions.each { region ->
        agents << new HeCloudServerGroupCachingAgent(
          credential,
          objectMapper,
          registry,
          region.name)

        agents << new HeCloudInstanceTypeCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new HeCloudKeyPairCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new HeCloudImageCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new HeCloudInstanceCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new HeCloudLoadBalancerCachingAgent(
          credential,
          objectMapper,
          registry,
          region.name
        )

        agents << new HeCloudSecurityGroupCachingAgent(
          credential,
          objectMapper,
          registry,
          region.name
        )

        agents << new HeCloudNetworkCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new HeCloudSubnetCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new HeCloudLoadBalancerInstanceStateCachingAgent(
          credential,
          objectMapper,
          region.name
        )
      }
    }
    return new HeCloudInfrastructureProvider(agents)
  }
}
