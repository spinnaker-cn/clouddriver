package com.netflix.spinnaker.clouddriver.ecloud.deploy.handlers;

import com.alibaba.fastjson2.JSONObject;
import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.deploy.DeployDescription;
import com.netflix.spinnaker.clouddriver.deploy.DeployHandler;
import com.netflix.spinnaker.clouddriver.deploy.DeploymentResult;
import com.netflix.spinnaker.clouddriver.ecloud.client.openapi.EcloudOpenApiHelper;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.EcloudServerGroupNameResolver;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.EcloudDeployDescription;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudRequest;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudResponse;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudServerGroup;
import com.netflix.spinnaker.clouddriver.ecloud.provider.view.EcloudClusterProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class EcloudDeployHandler implements DeployHandler<EcloudDeployDescription> {

  private static final Logger log = LoggerFactory.getLogger(EcloudDeployHandler.class);

  private static final String BASE_PHASE = "DEPLOY";

  @Autowired private EcloudClusterProvider ecloudClusterProvider;

  private static Task getTask() {
    return TaskRepository.threadLocalTask.get();
  }

  @Override
  public boolean handles(DeployDescription description) {
    return description instanceof EcloudDeployDescription;
  }

  @Override
  public DeploymentResult handle(EcloudDeployDescription description, List priorOutputs) {
    DeploymentResult deploymentResult = new DeploymentResult();
    Task task = getTask();
    task.updateStatus(BASE_PHASE, "Initializing deployment to " + description.getRegion());

    String region = description.getRegion();
    EcloudServerGroupNameResolver serverGroupNameResolver =
        new EcloudServerGroupNameResolver(
            description.getAccountName(), description.getRegion(), ecloudClusterProvider);
    String serverGroupName =
        serverGroupNameResolver.resolveNextServerGroupName(
            description.getApplication(), description.getStack(), description.getDetail(), false);

    task.updateStatus(BASE_PHASE, "Produce server group name: " + serverGroupName);
    description.setServerGroupName(serverGroupName);

    boolean useSourceConfig = false;
    boolean copyScalingPolies = false;
    String scalingConfigId = null;
    List<EcloudServerGroup.ScalingPolicy> scalingPolicies = null;
    if (description.getSource() != null) {
      String sourceServerGroupName = description.getSource().getServerGroupName();
      String sourceRegion = description.getSource().getRegion();
      String accountName = description.getAccountName();
      boolean useSourceCapacity = description.getSource().getUseSourceCapacity();
      EcloudServerGroup sourceServerGroup =
          ecloudClusterProvider.getServerGroup(accountName, sourceRegion, sourceServerGroupName);
      if (sourceServerGroup == null) {
        task.updateStatus(BASE_PHASE, "Fail to get source serverGroup:" + sourceServerGroup);
        deploymentResult.getMessages().add("Fail to get source server group config");
        return deploymentResult;
      } else {
        log.info("Source Server Group:" + sourceServerGroupName);
        // compare configs -- CloneAction
        Map<String, Object> launchConfig = sourceServerGroup.getLaunchConfig();
        boolean sameConfig = this.compareScalingConfig(description, launchConfig);
        if (!sameConfig) {
          log.info(
              "Scaling config is not the same with the source server group.It will be created.");
        } else {
          useSourceConfig = true;
          scalingConfigId =
              (String) sourceServerGroup.getLaunchConfig().get("lauchConfigurationId");
        }
        if (useSourceCapacity) {
          description.setMaxSize(sourceServerGroup.getCapacity().getMax());
          description.setMinSize(sourceServerGroup.getCapacity().getMin());
          description.setDesiredCapacity(sourceServerGroup.getCapacity().getDesired());
        }
        if (!CollectionUtils.isEmpty(sourceServerGroup.getScalingPolicies())) {
          copyScalingPolies = true;
        }
      }
    }
    if (!useSourceConfig) {
      scalingConfigId = this.createScalingConfig(description);
    }
    if (scalingConfigId == null) {
      task.updateStatus(BASE_PHASE, "Create server group config " + serverGroupName + " Failed...");
      deploymentResult.getMessages().add("Fail to create server group config");
      return deploymentResult;
    }
    // create server group
    description.setScalingConfigId(scalingConfigId);
    task.updateStatus(BASE_PHASE, "Composing server group " + serverGroupName + "...");
    String scalingGroupId = this.createScalingGroup(description);
    if (scalingGroupId == null) {
      // delete scaling config
      if (!useSourceConfig) {
        this.deleteScalingConfig(description);
      }
      task.updateStatus(BASE_PHASE, "Create server group " + serverGroupName + " Failed...");
      deploymentResult.getMessages().add("Fail to create server group");
      return deploymentResult;
    }
    description.setScalingGroupId(scalingGroupId);
    // create scalingPolicies
    if (copyScalingPolies) {
      boolean spCreated = this.createScalingPolicies(description);
      if (!spCreated) {
        // destroy scaling group
        this.destroyScalingGroup(description);
        // delete scaling config
        if (!useSourceConfig) {
          this.deleteScalingConfig(description);
        }
        task.updateStatus(BASE_PHASE, "Create scaling policies " + serverGroupName + " Failed...");
        deploymentResult.getMessages().add("Fail to create scaling policies");
        return deploymentResult;
      }
    }
    boolean enable = this.enableScalingGroup(description);
    if (!enable) {
      // destroy scaling group
      this.destroyScalingGroup(description);
      // delete scaling config
      if (!useSourceConfig) {
        this.deleteScalingConfig(description);
      }
      task.updateStatus(BASE_PHASE, "Enable server group " + serverGroupName + " Failed...");
      deploymentResult.getMessages().add("Fail to enable server group");
      return deploymentResult;
    }
    task.updateStatus(
        BASE_PHASE, "Done creating server group " + serverGroupName + " in " + region + ".");

    deploymentResult.getServerGroupNames().add(region + ":" + serverGroupName);
    deploymentResult.getServerGroupNameByRegion().put(region, serverGroupName);

    return deploymentResult;
  }

  private boolean compareScalingConfig(
      EcloudDeployDescription description, Map<String, Object> launchConfig) {
    if (launchConfig == null) {
      return false;
    }
    // image
    if (!description.getImageId().equals(launchConfig.get("imageId"))) {
      log.info("[Compare Scaling Config: Different ImageId]");
      return false;
    }
    // instanceType
    List<String> instanceTypes =
        description.getInstanceTypeRelas().stream()
            .map(i -> i.getInstanceType())
            .collect(Collectors.toList());
    List<String> lcInstanceTypes = (List<String>) launchConfig.get("instanceTypes");
    Collections.sort(instanceTypes);
    Collections.sort(lcInstanceTypes);
    if (!instanceTypes.equals(lcInstanceTypes)) {
      log.info("[Compare Scaling Config: Different InstanceTypes]");
      return false;
    }
    // system disk
    Map systemDisk = (Map) launchConfig.get("systemDisk");
    String diskType = (String) systemDisk.get("diskType");
    Integer diskSize = (Integer) systemDisk.get("diskSize");
    if (!description.getSystemDisk().getDiskType().equals(diskType)
        || !description.getSystemDisk().getDiskSize().equals(diskSize)) {
      log.info("[Compare Scaling Config: Different SystemDisk]");
      return false;
    }
    // security Groups
    Set<String> lcSecurityGroups =
        new HashSet<String>((Collection<String>) launchConfig.get("securityGroupIds"));
    if (!description.getSecurityGroups().equals(lcSecurityGroups)) {
      log.info("[Compare Scaling Config: Different securityGroupIds]");
      return false;
    }
    // access
    Map<String, Object> loginSettings = (Map<String, Object>) launchConfig.get("loginSettings");
    String oriKeyPair = null;
    List<String> keyIds = (List<String>) loginSettings.get("keyIds");
    if (keyIds != null && !keyIds.isEmpty()) {
      oriKeyPair = keyIds.get(0);
    }
    if (oriKeyPair == null) {
      if (description.getKeyPairName() != null) {
        log.info("[Compare Scaling Config: Different KeyPairName]");
        return false;
      }
    } else if (!oriKeyPair.equals(description.getKeyPairName())) {
      log.info("[Compare Scaling Config: Different KeyPairName]");
      return false;
    }
    return true;
  }

  private String createScalingConfig(EcloudDeployDescription description) {
    EcloudRequest request =
        new EcloudRequest(
            "POST",
            description.getRegion(),
            "/api/v4/autoScaling/scalingConfig/create/v2",
            description.getCredentials().getAccessKey(),
            description.getCredentials().getSecretKey());
    Map<String, Object> params = new HashMap<>();
    params.put("scalingConfigName", description.getServerGroupName());
    Map<String, String> access = new HashMap<>();
    if (!StringUtils.isEmpty(description.getKeyPairName())) {
      access.put("accessType", "KEYPAIR");
      access.put("keypairName", description.getKeyPairName());
    } else {
      access.put("accessType", "SET_AFTER_CREATE");
    }
    params.put("access", access);
    params.put("imageId", description.getImageId());
    params.put("imageType", description.getIsPublic() == 0 ? "PRIVATE" : "PUBLIC");
    params.put(
        "securityReinforce",
        description.getSecurityReinforce() == null ? false : description.getSecurityReinforce());
    params.put("serverName", description.getServerGroupName());
    // spec
    List<Map> scalingConfigCreateFlavorList = new ArrayList<>();
    for (EcloudDeployDescription.InstanceTypeRela instanceTypeRela :
        description.getInstanceTypeRelas()) {
      Map<String, Object> flavor = new HashMap<>();
      flavor.put("specsName", instanceTypeRela.getInstanceType());
      flavor.put("cpu", instanceTypeRela.getCpu());
      flavor.put("memory", instanceTypeRela.getMem());
      flavor.put("index", instanceTypeRela.getIndex() + 1);
      flavor.put("disk", 20);
      scalingConfigCreateFlavorList.add(flavor);
    }
    params.put("scalingConfigCreateFlavorList", scalingConfigCreateFlavorList);
    // disk
    List<Map> scalingConfigCreateVolumeList = new ArrayList<>();
    Map<String, Object> systemDisk = new HashMap<>();
    systemDisk.put("bootable", true);
    systemDisk.put("volumeType", description.getSystemDisk().getDiskType());
    systemDisk.put("volumeSize", description.getSystemDisk().getDiskSize());
    scalingConfigCreateVolumeList.add(systemDisk);
    if (!CollectionUtils.isEmpty(description.getDataDisks())) {
      for (EcloudDeployDescription.Disk dataDisk : description.getDataDisks()) {
        Map<String, Object> map = new HashMap<>();
        map.put("bootable", false);
        map.put("volumeType", dataDisk.getDiskType());
        map.put("volumeSize", dataDisk.getDiskSize());
        scalingConfigCreateVolumeList.add(map);
      }
    }
    params.put("scalingConfigCreateVolumeList", scalingConfigCreateVolumeList);
    // securityGroups
    List<Map> scalingConfigCreateSecurityGroupList = new ArrayList<>();
    for (String securityGroup : description.getSecurityGroups()) {
      Map<String, Object> map = new HashMap<>();
      map.put("securityGroupId", securityGroup);
      scalingConfigCreateSecurityGroupList.add(map);
    }
    params.put("scalingConfigCreateSecurityGroupList", scalingConfigCreateSecurityGroupList);
    log.info("create ScalingConfig:" + JSONObject.toJSONString(params));
    request.setBodyParams(params);
    EcloudResponse response = EcloudOpenApiHelper.execute(request);
    if (response.getErrorMessage() != null) {
      log.error("Create ScalingConfig Failed:" + response.getErrorMessage());
    }
    Map body = (Map) response.getBody();
    if (body != null) {
      return (String) body.get("scalingConfigId");
    } else {
      log.error("Create ScalingConfig Return Empty Body!");
    }
    return null;
  }

  private String createScalingGroup(EcloudDeployDescription description) {
    EcloudRequest request =
        new EcloudRequest(
            "POST",
            description.getRegion(),
            "/api/v4/autoScaling/scalingGroup",
            description.getCredentials().getAccessKey(),
            description.getCredentials().getSecretKey());
    Map<String, Object> params = new HashMap<>();
    params.put("scalingGroupName", description.getServerGroupName());
    params.put("maxSize", description.getMaxSize());
    params.put("minSize", description.getMinSize());
    params.put("desiredSize", description.getDesiredCapacity());
    params.put("scalingConfigId", description.getScalingConfigId());
    params.put("vpcId", description.getRouterId());
    List<Map> subnets = new ArrayList<>();
    for (EcloudDeployDescription.SubnetRela one : description.getSubnets()) {
      Map<String, Object> map = new HashMap<>();
      map.put("subnetId", one.getNetworkId());
      map.put("region", one.getZone());
      map.put("priorityIndex", one.getIndex() + 1);
      subnets.add(map);
    }
    params.put("subnetList", subnets);
    params.put(
        "multiRegionCreatePolicy",
        description.getMultiRegionCreatePolicy() == null
            ? "PRIORITY"
            : description.getMultiRegionCreatePolicy());
    if (!CollectionUtils.isEmpty(description.getForwardLoadBalancers())) {
      List<Map> loadBalancerList = new ArrayList<>();
      for (EcloudDeployDescription.ForwardLoadBalancer one :
          description.getForwardLoadBalancers()) {
        Map<String, Object> map = new HashMap<>();
        map.put("loadBalanceId", one.getLoadBalancerId());
        map.put("poolId", one.getPoolId());
        map.put("memberPort", one.getPort());
        map.put("memberWeight", one.getWeight());
        map.put("subnetIds", Collections.singleton(one.getSubnetId()));
        map.put("region", one.getZone());
        loadBalancerList.add(map);
      }
      params.put("loadBalancerList", loadBalancerList);
    }
    params.put("removalPolicy", description.getTerminationPolicy());
    if (!CollectionUtils.isEmpty(description.getTags())) {
      List<Map> tags = new ArrayList<>();
      for (Map.Entry<String, String> entry : description.getTags().entrySet()) {
        Map<String, Object> map = new HashMap<>();
        map.put("key", entry.getKey());
        map.put("value", entry.getValue());
        tags.add(map);
      }
      params.put("tags", tags);
    }
    request.setBodyParams(params);
    log.info("create ScalingGroup:" + JSONObject.toJSONString(params));
    EcloudResponse response = EcloudOpenApiHelper.execute(request);
    if (response.getErrorMessage() != null) {
      log.error("Create ScalingGroup Failed:" + response.getErrorMessage());
      return null;
    }
    Map body = (Map) response.getBody();
    return (String) body.get("scalingGroupId");
  }

  private boolean createScalingPolicies(EcloudDeployDescription description) {
    String sourceServerGroupName = description.getSource().getServerGroupName();
    String sourceRegion = description.getSource().getRegion();
    String accountName = description.getAccountName();
    Map<String, Object> attributes =
        ecloudClusterProvider.getAttributes(accountName, sourceRegion, sourceServerGroupName);
    // copy scaling rule
    Map newScalingRuleMap = new HashMap<>();
    Map<String, Map> sgRuleMap = (Map<String, Map>) attributes.get("scalingRules");
    for (Map.Entry<String, Map> entry : sgRuleMap.entrySet()) {
      Map sr = entry.getValue();
      // create scalingRule
      EcloudRequest request =
          new EcloudRequest(
              "POST",
              description.getRegion(),
              "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/scalingRule",
              description.getCredentials().getAccessKey(),
              description.getCredentials().getSecretKey());
      Map<String, Object> body = new HashMap<>();
      body.put("adjustmentType", sr.get("adjustmentType"));
      body.put("adjustmentValue", sr.get("adjustmentValue"));
      body.put("coolDown", sr.get("coolDown"));
      body.put("minAdjustmentValue", sr.get("minAdjustmentValue"));
      body.put("scalingRuleName", sr.get("scalingRuleName"));
      body.put("scalingRuleType", sr.get("scalingRuleType"));
      body.put("scalingGroupId", description.getScalingGroupId());
      request.setBodyParams(body);
      EcloudResponse rsp = EcloudOpenApiHelper.execute(request);
      if (rsp.getErrorMessage() != null) {
        log.error("Create Scaling Rule Failed:" + rsp.getErrorMessage());
        return false;
      }
      Map rspBody = (Map) rsp.getBody();
      if (rspBody == null || rspBody.get("scalingRuleId") == null) {
        log.error("Create Scaling Rule Return Empty Body!");
        return false;
      }
      newScalingRuleMap.put(
          (String) sr.get("scalingRuleId"), (String) rspBody.get("scalingRuleId"));
    }
    // copy alarm tasks
    List<Map> alarmTasks = (List<Map>) attributes.get("alarmTasks");
    for (Map task : alarmTasks) {
      EcloudRequest request =
          new EcloudRequest(
              "POST",
              description.getRegion(),
              "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/alarmTask",
              description.getCredentials().getAccessKey(),
              description.getCredentials().getSecretKey());
      Map<String, Object> body = new HashMap<>();
      body.put("alarmTaskName", task.get("alarmTaskName"));
      body.put("comparisonOperator", task.get("comparisonOperator"));
      body.put("evaluationCount", task.get("evaluationCount"));
      body.put("metricName", task.get("metricName"));
      body.put("monitorType", task.get("monitorType"));
      body.put("period", task.get("period"));
      body.put("statistics", task.get("statistics"));
      body.put("threshold", task.get("threshold"));
      body.put("description", task.get("description"));
      body.put("scalingRuleId", newScalingRuleMap.get(task.get("scalingRuleId")));
      body.put("scalingGroupId", description.getScalingGroupId());
      request.setBodyParams(body);
      EcloudResponse rsp = EcloudOpenApiHelper.execute(request);
      if (rsp.getErrorMessage() != null) {
        log.error("Create Alarm Task Failed:" + rsp.getErrorMessage());
        return false;
      }
    }
    // copy scheduled tasks
    List<Map> scheduledTasks = (List<Map>) attributes.get("scheduledTasks");
    for (Map task : scheduledTasks) {
      EcloudRequest request =
          new EcloudRequest(
              "POST",
              description.getRegion(),
              "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/scheduledTask",
              description.getCredentials().getAccessKey(),
              description.getCredentials().getSecretKey());
      Map<String, Object> body = new HashMap<>();
      body.put("taskType", task.get("taskType"));
      body.put("scheduledTaskName", task.get("scheduledTaskName"));
      body.put("triggerTime", task.get("triggerTime"));
      body.put("period", task.get("period"));
      body.put("periodValue", task.get("periodValue"));
      body.put("expireTime", task.get("expireTime"));
      body.put("description", task.get("description"));
      body.put("scalingRuleId", newScalingRuleMap.get(task.get("scalingRuleId")));
      body.put("scalingGroupId", description.getScalingGroupId());
      request.setBodyParams(body);
      EcloudResponse rsp = EcloudOpenApiHelper.execute(request);
      if (rsp.getErrorMessage() != null) {
        log.error("Create Scheduled Task Failed:" + rsp.getErrorMessage());
        return false;
      }
    }
    return true;
  }

  private boolean enableScalingGroup(EcloudDeployDescription description) {
    EcloudRequest enableRequest =
        new EcloudRequest(
            "PUT",
            description.getRegion(),
            "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/scalingGroup/"
                + description.getScalingGroupId(),
            description.getCredentials().getAccessKey(),
            description.getCredentials().getSecretKey());
    Map<String, String> query = new HashMap<>();
    query.put("action", "enable");
    enableRequest.setQueryParams(query);
    EcloudResponse enableRsp = EcloudOpenApiHelper.execute(enableRequest);
    if (enableRsp.getErrorMessage() != null) {
      log.error("Enable ScalingGroup Failed:" + enableRsp.getErrorMessage());
      return false;
    }
    return true;
  }

  private void destroyScalingGroup(EcloudDeployDescription description) {
    EcloudRequest request =
        new EcloudRequest(
            "DELETE",
            description.getRegion(),
            "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/scalingGroup/"
                + description.getScalingGroupId(),
            description.getCredentials().getAccessKey(),
            description.getCredentials().getSecretKey());
    request.setVersion("2016-12-05");
    EcloudResponse rsp = EcloudOpenApiHelper.execute(request);
    if (rsp.getErrorMessage() != null) {
      log.error("Delete ScalingGroup Failed:" + rsp.getErrorMessage());
    }
  }

  private void deleteScalingConfig(EcloudDeployDescription description) {
    EcloudRequest request =
        new EcloudRequest(
            "DELETE",
            description.getRegion(),
            "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/scalingConfig",
            description.getCredentials().getAccessKey(),
            description.getCredentials().getSecretKey());
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("scalingConfigIds", description.getScalingConfigId());
    request.setQueryParams(queryParams);
    EcloudResponse response = EcloudOpenApiHelper.execute(request);
    if (response.getErrorMessage() != null) {
      log.error("Delete ScalingConfig Failed:" + response.getErrorMessage());
    }
  }
}
