package com.netflix.spinnaker.config


import com.netflix.spinnaker.clouddriver.hecloud.config.HeCloudConfigurationProperties
import com.netflix.spinnaker.clouddriver.hecloud.security.HeCloudCredentialsInitializer
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.*
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableConfigurationProperties
@EnableScheduling
@ConditionalOnProperty('hecloud.enabled')
@ComponentScan(["com.netflix.spinnaker.clouddriver.hecloud"])
@Import([ HeCloudCredentialsInitializer ])
class HeCloudConfiguration {
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  @Bean
  @ConfigurationProperties("hecloud")
  HeCloudConfigurationProperties hecloudConfigurationProperties() {
    new HeCloudConfigurationProperties()
  }
}
