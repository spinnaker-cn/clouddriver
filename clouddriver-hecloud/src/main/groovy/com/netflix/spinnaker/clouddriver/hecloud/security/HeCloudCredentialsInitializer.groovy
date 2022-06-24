package com.netflix.spinnaker.clouddriver.hecloud.security

import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository
import com.netflix.spinnaker.clouddriver.hecloud.config.HeCloudConfigurationProperties
import groovy.util.logging.Slf4j
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Slf4j
@Configuration
class HeCloudCredentialsInitializer {
  @Bean
  List<HeCloudNamedAccountCredentials> heCloudNamedAccountCredentials(
    HeCloudConfigurationProperties hecloudConfigurationProperties,
    AccountCredentialsRepository accountCredentialsRepository,
    String clouddriverUserAgentApplicationName
  ) {
    def hecloudAccounts = []
    hecloudConfigurationProperties.accounts.each {
      HeCloudConfigurationProperties.ManagedAccount managedAccount ->
      try {
        def hecloudAccount = new HeCloudNamedAccountCredentials(
          managedAccount.name,
          managedAccount.environment ?: managedAccount.name,
          managedAccount.accountType ?: managedAccount.name,
          managedAccount.accessKeyId,
          managedAccount.accessSecretKey,
          managedAccount.regions,
          clouddriverUserAgentApplicationName
        )
        hecloudAccounts << (accountCredentialsRepository.save(managedAccount.name, hecloudAccount)
        as HeCloudNamedAccountCredentials)
      } catch (e) {
        log.error("Could not load account ${managedAccount.name} for HeCloud.", e)
      }
    }
  }
}
