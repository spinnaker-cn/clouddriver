package com.netflix.spinnaker.clouddriver.hecloud.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.clouddriver.model.HealthState
import com.netflix.spinnaker.clouddriver.model.Instance
import com.netflix.spinnaker.clouddriver.names.NamerRegistry
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudProvider
import com.netflix.spinnaker.moniker.Moniker
import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = "name")
class HeCloudInstance implements Instance, HeCloudBasicResource {

  final String cloudProvider = HeCloudProvider.ID
  final String providerType = HeCloudProvider.ID
  String instanceName
  String account
  String name //unique instance id
  Long launchTime
  String launchedTime
  String zone
  HeCloudInstanceHealth instanceHealth
  List<HeCloudTargetHealth> targetHealths = []
  String vpcId
  String subnetId
  List<String> privateIpAddresses = []
  List<String> publicIpAddresses = []
  String instanceType
  String imageId
  List<String> securityGroupIds = []
  List<Map<String, String>> tags = []

  String serverGroupName

  @Override
  String getHumanReadableName() {
    return instanceName
  }

  @Override
  @JsonIgnore
  String getMonikerName() {
    return serverGroupName
  }

  List<Map<String, Object>> getHealth() {
    ObjectMapper objectMapper = new ObjectMapper()
    def healths = []
    if (instanceHealth) {
      healths << objectMapper.convertValue(instanceHealth, new TypeReference<Map<String, Object>>() {})
    }
    if (targetHealths) {
      for (targetHealth in targetHealths) {
        healths << objectMapper.convertValue(targetHealth, new TypeReference<Map<String, Object>>() {})
      }
    }
    return healths as List<Map<String, Object>>
  }

  @Override
  HealthState getHealthState() {
    someUpRemainingUnknown(health) ? HealthState.Up :
      anyStarting(health) ? HealthState.Starting :
        anyDown(health) ? HealthState.Down :
          anyOutOfService(health) ? HealthState.OutOfService : HealthState.Unknown
  }

  Moniker getMoniker() {
    return NamerRegistry.lookup()
      .withProvider(HeCloudProvider.ID)
      .withAccount(account)
      .withResource(HeCloudBasicResource)
      .deriveMoniker(this)
  }

  private static boolean someUpRemainingUnknown(List<Map<String, String>> healthList) {
    List<Map<String, String>> knownHealthList = healthList?.findAll {
      it.state != HealthState.Unknown.toString()
    }
    knownHealthList ? knownHealthList.every {
      it.state == HealthState.Up.toString()
    } : false
  }

  private static boolean anyStarting(List<Map<String, String>> healthList) {
    healthList.any {
      it.state == HealthState.Starting.toString()
    }
  }

  private static boolean anyDown(List<Map<String, String>> healthList) {
    healthList.any {
      it.state == HealthState.Down.toString()
    }
  }

  private static boolean anyOutOfService(List<Map<String, String>> healthList) {
    healthList.any {
      it.state == HealthState.OutOfService.toString()
    }
  }
}
