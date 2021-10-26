package com.netflix.spinnaker.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author chen_muyi
 * @date 2021/10/21 19:47
 */
@Configuration
@ComponentScan("com.netflix.spinnaker.monitor")
public class InitConfig {
  /**
   * 监控
   *
   * @param applicationName
   * @return
   */
  @Bean
  MeterRegistryCustomizer<MeterRegistry> configurer(
      @Value("${spring.application.name}") String applicationName) {
    return (registry) -> registry.config().commonTags("application", applicationName);
  }
}
