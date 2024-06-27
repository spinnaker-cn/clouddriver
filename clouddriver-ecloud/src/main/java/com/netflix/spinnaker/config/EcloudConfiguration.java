package com.netflix.spinnaker.config;

import com.netflix.spinnaker.clouddriver.ecloud.config.EcloudConfigurationProperties;
import com.netflix.spinnaker.clouddriver.ecloud.security.EcloudCredentialsInitializer;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-15
 */
@Configuration
@EnableConfigurationProperties
@EnableScheduling
@ConditionalOnProperty("ecloud.enabled")
@ComponentScan({"com.netflix.spinnaker.clouddriver.ecloud"})
@Import(EcloudCredentialsInitializer.class)
class EcloudConfiguration {

  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  @Bean
  @ConfigurationProperties("ecloud")
  EcloudConfigurationProperties ecloudConfigurationProperties() {
    return new EcloudConfigurationProperties();
  }
}
