package com.netflix.spinnaker.clouddriver.ecloud.provider.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.CacheFilter;
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudCluster;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudInstance;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudServerGroup;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudTag;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudZone;
import com.netflix.spinnaker.clouddriver.ecloud.provider.agent.EcloudZoneHelper;
import com.netflix.spinnaker.clouddriver.model.ClusterProvider;
import com.netflix.spinnaker.clouddriver.model.ServerGroup;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @author xu.dangling
 * @date 2024/4/9 @Description
 */
@Slf4j
@Component
public class EcloudClusterProvider implements ClusterProvider<EcloudCluster> {

  private final ObjectMapper objectMapper;

  private final Cache cacheView;

  private final EcloudProvider provider;

  @Autowired EcloudInstanceProvider ecloudInstanceProvider;

  @Autowired
  public EcloudClusterProvider(
      ObjectMapper objectMapper, Cache cacheView, EcloudProvider provider) {
    this.objectMapper = objectMapper;
    this.cacheView = cacheView;
    this.provider = provider;
  }

  @Override
  public Map<String, Set<EcloudCluster>> getClusters() {
    Collection<CacheData> clusterData = cacheView.getAll(Keys.Namespace.CLUSTERS.ns);
    Set<EcloudCluster> clusters = this.translateClusters(clusterData, false);
    return clusters.stream()
        .collect(Collectors.groupingBy(EcloudCluster::getAccountName))
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> new HashSet<>(entry.getValue())));
  }

  @Override
  public Map<String, Set<EcloudCluster>> getClusterSummaries(String application) {
    CacheData applicationCache =
        cacheView.get(Keys.Namespace.APPLICATIONS.ns, Keys.getApplicationKey(application));
    if (applicationCache != null) {
      Set<EcloudCluster> clusters =
          translateClusters(
              resolveRelationshipData(applicationCache, Keys.Namespace.CLUSTERS.ns, null), false);
      return clusters.stream()
          .collect(Collectors.groupingBy(EcloudCluster::getAccountName, Collectors.toSet()));
    } else {
      return null;
    }
  }

  @Override
  public Map<String, Set<EcloudCluster>> getClusterDetails(String application) {
    CacheData applicationCache =
        cacheView.get(Keys.Namespace.APPLICATIONS.ns, Keys.getApplicationKey(application));
    if (applicationCache != null) {
      Set<EcloudCluster> clusters =
          translateClusters(
              resolveRelationshipData(
                  applicationCache,
                  Keys.Namespace.CLUSTERS.ns,
                  RelationshipCacheFilter.include(
                      Keys.Namespace.SERVER_GROUPS.ns, Keys.Namespace.LOAD_BALANCERS.ns)),
              true);
      return clusters.stream()
          .collect(Collectors.groupingBy(EcloudCluster::getAccountName, Collectors.toSet()));
    } else {
      return null;
    }
  }

  @Override
  public Set<EcloudCluster> getClusters(String application, String account) {
    CacheData applicationCache =
        cacheView.get(Keys.Namespace.APPLICATIONS.ns, Keys.getApplicationKey(application));
    if (applicationCache != null) {
      Collection<String> clusterKeys =
          applicationCache.getRelationships().get(Keys.Namespace.CLUSTERS).stream()
              .filter(key -> Keys.parse(key).get("account").equals(account))
              .collect(Collectors.toList());
      Collection<CacheData> clusters = cacheView.getAll(Keys.Namespace.CLUSTERS.ns, clusterKeys);
      return translateClusters(clusters, true);
    } else {
      return null;
    }
  }

  @Nullable
  @Override
  public EcloudCluster getCluster(String application, String account, String name) {
    CacheData cluster =
        cacheView.get(Keys.Namespace.CLUSTERS.ns, Keys.getClusterKey(name, application, account));
    return cluster == null ? null : translateCluster(cluster, true);
  }

  @Nullable
  @Override
  public EcloudCluster getCluster(
      String application, String account, String name, boolean includeDetails) {
    CacheData cluster =
        cacheView.get(Keys.Namespace.CLUSTERS.ns, Keys.getClusterKey(name, application, account));
    return cluster == null ? null : translateCluster(cluster, includeDetails);
  }

  @Nullable
  @Override
  public EcloudServerGroup getServerGroup(
      String account, String region, String name, boolean includeDetails) {
    CacheData cacheData =
        cacheView.get(
            Keys.Namespace.SERVER_GROUPS.ns, Keys.getServerGroupKey(name, account, region));
    if (cacheData != null) {
      EcloudServerGroup serverGroup = translateServerGroup(cacheData);
      return serverGroup;
    }
    return null;
  }

  @Nullable
  @Override
  public EcloudServerGroup getServerGroup(String account, String region, String name) {
    CacheData cacheData =
        cacheView.get(
            Keys.Namespace.SERVER_GROUPS.ns, Keys.getServerGroupKey(name, account, region));
    if (cacheData != null) {
      EcloudServerGroup serverGroup = translateServerGroup(cacheData);
      return serverGroup;
    }
    return null;
  }

  public Map<String, Object> getAttributes(String account, String region, String name) {
    CacheData cacheData =
        cacheView.get(
            Keys.Namespace.SERVER_GROUPS.ns, Keys.getServerGroupKey(name, account, region));
    if (cacheData != null) {
      return cacheData.getAttributes();
    }
    return null;
  }

  public String getServerGroupIdByName(String account, String region, String name) {
    CacheData serverGroup =
        cacheView.get(
            Keys.Namespace.SERVER_GROUPS.ns, Keys.getServerGroupKey(name, account, region));
    if (serverGroup != null) {
      return (String) serverGroup.getAttributes().get("scalingGroupId");
    }
    return null;
  }

  @Override
  public String getCloudProviderId() {
    return EcloudProvider.ID;
  }

  @Override
  public boolean supportsMinimalClusters() {
    return false;
  }

  private Set<EcloudCluster> translateClusters(
      Collection<CacheData> clusterData, boolean includeDetail) {
    Set<EcloudCluster> clusters =
        clusterData.stream()
            .map(one -> translateCluster(one, includeDetail))
            .collect(Collectors.toSet());
    return clusters;
  }

  private EcloudCluster translateCluster(CacheData clusterData, boolean includeDetail) {
    Map<String, String> clusterKey = Keys.parse(clusterData.getId());
    EcloudCluster cluster = new EcloudCluster();
    cluster.setAccountName(clusterKey.get("account"));
    cluster.setName(clusterKey.get("name"));
    cluster.setType(provider.getId());
    Collection<CacheData> clusterServerCache =
        this.resolveRelationshipData(
            clusterData,
            Keys.Namespace.SERVER_GROUPS.ns,
            RelationshipCacheFilter.include(
                Keys.Namespace.INSTANCES.ns, Keys.Namespace.LOAD_BALANCERS.ns));
    Map<String, EcloudServerGroup> serverGroups = translateServerGroups(clusterServerCache);
    cluster.setServerGroups(serverGroups.values().stream().collect(Collectors.toSet()));
    //    if (includeDetail) {
    //      Collection<CacheData> lbCache =
    //          this.resolveRelationshipData(clusterData, Keys.Namespace.LOAD_BALANCERS.ns, null);
    //      Set<EcloudLoadBalancer> lbSet = this.translateLoadBalancers(lbCache);
    //      cluster.setLoadBalancers(lbSet);
    //    }
    return cluster;
  }

  private Map<String, EcloudServerGroup> translateServerGroups(
      Collection<CacheData> serverGroupData) {
    Map<String, EcloudServerGroup> serverGroups = new HashMap<>(16);
    for (CacheData one : serverGroupData) {
      serverGroups.put(one.getId(), translateServerGroup(one));
    }
    return serverGroups;
  }

  public EcloudServerGroup translateServerGroup(CacheData serverGroupData) {
    Map<String, Object> attributes = serverGroupData.getAttributes();
    EcloudServerGroup serverGroup = new EcloudServerGroup();
    String serverGroupName = (String) attributes.get("scalingGroupName");
    serverGroup.setName(serverGroupName);
    serverGroup.setCloudProvider((String) attributes.get("provider"));
    serverGroup.setProvider((String) attributes.get("provider"));
    serverGroup.setType(provider.getId());
    serverGroup.setRegion((String) attributes.get("poolId"));
    serverGroup.setScalingGroupId((String) attributes.get("scalingGroupId"));
    String createdAt = (String) attributes.get("createdAt");
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    try {
      serverGroup.setCreatedTime(sdf.parse(createdAt).getTime());
    } catch (ParseException e) {
      log.error(e.getMessage(), e);
    }
    String status = (String) attributes.get("status");
    if ("INACTIVE".equalsIgnoreCase(status)) {
      serverGroup.setDisabled(true);
    }
    // asg
    EcloudServerGroup.AutoScalingGroup asg = new EcloudServerGroup.AutoScalingGroup();
    if (attributes.get("networkWithRegionList") != null) {
      List<Map> networkWithRegionList = (List<Map>) attributes.get("networkWithRegionList");
      Set<String> zones =
          networkWithRegionList.stream()
              .map(
                  one -> {
                    EcloudZone zone =
                        EcloudZoneHelper.getEcloudZone(
                            (String) attributes.get("account"),
                            (String) attributes.get("poolId"),
                            (String) one.get("region"));
                    return zone == null ? null : zone.getName();
                  })
              .collect(Collectors.toSet());
      asg.setZoneSet(zones);
      List<String> networkIdList = new ArrayList<>();
      networkWithRegionList.stream()
          .forEach(
              one -> {
                List<String> networkIds = (List<String>) one.get("networkIdList");
                if (networkIds != null) {
                  networkIdList.addAll(networkIds);
                }
              });
    }
    asg.setDesiredCapacity((Integer) attributes.get("desiredSize"));
    asg.setMaxSize((Integer) attributes.get("maxSize"));
    asg.setMinSize((Integer) attributes.get("minSize"));
    asg.setInstanceCount((Integer) attributes.get("nowCapacity"));
    List<EcloudTag> tags = new ArrayList<>();
    List<Map> tagList = (List<Map>) attributes.get("tagList");
    if (tagList != null && !tagList.isEmpty()) {
      for (Map map : tagList) {
        EcloudTag tag = new EcloudTag();
        tag.setKey((String) map.get("tagKey"));
        tag.setValue((String) map.get("tagValue"));
        tags.add(tag);
      }
    }
    asg.setTags(tags);
    asg.setVpcId((String) attributes.get("realVpcId"));
    asg.setRouterId((String) attributes.get("vpcId"));
    asg.setVpcName((String) attributes.get("vpcName"));
    asg.setTerminationPolicySet(Collections.singleton((String) attributes.get("removalPolicy")));
    asg.setMultiRegionCreatePolicy((String) attributes.get("multiRegionCreatePolicy"));
    serverGroup.setAsg(asg);
    // loadBalancer & subnets
    List<Map> loadBalancerInfos = (List<Map>) attributes.get("loadBalancers");
    List<EcloudServerGroup.ForwardLoadBalancer> lbs = new ArrayList<>();
    Set subnetIdSet = new HashSet<>();
    if (!CollectionUtils.isEmpty(loadBalancerInfos)) {
      for (Map lbInfo : loadBalancerInfos) {
        EcloudServerGroup.ForwardLoadBalancer lb = new EcloudServerGroup.ForwardLoadBalancer();
        lb.setLoadBalancerId((String) lbInfo.get("loadBalancerId"));
        lb.setPoolId((String) lbInfo.get("loadBalancerPoolId"));
        lb.setPort((Integer) lbInfo.get("memberPort"));
        lb.setWeight((Integer) lbInfo.get("memberWeight"));
        lbs.add(lb);
        List<String> subnetIds = (List<String>) lbInfo.get("subnetIds");
        if (!CollectionUtils.isEmpty(subnetIds)) {
          lb.setSubnetId(subnetIds.get(0));
          subnetIdSet.add(lb.getSubnetId());
        }
      }
    }
    asg.setSubnetIdSet(subnetIdSet);
    serverGroup.setForwardLoadBalancers(lbs);
    serverGroup.setLoadBalancers(
        lbs.stream()
            .map(EcloudServerGroup.ForwardLoadBalancer::getLoadBalancerId)
            .collect(Collectors.toSet()));
    // launchConfig
    EcloudServerGroup.LauchConfiguartion lc = new EcloudServerGroup.LauchConfiguartion();
    Map sc = (Map) attributes.get("scalingConfig");
    if (sc != null) {
      Set<String> securityGroups = new HashSet<>();
      if (sc.get("securityGroupInfoRespList") != null) {
        List<Map> secGrps = (List<Map>) sc.get("securityGroupInfoRespList");
        securityGroups =
            secGrps.stream()
                .map(one -> (String) one.get("securityGroupId"))
                .collect(Collectors.toSet());
      }
      serverGroup.setSecurityGroups(securityGroups);
      lc.setSecurityGroupIds(securityGroups);
      lc.setImageId((String) sc.get("imageId"));
      lc.setImageName((String) sc.get("imageName"));
      lc.setLauchConfigurationId((String) sc.get("scalingConfigId"));
      lc.setLauchConfigurationName((String) sc.get("scalingConfigName"));
      Map<String, String> instanceNameSettings = new HashMap<>(4);
      instanceNameSettings.put("instanceName", (String) sc.get("serverName"));
      instanceNameSettings.put("instanceNameStyle", "ORIGINAL");
      lc.setInstanceNameSettings(instanceNameSettings);
      List<Map> flavorInfoList = (List<Map>) sc.get("flavorInfoRespList");
      if (flavorInfoList != null && !flavorInfoList.isEmpty()) {
        List<String> instanceTypes =
            flavorInfoList.stream()
                .map(one -> (String) one.get("specsName"))
                .collect(Collectors.toList());
        lc.setInstanceType(String.join(",", instanceTypes));
        lc.setInstanceTypes(instanceTypes);
      }
      lc.setInstanceTags(tags);
      List<Map> volumeInfoList = (List<Map>) sc.get("volumeInfoRespList");
      if (volumeInfoList != null && !volumeInfoList.isEmpty()) {
        List<Map> dataDisks = new ArrayList<>();
        for (Map info : volumeInfoList) {
          Map<String, Object> disk = new HashMap<>();
          disk.put("diskType", (String) info.get("volumeType"));
          disk.put("diskSize", (Integer) info.get("volumeSize"));
          if ((boolean) info.get("bootable")) {
            lc.setSystemDisk(disk);
          } else {
            dataDisks.add(disk);
          }
        }
        lc.setDataDisks(dataDisks);
      }
      lc.setLaunchConfigurationStatus((String) sc.get("opStatus"));
      lc.setCreatedTime((String) sc.get("createdAt"));
      EcloudServerGroup.LoginSettings loginSettings = new EcloudServerGroup.LoginSettings();
      if (sc.get("accessInfoRespList") != null) {
        List<Map> accessList = (List<Map>) sc.get("accessInfoRespList");
        List<String> keyPairs =
            accessList.stream()
                .filter(one -> "KEYPAIR".equals((String) one.get("accessType")))
                .map(one -> (String) one.get("keypairName"))
                .collect(Collectors.toList());
        loginSettings.setKeyIds(keyPairs);
      }
      lc.setLoginSettings(loginSettings);
      EcloudServerGroup.InternetAccessible internetAccessible =
          new EcloudServerGroup.InternetAccessible();
      Map fip = (Map) sc.get("fipAndBandwidth");
      if (fip == null) {
        internetAccessible.setPublicIpAssigned(false);
      } else {
        internetAccessible.setPublicIpAssigned(true);
        internetAccessible.setInternetChargeType((String) fip.get("chargeType"));
        internetAccessible.setInternetMaxBandwidthOut((Integer) fip.get("bandwidthSize"));
        internetAccessible.setFipType((String) fip.get("fipType"));
      }
      lc.setInternetAccessible(internetAccessible);
      EcloudServerGroup.EnhancedService enhancedService = new EcloudServerGroup.EnhancedService();
      Map<String, Boolean> securityService = new HashMap<>();
      Boolean securityReinforce = (Boolean) sc.get("securityReinforce");
      securityService.put("enable", securityReinforce);
      enhancedService.setSecurityService(securityService);
      lc.setEnhancedService(enhancedService);
      lc.setSecurityReinforce(securityReinforce);
      serverGroup.setLaunchConfig(objectMapper.convertValue(lc, Map.class));
    }
    // scalingPolices: scalingRule+alarmTasks+scheduledTask
    List<EcloudServerGroup.ScalingPolicy> sps = new ArrayList<>();
    Map<String, Map> sgRuleMap = (Map<String, Map>) attributes.get("scalingRules");
    List<Map> alarmTasks = (List<Map>) attributes.get("alarmTasks");
    List<Map> scheduledTasks = (List<Map>) attributes.get("scheduledTasks");
    if (!CollectionUtils.isEmpty(alarmTasks) && sgRuleMap != null) {
      for (Map map : alarmTasks) {
        EcloudServerGroup.ScalingPolicy sp = new EcloudServerGroup.ScalingPolicy();
        String sgRuleId = (String) map.get("scalingRuleId");
        Map sgRule = sgRuleMap.get(sgRuleId);
        sp.setAutoScalingPolicyId((String) map.get("alarmTaskId"));
        sp.setPolicyType((String) map.get("scalingRuleType"));
        sp.setPolicyName((String) map.get("alarmTaskName"));
        EcloudServerGroup.MetricAlarm metricAlarm = new EcloudServerGroup.MetricAlarm();
        metricAlarm.setMetricName((String) map.get("metricName"));
        metricAlarm.setMetricType((String) map.get("monitorType"));
        metricAlarm.setPeriod(Integer.parseInt((String) map.get("period")));
        metricAlarm.setStatistic((String) map.get("statistics"));
        metricAlarm.setComparisonOperator((String) map.get("comparisonOperator"));
        metricAlarm.setThreshold((Integer) map.get("threshold"));
        metricAlarm.setContinuousTime((Integer) map.get("evaluationCount"));
        sp.setMetricAlarm(metricAlarm);
        sp.setScalingRuleId(sgRuleId);
        sp.setScalingRuleType((String) sgRule.get("scalingRuleType"));
        sp.setScalingRuleName((String) sgRule.get("scalingRuleName"));
        sp.setCooldown((Integer) sgRule.get("coolDown"));
        sp.setAdjustmentType((String) sgRule.get("adjustmentType"));
        sp.setAdjustmentValue((Integer) sgRule.get("adjustmentValue"));
        sp.setMinAdjustmentMagnitude((Integer) sgRule.get("minAdjustmentValue"));
        sp.setTaskDescription((String) map.get("description"));
        sps.add(sp);
      }
    }
    if (!CollectionUtils.isEmpty(scheduledTasks) && sgRuleMap != null) {
      for (Map map : scheduledTasks) {
        EcloudServerGroup.ScalingPolicy sp = new EcloudServerGroup.ScalingPolicy();
        String sgRuleId = (String) map.get("scalingRuleId");
        Map sgRule = sgRuleMap.get(sgRuleId);
        sp.setAutoScalingPolicyId((String) map.get("scheduledTaskId"));
        sp.setPolicyType((String) map.get("taskType"));
        sp.setPolicyName((String) map.get("scheduledTaskName"));
        EcloudServerGroup.ScheduledTask scheduleTask = new EcloudServerGroup.ScheduledTask();
        scheduleTask.setTriggerTime((String) map.get("triggerTime"));
        scheduleTask.setPeriodName((String) map.get("period"));
        scheduleTask.setPeriodValue((String) map.get("periodValue"));
        scheduleTask.setRetryExpireTime((Integer) map.get("retryExpireTime"));
        sp.setScheduledTask(scheduleTask);
        sp.setScalingRuleId(sgRuleId);
        sp.setScalingRuleType((String) sgRule.get("scalingRuleType"));
        sp.setScalingRuleName((String) sgRule.get("scalingRuleName"));
        sp.setCooldown((Integer) sgRule.get("coolDown"));
        sp.setAdjustmentType((String) sgRule.get("adjustmentType"));
        sp.setAdjustmentValue((Integer) sgRule.get("adjustmentValue"));
        sp.setMinAdjustmentMagnitude((Integer) sgRule.get("minAdjustmentValue"));
        sp.setTaskDescription((String) map.get("description"));
        sps.add(sp);
      }
    }
    serverGroup.setScalingPolicies(sps);
    // scheduledActions
    List<Map> scheduledActions = (List<Map>) attributes.get("scheduledActions");
    List<EcloudServerGroup.ScheduledAction> sas = new ArrayList<>();
    if (!CollectionUtils.isEmpty(scheduledActions)) {
      for (Map map : scheduledActions) {
        EcloudServerGroup.ScheduledAction action = new EcloudServerGroup.ScheduledAction();
        action.setScheduledActionId((String) map.get("scalingActivityId"));
        action.setScheduledActionName(
            map.get("scalingActivityType") + " Triggered By " + map.get("scalingActivityTrigger"));
        action.setStatus((String) map.get("scalingActivityStatus"));
        action.setStartTime((String) map.get("createdAt"));
        action.setEndTime((String) map.get("finishedAt"));
        action.setDesiredCapacity((int) map.get("desireScalingNodeNumber"));
        action.setRealScalingSize((int) map.get("realScalingNodeNumber"));
        sas.add(action);
      }
    }
    serverGroup.setScheduledActions(sas);
    String account = (String) attributes.get("account");
    String region = (String) attributes.get("poolId");
    serverGroup.setCapacity(
        ServerGroup.Capacity.builder()
            .max(asg.getMaxSize())
            .min(asg.getMinSize())
            .desired(asg.getDesiredCapacity())
            .build());
    // nodes
    Set<EcloudInstance> instanceSet =
        translateServerGroupInstances(serverGroupData, account, region);
    serverGroup.setInstances(instanceSet);
    return serverGroup;
  }

  public Set<EcloudInstance> translateServerGroupInstances(
      CacheData serverGroupData, String account, String region) {
    Collection<CacheData> instanceCache =
        this.resolveRelationshipData(serverGroupData, Keys.Namespace.INSTANCES.ns, null);
    Set<EcloudInstance> instanceSet = new HashSet<>();
    instanceCache.forEach(
        one -> {
          instanceSet.add(ecloudInstanceProvider.instanceFromCacheData(one, account, region));
        });
    return instanceSet;
  }

  private Collection<CacheData> resolveRelationshipData(
      CacheData source, String relationship, CacheFilter cacheFilter) {
    Collection<String> relationKeys = new HashSet<>();
    if (source != null) {
      relationKeys = source.getRelationships().get(relationship);
    }
    if (relationKeys != null) {
      return cacheView.getAll(relationship, relationKeys, cacheFilter);
    }
    return new ArrayList<>();
  }
}
