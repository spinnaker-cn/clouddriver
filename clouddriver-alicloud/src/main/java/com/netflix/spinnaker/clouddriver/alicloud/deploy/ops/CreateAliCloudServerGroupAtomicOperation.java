/*
 * Copyright 2019 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.alicloud.deploy.ops;

import com.aliyuncs.IAcsClient;
import com.aliyuncs.ess.model.v20140828.*;
import com.aliyuncs.exceptions.ClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.clouddriver.alicloud.AliCloudProvider;
import com.netflix.spinnaker.clouddriver.alicloud.common.ClientFactory;
import com.netflix.spinnaker.clouddriver.alicloud.deploy.AliCloudServerGroupNameResolver;
import com.netflix.spinnaker.clouddriver.alicloud.deploy.description.BasicAliCloudDeployDescription;
import com.netflix.spinnaker.clouddriver.alicloud.exception.AliCloudException;
import com.netflix.spinnaker.clouddriver.alicloud.exception.ExceptionUtils;
import com.netflix.spinnaker.clouddriver.deploy.DeploymentResult;
import com.netflix.spinnaker.clouddriver.model.ClusterProvider;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum;
import groovy.util.logging.Slf4j;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

@Slf4j
public class CreateAliCloudServerGroupAtomicOperation implements AtomicOperation<DeploymentResult> {

  private final Logger log =
      LoggerFactory.getLogger(CreateAliCloudServerGroupAtomicOperation.class);

  private final List<ClusterProvider> clusterProviders;

  private final ObjectMapper objectMapper;

  private final BasicAliCloudDeployDescription description;

  private final ClientFactory clientFactory;

  private final List<String> envs = Arrays.asList("base", "cana", "gray");

  public CreateAliCloudServerGroupAtomicOperation(
      BasicAliCloudDeployDescription description,
      ObjectMapper objectMapper,
      ClientFactory clientFactory,
      List<ClusterProvider> clusterProviders) {
    this.description = description;
    this.objectMapper = objectMapper;
    this.clientFactory = clientFactory;
    this.clusterProviders = clusterProviders;
  }

  @Override
  public DeploymentResult operate(List priorOutputs) {
    DeploymentResult result = new DeploymentResult();
    // create scaling group
    IAcsClient client =
        clientFactory.createClient(
            description.getRegion(),
            description.getCredentials().getAccessKeyId(),
            description.getCredentials().getAccessSecretKey());
    AliCloudServerGroupNameResolver resolver =
        new AliCloudServerGroupNameResolver(
            description.getCredentials().getName(), description.getRegion(), clusterProviders);
    String serverGroupName =
        resolver.resolveNextServerGroupName(
            description.getApplication(),
            description.getStack(),
            description.getFreeFormDetails(),
            false);
    description.setScalingGroupName(serverGroupName);
    CreateScalingGroupRequest createScalingGroupRequest =
        objectMapper.convertValue(description, CreateScalingGroupRequest.class);
    rebuildCreateScalingGroupRequest(description, createScalingGroupRequest);
    createScalingGroupRequest.setScalingGroupName(serverGroupName);
    if (!StringUtils.isEmpty(description.getVSwitchId())) {
      createScalingGroupRequest.setVSwitchId(description.getVSwitchId());
    }
    if (description.getVSwitchIds() != null) {
      createScalingGroupRequest.setVSwitchIds(description.getVSwitchIds());
    }
    CreateScalingGroupResponse createScalingGroupResponse;
    try {
      createScalingGroupResponse = client.getAcsResponse(createScalingGroupRequest);
      description.setScalingGroupId(createScalingGroupResponse.getScalingGroupId());
    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1);
      log.info(e.getMessage());
      throw new AliCloudException(e.getMessage());
    }

    if (StringUtils.isEmpty(description.getScalingGroupId())) {
      return result;
    }

    String scalingConfigurationId = null;

    // create scaling configuration
    for (CreateScalingConfigurationRequest scalingConfiguration :
        description.getScalingConfigurations()) {
      CreateScalingConfigurationRequest configurationRequest =
          objectMapper.convertValue(scalingConfiguration, CreateScalingConfigurationRequest.class);
      configurationRequest.setScalingGroupId(description.getScalingGroupId());
      CreateScalingConfigurationResponse configurationResponse;
      try {
        configurationResponse = client.getAcsResponse(configurationRequest);
        scalingConfigurationId = configurationResponse.getScalingConfigurationId();
      } catch (Exception e) {
        ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1);
        log.info(e.getMessage());
        throw new AliCloudException(e.getMessage());
      }
    }

    if (StringUtils.isEmpty(scalingConfigurationId)) {
      return result;
    }

    EnableScalingGroupRequest enableScalingGroupRequest = new EnableScalingGroupRequest();
    enableScalingGroupRequest.setScalingGroupId(description.getScalingGroupId());
    enableScalingGroupRequest.setActiveScalingConfigurationId(scalingConfigurationId);
    EnableScalingGroupResponse enableScalingGroupResponse;
    try {
      enableScalingGroupResponse = client.getAcsResponse(enableScalingGroupRequest);
    } catch (ClientException e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1);
      log.error(e.getMessage());
      throw new AliCloudException(e.getMessage());
    }

    this.copySourceServerGroupRelatedResource(description);

    buildResult(description, result);

    return result;
  }

  private void rebuildCreateScalingGroupRequest(
      BasicAliCloudDeployDescription description, CreateScalingGroupRequest request) {
    if (description != null
        && description.getSource() != null
        && description.getSource().getUseSourceCapacity() != null
        && description.getSource().getUseSourceCapacity()
        && StringUtils.isNotEmpty(description.getSource().getAsgName())) {

      String asgName = description.getSource().getAsgName();
      DescribeScalingGroupsRequest describeScalingGroupsRequest =
          new DescribeScalingGroupsRequest();
      describeScalingGroupsRequest.setScalingGroupName(asgName);
      DescribeScalingGroupsResponse describeScalingGroupsResponse;
      try {
        IAcsClient client =
            clientFactory.createClient(
                description.getRegion(),
                description.getCredentials().getAccessKeyId(),
                description.getCredentials().getAccessSecretKey());

        describeScalingGroupsResponse = client.getAcsResponse(describeScalingGroupsRequest);
        List<DescribeScalingGroupsResponse.ScalingGroup> scalingGroups =
            describeScalingGroupsResponse.getScalingGroups();
        if (CollectionUtils.isEmpty(scalingGroups)) {
          throw new AliCloudException("Old server group is does not exist");
        }
        scalingGroups =
            scalingGroups.stream()
                .filter(
                    scalingGroup ->
                        AliConditionMatchUtils.match(
                            describeScalingGroupsRequest.getScalingGroupName(), scalingGroup))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(scalingGroups)) {
          throw new AliCloudException("Old server group is does not exist");
        }
        DescribeScalingGroupsResponse.ScalingGroup scalingGroup = scalingGroups.get(0);
        if (scalingGroup.getMaxSize() != null) {
          request.setMaxSize(scalingGroup.getMaxSize());
        }
        if (scalingGroup.getMinSize() != null) {
          request.setMinSize(scalingGroup.getMinSize());
        }
        request.setDesiredCapacity(
            scalingGroup.getDesiredCapacity() != null
                ? scalingGroup.getDesiredCapacity()
                : scalingGroup.getMinSize());
      } catch (Exception e) {
        ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1);
        log.info(e.getMessage());
        throw new AliCloudException(e.getMessage());
      }
    }
  }

  private void buildResult(BasicAliCloudDeployDescription description, DeploymentResult result) {

    List<String> serverGroupNames = new ArrayList<>();
    serverGroupNames.add(description.getRegion() + ":" + description.getScalingGroupName());
    result.setServerGroupNames(serverGroupNames);

    Map<String, String> serverGroupNameByRegion = new HashMap<>();
    serverGroupNameByRegion.put(description.getRegion(), description.getScalingGroupName());
    result.setServerGroupNameByRegion(serverGroupNameByRegion);

    Set<DeploymentResult.Deployment> deployments = new HashSet<>();

    DeploymentResult.Deployment.Capacity capacity = new DeploymentResult.Deployment.Capacity();
    capacity.setMax(description.getMaxSize());
    capacity.setMin(description.getMinSize());
    capacity.setDesired(description.getMinSize());

    DeploymentResult.Deployment deployment = new DeploymentResult.Deployment();
    deployment.setCloudProvider(AliCloudProvider.ID);
    deployment.setAccount(description.getCredentials().getName());
    deployment.setCapacity(capacity);
    deployment.setLocation(description.getRegion());
    deployment.setServerGroupName(description.getScalingGroupName());

    deployments.add(deployment);
    result.setDeployments(deployments);
  }

  private void copySourceServerGroupRelatedResource(BasicAliCloudDeployDescription description) {
    String sourceScalingGroupId = "";
    String destScalingGroupId = description.getScalingGroupId();
    String region = description.getRegion();

    String asgName = description.getSource().getAsgName();

    DescribeScalingGroupsRequest describeScalingGroupsRequest = new DescribeScalingGroupsRequest();
    describeScalingGroupsRequest.setScalingGroupName(asgName);
    DescribeScalingGroupsResponse describeScalingGroupsResponse;
    try {
      IAcsClient client =
          clientFactory.createClient(
              description.getRegion(),
              description.getCredentials().getAccessKeyId(),
              description.getCredentials().getAccessSecretKey());

      describeScalingGroupsResponse = client.getAcsResponse(describeScalingGroupsRequest);
      if (describeScalingGroupsResponse.getScalingGroups().size() == 0) {
        log.info("Old server group is does not exist");
        return;
      }

      DescribeScalingGroupsResponse.ScalingGroup scalingGroup =
          describeScalingGroupsResponse.getScalingGroups().get(0);
      sourceScalingGroupId = scalingGroup.getScalingGroupId();

      if (StringUtils.isEmpty(sourceScalingGroupId) || StringUtils.isEmpty(destScalingGroupId)) {
        return;
      }

      int scheduledTaskPageSize = 50;
      int scheduledTaskPageNumber = 1;
      DescribeScheduledTasksRequest describeTaskRequest = new DescribeScheduledTasksRequest();
      describeTaskRequest.setSysRegionId(region);
      describeTaskRequest.setPageSize(scheduledTaskPageSize);
      describeTaskRequest.setPageNumber(scheduledTaskPageNumber);
      describeTaskRequest.putQueryParameter("ScalingGroupId", sourceScalingGroupId);
      DescribeScheduledTasksResponse describeTasksResponse =
          client.getAcsResponse(describeTaskRequest);
      while (describeTasksResponse != null
          && !CollectionUtils.isEmpty(describeTasksResponse.getScheduledTasks())) {
        for (DescribeScheduledTasksResponse.ScheduledTask scheduledTask :
            describeTasksResponse.getScheduledTasks()) {
          if (sourceScalingGroupId.equals(scheduledTask.getScalingGroupId())
              && StringUtils.isBlank(scheduledTask.getScheduledAction())) {
            CreateScheduledTaskRequest createScheduledTaskRequest =
                objectMapper.convertValue(scheduledTask, CreateScheduledTaskRequest.class);
            createScheduledTaskRequest.setScalingGroupId(destScalingGroupId);
            String launchTime = createScheduledTaskRequest.getLaunchTime();
            if (launchTime != null) {
              createScheduledTaskRequest.setLaunchTime(getNewLaunchTime(launchTime));
            }
            createScheduledTaskRequest.setSysRegionId(region);
            createScheduledTaskRequest.setScheduledTaskName("");
            client.getAcsResponse(createScheduledTaskRequest);
          }
        }

        scheduledTaskPageNumber = scheduledTaskPageNumber + 1;
        describeTaskRequest.setPageNumber(scheduledTaskPageNumber);
        describeTasksResponse = client.getAcsResponse(describeTaskRequest);
      }

      int pageSize = 50;
      int pageNumber = 1;
      DescribeScalingRulesRequest describeScalingRulesRequest = new DescribeScalingRulesRequest();
      describeScalingRulesRequest.setSysRegionId(region);
      describeScalingRulesRequest.setScalingGroupId(sourceScalingGroupId);
      describeScalingRulesRequest.setPageSize(pageSize);
      describeScalingRulesRequest.setPageNumber(pageNumber);

      DescribeScalingRulesResponse describeScalingRulesResponse =
          client.getAcsResponse(describeScalingRulesRequest);
      Map<String, String> scalingRuleAriMap = new HashMap<>();
      while (describeScalingRulesResponse != null
          && !CollectionUtils.isEmpty(describeScalingRulesResponse.getScalingRules())) {
        for (DescribeScalingRulesResponse.ScalingRule scalingRule :
            describeScalingRulesResponse.getScalingRules()) {
          // scaling rule
          CreateScalingRuleRequest createScalingRuleRequest =
              objectMapper.convertValue(scalingRule, CreateScalingRuleRequest.class);
          createScalingRuleRequest.setScalingGroupId(destScalingGroupId);
          createScalingRuleRequest.setScalingRuleName("");
          CreateScalingRuleResponse createScalingRuleResponse =
              client.getAcsResponse(createScalingRuleRequest);
          if (createScalingRuleResponse != null
              && StringUtils.isNotEmpty(createScalingRuleResponse.getScalingRuleAri())) {
            scalingRuleAriMap.put(
                scalingRule.getScalingRuleAri(), createScalingRuleResponse.getScalingRuleAri());
            // scheduled task
            DescribeScheduledTasksRequest describeScheduledTasksRequest =
                new DescribeScheduledTasksRequest();
            describeScheduledTasksRequest.setSysRegionId(region);
            describeScheduledTasksRequest.setScheduledAction1(scalingRule.getScalingRuleAri());
            describeScheduledTasksRequest.setPageSize(pageSize);
            int taskPageNumber = 1;
            describeScheduledTasksRequest.setPageNumber(taskPageNumber);
            DescribeScheduledTasksResponse describeScheduledTasksResponse =
                client.getAcsResponse(describeScheduledTasksRequest);

            while (describeScheduledTasksResponse != null
                && !CollectionUtils.isEmpty(describeScheduledTasksResponse.getScheduledTasks())) {
              for (DescribeScheduledTasksResponse.ScheduledTask scheduledTask :
                  describeScheduledTasksResponse.getScheduledTasks()) {

                if (StringUtils.isNotEmpty(scheduledTask.getScheduledAction())) {
                  scheduledTask.setScheduledAction(createScalingRuleResponse.getScalingRuleAri());
                  scheduledTask.setScalingGroupId(null);
                  scheduledTask.setMaxValue(null);
                  scheduledTask.setMinValue(null);
                  scheduledTask.setDesiredCapacity(null);
                }

                CreateScheduledTaskRequest createScheduledTaskRequest =
                    objectMapper.convertValue(scheduledTask, CreateScheduledTaskRequest.class);

                String launchTime = createScheduledTaskRequest.getLaunchTime();
                if (launchTime != null) {
                  createScheduledTaskRequest.setLaunchTime(getNewLaunchTime(launchTime));
                }
                createScheduledTaskRequest.setSysRegionId(region);
                createScheduledTaskRequest.setScheduledTaskName("");
                CreateScheduledTaskResponse createScheduledTaskResponse =
                    client.getAcsResponse(createScheduledTaskRequest);
              }

              taskPageNumber = taskPageNumber + 1;
              describeScheduledTasksRequest.setPageNumber(taskPageNumber);
              describeScheduledTasksResponse = client.getAcsResponse(describeScheduledTasksRequest);
            }
          }
        }
        pageNumber = pageNumber + 1;
        describeScalingRulesRequest.setPageNumber(pageNumber);
        describeScalingRulesResponse = client.getAcsResponse(describeScalingRulesRequest);
      }

      // notification configuration
      DescribeNotificationConfigurationsRequest describeNotificationConfigurationsRequest =
          new DescribeNotificationConfigurationsRequest();
      describeNotificationConfigurationsRequest.setScalingGroupId(sourceScalingGroupId);
      DescribeNotificationConfigurationsResponse describeNotificationConfigurationsResponse =
          client.getAcsResponse(describeNotificationConfigurationsRequest);
      if (describeNotificationConfigurationsResponse != null
          && !CollectionUtils.isEmpty(
              describeNotificationConfigurationsResponse.getNotificationConfigurationModels())) {
        for (DescribeNotificationConfigurationsResponse.NotificationConfigurationModel
            notificationConfigurationModel :
                describeNotificationConfigurationsResponse.getNotificationConfigurationModels()) {
          CreateNotificationConfigurationRequest createNotificationConfigurationRequest =
              objectMapper.convertValue(
                  notificationConfigurationModel, CreateNotificationConfigurationRequest.class);
          createNotificationConfigurationRequest.setScalingGroupId(destScalingGroupId);
          CreateNotificationConfigurationResponse createNotificationConfigurationResponse =
              client.getAcsResponse(createNotificationConfigurationRequest);
        }
      }

      // alarm
      DescribeAlarmsRequest describeAlarmsRequest = new DescribeAlarmsRequest();
      describeAlarmsRequest.setSysRegionId(region);
      describeAlarmsRequest.setScalingGroupId(sourceScalingGroupId);
      describeAlarmsRequest.setPageSize(pageSize);
      int alarmPageNumber = 1;
      describeAlarmsRequest.setPageNumber(alarmPageNumber);
      DescribeAlarmsResponse describeAlarmsResponse = client.getAcsResponse(describeAlarmsRequest);

      while (describeAlarmsResponse != null
          && !CollectionUtils.isEmpty(describeAlarmsResponse.getAlarmList())) {
        for (DescribeAlarmsResponse.Alarm alarm : describeAlarmsResponse.getAlarmList()) {
          CreateAlarmRequest createAlarmRequest =
              objectMapper.convertValue(alarm, CreateAlarmRequest.class);
          createAlarmRequest.setSysRegionId(region);
          createAlarmRequest.setScalingGroupId(destScalingGroupId);
          if (!CollectionUtils.isEmpty(createAlarmRequest.getDimensions())) {
            for (CreateAlarmRequest.Dimension dimension : createAlarmRequest.getDimensions()) {
              if (sourceScalingGroupId.equals(dimension.getDimensionValue())) {
                dimension.setDimensionValue(destScalingGroupId);
              }
            }
          }
          createAlarmRequest.setDimensions(createAlarmRequest.getDimensions());
          if (!CollectionUtils.isEmpty(createAlarmRequest.getAlarmActions())) {
            List<String> alarmActions = new ArrayList<>();
            for (String alarmAction : createAlarmRequest.getAlarmActions()) {
              if (StringUtils.isNotEmpty(scalingRuleAriMap.get(alarmAction))) {
                alarmActions.add(scalingRuleAriMap.get(alarmAction));
              } else {
                alarmActions.add(alarmAction);
              }
            }
            createAlarmRequest.setAlarmActions(alarmActions);
            CreateAlarmResponse createAlarmResponse = client.getAcsResponse(createAlarmRequest);
            if (createAlarmResponse != null
                && StringUtils.isNotEmpty(createAlarmResponse.getAlarmTaskId())
                && alarm.getEnable() != null
                && alarm.getEnable()) {
              EnableAlarmRequest enableAlarmRequest = new EnableAlarmRequest();
              enableAlarmRequest.setSysRegionId(region);
              enableAlarmRequest.setAlarmTaskId(createAlarmResponse.getAlarmTaskId());
              EnableAlarmResponse enableAlarmResponse = client.getAcsResponse(enableAlarmRequest);
            }
          }
        }
        alarmPageNumber = alarmPageNumber + 1;
        describeAlarmsRequest.setPageNumber(alarmPageNumber);
        describeAlarmsResponse = client.getAcsResponse(describeAlarmsRequest);
      }

      // copy lifecycle hooks from prod
      String scalingGroupName = description.getScalingGroupName();
      for (String env : envs) {
        if (scalingGroupName.toLowerCase().contains(env.toLowerCase())) {
          scalingGroupName =
              scalingGroupName.substring(0, scalingGroupName.lastIndexOf("-")).replace(env, "prod");
          DescribeScalingGroupsRequest request = new DescribeScalingGroupsRequest();
          request.setScalingGroupName(scalingGroupName);
          DescribeScalingGroupsResponse response = client.getAcsResponse(request);
          if (response == null || CollectionUtils.isEmpty(response.getScalingGroups())) {
            log.info("Old server group is does not exist");
            return;
          }
          sourceScalingGroupId = response.getScalingGroups().get(0).getScalingGroupId();
        }
      }

      // lifecycle hooks
      DescribeLifecycleHooksRequest describeLifecycleHooksRequest =
          new DescribeLifecycleHooksRequest();
      describeLifecycleHooksRequest.setScalingGroupId(sourceScalingGroupId);
      describeLifecycleHooksRequest.setPageSize(pageSize);
      int hooksPageNumber = 1;
      describeLifecycleHooksRequest.setPageNumber(hooksPageNumber);
      DescribeLifecycleHooksResponse describeLifecycleHooksResponse =
          client.getAcsResponse(describeLifecycleHooksRequest);

      while (describeLifecycleHooksResponse != null
          && !CollectionUtils.isEmpty(describeLifecycleHooksResponse.getLifecycleHooks())) {
        for (DescribeLifecycleHooksResponse.LifecycleHook lifecycleHook :
            describeLifecycleHooksResponse.getLifecycleHooks()) {
          CreateLifecycleHookRequest createLifecycleHookRequest = new CreateLifecycleHookRequest();
          createLifecycleHookRequest.setScalingGroupId(lifecycleHook.getScalingGroupId());
          createLifecycleHookRequest.setLifecycleHookName(lifecycleHook.getLifecycleHookName());
          createLifecycleHookRequest.setDefaultResult(lifecycleHook.getDefaultResult());
          createLifecycleHookRequest.setHeartbeatTimeout(lifecycleHook.getHeartbeatTimeout());
          createLifecycleHookRequest.setLifecycleTransition(lifecycleHook.getLifecycleTransition());
          //          CreateLifecycleHookRequest createLifecycleHookRequest =
          //              objectMapper.convertValue(lifecycleHook,
          // CreateLifecycleHookRequest.class);
          createLifecycleHookRequest.setScalingGroupId(destScalingGroupId);
          CreateLifecycleHookResponse createLifecycleHookResponse =
              client.getAcsResponse(createLifecycleHookRequest);
        }
        hooksPageNumber = hooksPageNumber + 1;
        describeLifecycleHooksRequest.setPageNumber(hooksPageNumber);
        describeLifecycleHooksResponse = client.getAcsResponse(describeLifecycleHooksRequest);
      }

    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1);
      log.error(e.getMessage());
      throw new AliCloudException(e.getMessage());
    }
  }

  private String getNewLaunchTime(String launchTime) throws ParseException {
    log.info("fix_launchTime_origin is " launchTime);
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    Date launchTimeDate = df.parse(launchTime);

    Calendar nowTimeCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    Calendar onlineCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    onlineCalendar.setTime(launchTimeDate);

    log.info("fix_nowTimeCalendar is " nowTimeCalendar.getTime());
    log.info("fix_onlineCalendar is " onlineCalendar.getTime());

    if (onlineCalendar.before(nowTimeCalendar)) {
      nowTimeCalendar.set(Calendar.MINUTE, onlineCalendar.get(Calendar.MINUTE));
      nowTimeCalendar.set(Calendar.HOUR_OF_DAY, onlineCalendar.get(Calendar.HOUR_OF_DAY));
      nowTimeCalendar.add(Calendar.DATE, 1);
      return df.format(nowTimeCalendar.getTime());
    }
    return launchTime;
  }
}
