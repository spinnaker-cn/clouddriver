package com.netflix.spinnaker.clouddriver.ecloud.provider.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.cats.agent.Agent;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.provider.EcloudSearchableProvider;
import com.netflix.spinnaker.clouddriver.ecloud.provider.agent.EcloudImageCachingAgent;
import com.netflix.spinnaker.clouddriver.ecloud.provider.agent.EcloudInstanceCachingAgent;
import com.netflix.spinnaker.clouddriver.ecloud.provider.agent.EcloudInstanceTypeCachingAgent;
import com.netflix.spinnaker.clouddriver.ecloud.provider.agent.EcloudKeyPairCachingAgent;
import com.netflix.spinnaker.clouddriver.ecloud.provider.agent.EcloudLoadbalancerCachingAgent;
import com.netflix.spinnaker.clouddriver.ecloud.provider.agent.EcloudLoadbalancerInstanceStateCachingAgent;
import com.netflix.spinnaker.clouddriver.ecloud.provider.agent.EcloudSecurityGroupCachingAgent;
import com.netflix.spinnaker.clouddriver.ecloud.provider.agent.EcloudServerGroupCachingAgent;
import com.netflix.spinnaker.clouddriver.ecloud.provider.agent.EcloudSubNetworkCachingAgent;
import com.netflix.spinnaker.clouddriver.ecloud.provider.agent.EcloudVpcCachingAgent;
import com.netflix.spinnaker.clouddriver.ecloud.provider.agent.EcloudZoneHelper;
import com.netflix.spinnaker.clouddriver.ecloud.security.EcloudCredentials;
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository;
import com.netflix.spinnaker.clouddriver.security.ProviderUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-15
 */
@Configuration
@EnableConfigurationProperties
public class EcloudProviderConfig {
  @Bean
  @DependsOn("synchronizeEcloudAccounts")
  EcloudSearchableProvider ecloudSearchableProvider(
      AccountCredentialsRepository accountCredentialsRepository,
      ObjectMapper objectMapper,
      EcloudProvider ecloudProvider,
      Registry registry) {

    Set<EcloudCredentials> allAccounts =
        ProviderUtils.buildThreadSafeSetOfAccounts(
            accountCredentialsRepository, EcloudCredentials.class);

    List<Agent> agents = new ArrayList<>();
    for (EcloudCredentials credentials : allAccounts) {
      if (credentials.getCloudProvider().equals(EcloudProvider.ID)) {
        for (EcloudCredentials.EcloudRegion region : credentials.getRegions()) {
          EcloudZoneHelper.loadZones(credentials, region.getName());

          agents.add(new EcloudInstanceCachingAgent(credentials, region.getName(), objectMapper));
          agents.add(
              new EcloudInstanceTypeCachingAgent(credentials, region.getName(), objectMapper));
          agents.add(new EcloudVpcCachingAgent(credentials, region.getName(), objectMapper));
          agents.add(new EcloudSubNetworkCachingAgent(credentials, region.getName(), objectMapper));
          agents.add(new EcloudImageCachingAgent(credentials, region.getName(), objectMapper));
          agents.add(new EcloudKeyPairCachingAgent(credentials, region.getName(), objectMapper));

          agents.add(
              new EcloudServerGroupCachingAgent(
                  credentials, region.getName(), registry, objectMapper));

          agents.add(
              new EcloudSecurityGroupCachingAgent(
                  ecloudProvider, credentials, objectMapper, registry, region.getName()));
          agents.add(
              new EcloudLoadbalancerCachingAgent(
                  ecloudProvider, region.getName(), objectMapper, registry, credentials));
          agents.add(
              new EcloudLoadbalancerInstanceStateCachingAgent(
                  credentials, objectMapper, region.getName()));
        }
      }
    }
    return new EcloudSearchableProvider(agents);
  }
}
