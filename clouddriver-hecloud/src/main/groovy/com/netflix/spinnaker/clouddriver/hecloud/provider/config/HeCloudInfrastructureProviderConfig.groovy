package com.netflix.spinnaker.clouddriver.hecloud.provider.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.CachingAgent
import com.netflix.spinnaker.clouddriver.hecloud.provider.HeCloudInfrastructureProvider
import com.netflix.spinnaker.clouddriver.hecloud.provider.agent.*
import com.netflix.spinnaker.clouddriver.hecloud.security.HeCloudNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository
import com.netflix.spinnaker.config.HeCloudConfiguration
import com.netflix.spinnaker.kork.jedis.RedisClientDelegate
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
  @Autowired
  RedisClientDelegate redisClientDelegate

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
      agents << new HeCloudRedisCheckCachingAgent(
        credential,
        redisClientDelegate
      )
    }

    return new HeCloudInfrastructureProvider(agents)
  }
}
