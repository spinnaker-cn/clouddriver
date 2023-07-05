package com.netflix.spinnaker.clouddriver.ctyun.security

import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository
import com.netflix.spinnaker.clouddriver.security.CredentialsInitializerSynchronizable
import com.netflix.spinnaker.clouddriver.ctyun.config.CtyunConfigurationProperties
import groovy.util.logging.Slf4j
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Slf4j
@Configuration
class CtyunCredentialsInitializer{
  @Bean
  List<CtyunNamedAccountCredentials> CtyunNamedAccountCredentials(
    CtyunConfigurationProperties ctyunConfigurationProperties,
    AccountCredentialsRepository accountCredentialsRepository,
    String clouddriverUserAgentApplicationName
  ) {
    def CtyunAccounts = []
    ctyunConfigurationProperties.accounts.each {
      CtyunConfigurationProperties.ManagedAccount managedAccount ->
      try {
        def CtyunAccount = new CtyunNamedAccountCredentials(
          managedAccount.name,
          managedAccount.environment ?: managedAccount.name,
          managedAccount.accountType ?: managedAccount.name,
          managedAccount.accessKey,
          managedAccount.securityKey,
          managedAccount.regions,
          clouddriverUserAgentApplicationName
        )
        CtyunAccounts << (accountCredentialsRepository.save(managedAccount.name, CtyunAccount)
        as CtyunNamedAccountCredentials)
      } catch (e) {
        log.error("Could not load account ${managedAccount.name} for Ctyun.", e)
      }
    }
  }
}
