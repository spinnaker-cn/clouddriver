package com.netflix.spinnaker.config;



import com.netflix.spinnaker.monitor.collector.CloudApiInvokeCollector;
//import io.micrometer.core.instrument.MeterRegistry;
import io.prometheus.client.Collector;
import io.prometheus.client.exporter.HTTPServer;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;

/**
 * @author chen_muyi
 * @date 2021/10/21 19:47
 */
@Configuration
@ComponentScan("com.netflix.spinnaker.monitor")
public class InitConfig {

  @Value("${prometheus.port:8000}")
  private Integer port;

  @Autowired
  List<Collector> collectors;

  @Bean
  public void startPrometheus (){
    collectors.forEach(Collector::register);
    try {
      new HTTPServer(port);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
