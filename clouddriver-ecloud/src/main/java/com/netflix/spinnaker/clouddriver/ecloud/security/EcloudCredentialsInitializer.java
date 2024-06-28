package com.netflix.spinnaker.clouddriver.ecloud.security;

import com.netflix.spinnaker.clouddriver.ecloud.config.EcloudConfigurationProperties;
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository;
import com.netflix.spinnaker.clouddriver.security.CredentialsInitializerSynchronizable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class EcloudCredentialsInitializer implements CredentialsInitializerSynchronizable {

  @Bean
  public List synchronizeEcloudAccounts(
      EcloudConfigurationProperties cloudConfigurationProperties,
      AccountCredentialsRepository accountCredentialsRepository) {
    List<EcloudCredentials> ecloudCredentialsList = new ArrayList<>();
    cloudConfigurationProperties.getAccounts().stream()
        .forEach(
            account -> {
              EcloudCredentials eCloudCredentials = new EcloudCredentials();
              eCloudCredentials.setName(account.getName());
              eCloudCredentials.setAccessKey(account.getAccessKey());
              eCloudCredentials.setSecretKey(account.getSecretKey());
              eCloudCredentials.setRegions(
                  account.getRegions().stream()
                      .map(one -> new EcloudCredentials.EcloudRegion(one))
                      .collect(Collectors.toList()));
              accountCredentialsRepository.save(account.getName(), eCloudCredentials);
              ecloudCredentialsList.add(eCloudCredentials);
            });
    return ecloudCredentialsList;
  }
}
