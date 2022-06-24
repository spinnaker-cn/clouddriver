package com.netflix.spinnaker.clouddriver.hecloud.config

import groovy.transform.ToString

@ToString(includeNames = true)
class HeCloudConfigurationProperties {
  @ToString(includeNames = true)
  static class ManagedAccount {
    @ToString(includeNames = true)
    static class Region {
      String name
      List<String> availabilityZones
    }

    String name
    String environment
    String accountType
    String accessKeyId
    String accessSecretKey
    List<Region> regions
  }

  List<ManagedAccount> accounts = []
}
