package com.netflix.spinnaker.clouddriver.ctyun.config

import groovy.transform.ToString

@ToString(includeNames = true)
class CtyunConfigurationProperties {
  @ToString(includeNames = true)
  static class ManagedAccount {
    String name
    String environment
    String accountType
    String project
    String accessKey
    String securityKey
    List<String> regions
  /*  String userId
    String accountId*/
  }

  List<ManagedAccount> accounts = []
}
