package com.netflix.spinnaker.clouddriver.ecloud.config;

import java.util.List;
import lombok.Data;

@Data
public class EcloudConfigurationProperties {

  private List<Account> accounts;

  @Data
  public static class Account {

    private String name;

    private String accessKey;

    private String secretKey;

    private List<String> regions;
  }
}
