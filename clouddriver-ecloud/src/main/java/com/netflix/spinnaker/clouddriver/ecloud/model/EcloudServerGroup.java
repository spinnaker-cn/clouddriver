package com.netflix.spinnaker.clouddriver.ecloud.model;

import com.netflix.spinnaker.clouddriver.model.HealthState;
import com.netflix.spinnaker.clouddriver.model.ServerGroup;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * @author xu.dangling
 * @date 2024/4/9 @Description
 */
@Getter
@Setter
public class EcloudServerGroup implements ServerGroup {

  private String name;
  private String provider;
  private String type;
  private String cloudProvider;
  private String region;
  private Boolean disabled;
  private Long createdTime;
  private Set<String> zones;
  private Set<EcloudInstance> instances;
  private Set<String> loadBalancers;
  private Set<String> securityGroups;
  private Map<String, Object> launchConfig;
  private InstanceCounts instanceCounts;
  private Capacity capacity;
  private ImagesSummary imagesSummary;
  // extend attributes
  private String scalingGroupId;
  private String vpcId;
  private AutoScalingGroup asg;
  private List<ScalingPolicy> scalingPolicies;
  private List<ScheduledAction> scheduledActions;
  private Map<String, String> nodes;
  private List<ForwardLoadBalancer> forwardLoadBalancers;

  @Override
  public Boolean isDisabled() {
    return disabled;
  }

  @Override
  public InstanceCounts getInstanceCounts() {
    if (instances != null) {
      return ServerGroup.InstanceCounts.builder()
          .total(instances.size())
          .down(
              (int)
                  instances.stream()
                      .filter(i -> i.getHealthState().equals(HealthState.Down))
                      .count())
          .up(
              (int)
                  instances.stream().filter(i -> i.getHealthState().equals(HealthState.Up)).count())
          .unknown(
              (int)
                  instances.stream()
                      .filter(i -> i.getHealthState().equals(HealthState.Unknown))
                      .count())
          .build();
    }
    return null;
  }

  @Override
  public ImageSummary getImageSummary() {
    return null;
  }

  public static class EcloudImageSummary implements ServerGroup.ImageSummary {

    private Map<String, Object> i;
    private String serverGroupName;

    public EcloudImageSummary(Map<String, Object> i, String serverGroupName) {
      this.i = i;
      this.serverGroupName = serverGroupName;
    }

    @Override
    public String getServerGroupName() {
      return serverGroupName;
    }

    @Override
    public String getImageId() {
      return (String) i.get("imageId");
    }

    @Override
    public String getImageName() {
      return (String) i.get("imageName");
    }

    @Override
    public Map<String, Object> getImage() {
      return i;
    }

    @Override
    public Map<String, Object> getBuildInfo() {
      return null;
    }
  }

  @Getter
  @Setter
  public static class AutoScalingGroup {
    private Set<String> availabilityZones;
    private int defaultCooldown;
    private Set<String> terminationPolicies;
    private Set<String> enabledMetrics;
    private String vpczoneIdentifier;
    private Set<String> suspendedProcesses;
    private Set<String> zoneSet;
    private Set<String> terminationPolicySet;
    private String vpcId;
    private String vpcName;
    private String routerId;
    private Set<String> subnetIdSet;
    private Integer instanceCount;
    private Integer minSize;
    private Integer maxSize;
    private Integer desiredCapacity;
    private List<EcloudTag> tags;
    private String multiRegionCreatePolicy;
  }

  @Getter
  @Setter
  public static class ScalingPolicy {
    private String autoScalingPolicyId;
    private String policyARN;
    private String policyName;
    private String policyType;
    private String taskDescription;
    private String scalingRuleId;
    private String scalingRuleName;
    private String scalingRuleType;
    private Integer cooldown;
    private String adjustmentType;
    private Integer minAdjustmentStep;
    private Integer minAdjustmentMagnitude;
    private Integer adjustmentValue;
    private MetricAlarm metricAlarm;
    private ScheduledTask scheduledTask;
  }

  @Getter
  @Setter
  public static class MetricAlarm {
    private String metricName;
    private String metricType;
    private String statistic;
    private String comparisonOperator;
    private Integer threshold;
    private Integer period;
    private Integer continuousTime;
  }

  @Getter
  @Setter
  public static class ScheduledTask {
    private String triggerTime;
    private String periodName;
    private String periodValue;
    private Integer retryExpireTime;
  }

  @Getter
  @Setter
  public static class ScheduledAction {
    private String scheduledActionId;
    private String scheduledActionName;
    private String status;
    private String startTime;
    private String endTime;
    private int desiredCapacity;
    private int realScalingSize;
  }

  @Getter
  @Setter
  public static class LauchConfiguartion {
    private String lauchConfigurationId;
    private String lauchConfigurationName;
    private String instanceType;
    private Map systemDisk;
    private List<Map> dataDisks;
    private Set<String> securityGroupIds;
    private String imageId;
    private String imageName;
    private String launchConfigurationStatus;
    private List<Map> tags;
    private List<String> instanceTypes;
    private List<EcloudTag> instanceTags;
    private Map<String, String> instanceNameSettings;
    private InternetAccessible internetAccessible;
    private LoginSettings loginSettings;
    private EnhancedService enhancedService;
    private Boolean securityReinforce;
    private String createdTime;
  }

  @Getter
  @Setter
  public static class LoginSettings {
    private List<String> keyIds;
    private String password;
  }

  @Getter
  @Setter
  public static class InternetAccessible {
    private String internetChargeType;
    private String internetMaxBandwidthOut;
    private Boolean publicIpAssigned;
  }

  @Getter
  @Setter
  public static class EnhancedService {
    private Map<String, Boolean> securityService;
  }

  @Getter
  @Setter
  public static class ForwardLoadBalancer {
    private String loadBalancerId;
    private String poolId;
    private Integer port;
    private Integer weight;
    private String subnetId;
  }
}
