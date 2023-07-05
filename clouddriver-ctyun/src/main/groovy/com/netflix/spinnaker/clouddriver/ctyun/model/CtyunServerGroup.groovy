package com.netflix.spinnaker.clouddriver.ctyun.model

import com.netflix.spinnaker.clouddriver.model.HealthState
import com.netflix.spinnaker.clouddriver.model.Instance
import com.netflix.spinnaker.clouddriver.model.ServerGroup
import com.netflix.spinnaker.clouddriver.names.NamerRegistry
import com.netflix.spinnaker.clouddriver.ctyun.CtyunCloudProvider
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunAutoScalingClient
import com.netflix.spinnaker.moniker.Moniker

import java.text.SimpleDateFormat

class CtyunServerGroup implements ServerGroup, CtyunBasicResource {
  final String type = CtyunCloudProvider.ID
  final String cloudProvider = CtyunCloudProvider.ID
  String accountName
  String name
  String region
  Set<String> zones
  Set<CtyunInstance> instances = []
  Map<String, Object> image = [:]
  Map<String, Object> launchConfig = [:]
  Map<String, Object> asg = [:]
  Map buildInfo
  String vpcId

  Integer cooldown //冷却时间需返回用于回显
  List<Map<String,Object>> mazInfoList//可用区回显需返回


  List<Map> scalingPolicies
  List<Map> scheduledActions
  List<Map> loadBlanders

  Boolean disabled = false

  @Override
  Moniker getMoniker() {
    return NamerRegistry.lookup()
      .withProvider(CtyunCloudProvider.ID)
      .withAccount(accountName)
      .withResource(CtyunBasicResource)
      .deriveMoniker(this)
  }

  @Override
  String getMonikerName() {
    name
  }

  @Override
  Boolean isDisabled() {
    disabled
  }

  @Override
  Long getCreatedTime() {
   // def dateTime = CtyunAutoScalingClient.ConvertIsoDateTime(asg.createDate as String)
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    def dateTime  = asg.createDate!=null?sdf.parse(asg.createDate):null
    dateTime ? dateTime.time : null
  }

  @Override
  Set<String> getLoadBalancers() {
    def loadBalancerNames = []
    /*if (asg && asg.containsKey("forwardLoadBalancerSet")) {
      loadBalancerNames.addAll(asg.forwardLoadBalancerSet.collect {
        it["loadBalancerId"]
      })
    }

    if (asg && asg.containsKey("loadBalancerIdSet")) {
      loadBalancerNames.addAll(asg.loadBalancerIdSet)
    }*/
    loadBlanders.each {
      loadBalancerNames.add(it.lbID)
    }

    return loadBalancerNames as Set<String>
  }

  @Override
  Set<String> getSecurityGroups() {
    def securityGroups = []
    if (launchConfig && launchConfig.containsKey("securityGroupList")) {
      securityGroups = launchConfig.securityGroupList
    }
    securityGroups as Set<String>
  }

  @Override
  InstanceCounts getInstanceCounts() {
    new InstanceCounts(
      total: instances?.size() ?: 0,
      up: filterInstancesByHealthState(instances, HealthState.Up)?.size() ?: 0,
      down: filterInstancesByHealthState(instances, HealthState.Down)?.size() ?: 0,
      unknown: filterInstancesByHealthState(instances, HealthState.Unknown)?.size() ?: 0,
      starting: filterInstancesByHealthState(instances, HealthState.Starting)?.size() ?: 0,
      outOfService: filterInstancesByHealthState(instances, HealthState.OutOfService)?.size() ?: 0
    )
  }

  @Override
  Capacity getCapacity() {
    asg ? new Capacity(
      min: asg.minSize ? asg.minSize as Integer : 0,
      max: asg.maxSize ? asg.maxSize as Integer : 0,
      desired: asg.expectedCount ? asg.expectedCount as Integer : 0
    ) : null
  }

  @Override
  ImagesSummary getImagesSummary() {
    def buildInfo = buildInfo
    def image =image
    return new ImagesSummary() {
      @Override
      List<ImageSummary> getSummaries() {
        return [new ImageSummary() {
          String serverGroupName = name
          String imageName = image?.name
          String imageId = image?.imageId

          @Override
          Map<String, Object> getBuildInfo() {
            return buildInfo
          }

          @Override
          Map<String, Object> getImage() {
            return image
          }
        }]
      }
    }
  }

  @Override
  ImageSummary getImageSummary() {
    imagesSummary?.summaries?.get(0)
  }

  static Collection<Instance> filterInstancesByHealthState(Collection<Instance> instances, HealthState healthState) {
    instances.findAll { Instance it -> it.getHealthState() == healthState }
  }

}
