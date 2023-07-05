package com.netflix.spinnaker.config

import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository
import com.netflix.spinnaker.clouddriver.ctyun.config.CtyunConfigurationProperties
import com.netflix.spinnaker.clouddriver.ctyun.deploy.handlers.CtyunDeployHandler
import com.netflix.spinnaker.clouddriver.ctyun.security.CtyunCredentialsInitializer
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.*
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableConfigurationProperties
@EnableScheduling
@ConditionalOnProperty('ctyun.enabled')
@ComponentScan(["com.netflix.spinnaker.clouddriver.ctyun"])
@Import([ CtyunCredentialsInitializer ])
  class CtyunConfiguration {
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  @Bean
  @ConfigurationProperties("ctyun")
  CtyunConfigurationProperties ctyunConfigurationProperties() {
    new CtyunConfigurationProperties()
  }
}
