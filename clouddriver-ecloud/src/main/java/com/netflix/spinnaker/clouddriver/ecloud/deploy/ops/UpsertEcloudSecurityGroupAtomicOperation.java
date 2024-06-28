package com.netflix.spinnaker.clouddriver.ecloud.deploy.ops;

import com.ecloud.sdk.vpc.v1.model.ListSecurityGroupRuleResponseContent;
import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.ecloud.client.EcloudVirtualPrivateCloudClient;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.UpsertEcloudSecurityGroupDescription;
import com.netflix.spinnaker.clouddriver.ecloud.exception.ExceptionUtils;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudSecurityGroupRule;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpsertEcloudSecurityGroupAtomicOperation implements AtomicOperation<Map> {

  private static final String BASE_PHASE = "UPSERT_SECURITY_GROUP";

  private UpsertEcloudSecurityGroupDescription description;

  public UpsertEcloudSecurityGroupAtomicOperation(
      UpsertEcloudSecurityGroupDescription description) {
    this.description = description;
  }

  @Override
  public Map operate(List priorOutputs) {
    Task task = TaskRepository.threadLocalTask.get();
    task.updateStatus(
        BASE_PHASE,
        String.format(
            "Initializing upsert of Tencent securityGroup %s in %s...",
            description.getSecurityGroupName(), description.getRegion()));
    log.info(String.format("params = %s", description));

    try {
      String securityGroupId = description.getSecurityGroupId();
      if (securityGroupId != null && securityGroupId.length() > 0) {
        updateSecurityGroup(description);
      } else {
        insertSecurityGroup(description);
      }
    } catch (Exception e) {

      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.getClass());
      throw e;
    }
    log.info(
        String.format(
            "upsert securityGroup name:%s, id:%s",
            description.getSecurityGroupName(), description.getSecurityGroupId()));

    Map<String, Map<String, String>> result = new HashMap<>();
    Map<String, String> securityGroups = new HashMap<>();
    securityGroups.put("name", description.getSecurityGroupName());
    securityGroups.put("id", description.getSecurityGroupId());
    result.put(description.getRegion(), securityGroups);
    HashMap<String, Object> map = new HashMap<>();
    map.put("securityGroups", result);
    return map;
  }

  private void updateSecurityGroup(UpsertEcloudSecurityGroupDescription description) {
    Task task = TaskRepository.threadLocalTask.get();
    task.updateStatus(
        BASE_PHASE,
        String.format(
            "Start update securityGroup %s %s ...",
            description.getSecurityGroupName(), description.getSecurityGroupId()));
    String securityGroupId = description.getSecurityGroupId();
    EcloudVirtualPrivateCloudClient vpcClient =
        new EcloudVirtualPrivateCloudClient(description.getCredentials(), description.getRegion());
    final List<ListSecurityGroupRuleResponseContent> oldGroupRules =
        vpcClient.getSecurityGroupPolicies(securityGroupId).getBody().getContent();
    List<EcloudSecurityGroupRule> newGroupInRules = description.getInRules();
    // del in rules
    List<EcloudSecurityGroupRule> delGroupInRules = new ArrayList<>();
    if (oldGroupRules != null) {
      for (ListSecurityGroupRuleResponseContent ingress : oldGroupRules) {
        EcloudSecurityGroupRule keepRule =
            newGroupInRules.stream()
                .filter(r -> r.getId().equals(ingress.getId()))
                .findFirst()
                .orElse(null);
        if (keepRule == null) {
          delGroupInRules.add(new EcloudSecurityGroupRule(ingress.getId()));
        }
      }
    }
    if (!delGroupInRules.isEmpty()) {
      task.updateStatus(
          BASE_PHASE, String.format("Start delete securityGroup %s rules ...", securityGroupId));
      vpcClient.deleteSecurityGroupInRules(delGroupInRules);
      task.updateStatus(
          BASE_PHASE, String.format("delete securityGroup %s rules end", securityGroupId));
    }

    // add in rules
    List<EcloudSecurityGroupRule> addGroupInRules =
        description.getInRules().stream()
            .filter(r -> r.getId() == null)
            .collect(Collectors.toList());

    if (!addGroupInRules.isEmpty()) {
      task.updateStatus(
          BASE_PHASE, String.format("Start add securityGroup %s rules ...", securityGroupId));
      vpcClient.createSecurityGroupRules(securityGroupId, addGroupInRules, null);
      task.updateStatus(
          BASE_PHASE, String.format("add securityGroup %s rules end", securityGroupId));
    }

    task.updateStatus(
        BASE_PHASE,
        String.format(
            "Update securityGroup %s %s end",
            description.getSecurityGroupName(), description.getSecurityGroupId()));
  }

  private void insertSecurityGroup(UpsertEcloudSecurityGroupDescription description) {
    Task task = TaskRepository.threadLocalTask.get();
    task.updateStatus(
        BASE_PHASE,
        String.format("Start create new securityGroup %s ...", description.getSecurityGroupName()));
    EcloudVirtualPrivateCloudClient vpcClient =
        new EcloudVirtualPrivateCloudClient(description.getCredentials(), description.getRegion());
    String securityGroupId = vpcClient.createSecurityGroup(description);
    description.setSecurityGroupId(securityGroupId);
    task.updateStatus(
        BASE_PHASE,
        String.format(
            "Create new securityGroup %s success, id is %s.",
            description.getSecurityGroupName(), securityGroupId));
    if (!description.getInRules().isEmpty()) {
      task.updateStatus(
          BASE_PHASE,
          String.format("Start create new securityGroup rules in %s ...", securityGroupId));
      vpcClient.createSecurityGroupRules(
          securityGroupId, description.getInRules(), description.getOutRules());
      task.updateStatus(
          BASE_PHASE, String.format("Create new securityGroup rules in %s end", securityGroupId));
    }
  }
}
