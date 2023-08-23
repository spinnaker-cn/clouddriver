package com.netflix.spinnaker.clouddriver.ctyun.security

class CtyunCredentials {
  final String accessKey
  final String securityKey
  /*final String accountId
  final String userId*/

 /* CtyunCredentials(String accessKey, String securityKey,String accountId,String userId)
  {
    this.accessKey = accessKey
    this.securityKey = securityKey
    this.accountId = accountId
    this.userId = userId
  }*/
  CtyunCredentials(String accessKey, String securityKey)
  {
    this.accessKey = accessKey
    this.securityKey = securityKey
  }
}
