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
import com.aliyuncs.ess.model.v20140828.DescribeScalingGroupsResponse.ScalingGroup;
import com.aliyuncs.ess.model.v20140828.DescribeScalingInstancesResponse.ScalingInstance;
import com.aliyuncs.exceptions.ClientException;
import com.netflix.spinnaker.clouddriver.alicloud.common.ClientFactory;
import com.netflix.spinnaker.clouddriver.alicloud.deploy.description.DisableAliCloudServerGroupDescription;
import com.netflix.spinnaker.clouddriver.alicloud.exception.AliCloudException;
import com.netflix.spinnaker.clouddriver.alicloud.exception.ExceptionUtils;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum;
import groovy.util.logging.Slf4j;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

@Slf4j
public class DisableAliCloudServerGroupAtomicOperation implements AtomicOperation<Void> {

  private final Logger log =
      LoggerFactory.getLogger(DisableAliCloudServerGroupAtomicOperation.class);

  private final DisableAliCloudServerGroupDescription description;

  private final ClientFactory clientFactory;

  public DisableAliCloudServerGroupAtomicOperation(
      DisableAliCloudServerGroupDescription description, ClientFactory clientFactory) {
    this.description = description;
    this.clientFactory = clientFactory;
  }

  @Override
  public Void operate(List priorOutputs) {

    IAcsClient client =
        clientFactory.createClient(
            description.getRegion(),
            description.getCredentials().getAccessKeyId(),
            description.getCredentials().getAccessSecretKey());

    DescribeScalingGroupsRequest describeScalingGroupsRequest = new DescribeScalingGroupsRequest();
    describeScalingGroupsRequest.setScalingGroupName(description.getServerGroupName());
    describeScalingGroupsRequest.setPageSize(50);
    DescribeScalingGroupsResponse describeScalingGroupsResponse;
    try {
      describeScalingGroupsResponse = client.getAcsResponse(describeScalingGroupsRequest);
      if (describeScalingGroupsResponse.getTotalCount() > 3) {
        throw new AliCloudException(
            "Alicloud search interface, Disable "
                + describeScalingGroupsResponse.getTotalCount()
                + " Scaling Groups");
      }
      for (ScalingGroup scalingGroup : describeScalingGroupsResponse.getScalingGroups()) {
        if (!AliConditionMatchUtils.match(
            describeScalingGroupsRequest.getScalingGroupName(), scalingGroup)) {
          continue;
        }
        if ("Active".equals(scalingGroup.getLifecycleState())) {
          Integer maxSize = scalingGroup.getMaxSize();
          Integer minSize = scalingGroup.getMinSize();
          if (maxSize == 0 && minSize == 0) {
            // Number of query instances
            DescribeScalingInstancesRequest scalingInstancesRequest =
                new DescribeScalingInstancesRequest();
            scalingInstancesRequest.setScalingGroupId(scalingGroup.getScalingGroupId());
            scalingInstancesRequest.setScalingConfigurationId(
                scalingGroup.getActiveScalingConfigurationId());
            scalingInstancesRequest.setPageSize(50);
            DescribeScalingInstancesResponse scalingInstancesResponse =
                client.getAcsResponse(scalingInstancesRequest);
            List<ScalingInstance> scalingInstances = scalingInstancesResponse.getScalingInstances();
            if (scalingInstances.size() > 0) {
              // Remove instance
              List<String> instanceIds = new ArrayList<>();
              scalingInstances.forEach(
                  scalingInstance -> {
                    instanceIds.add(scalingInstance.getInstanceId());
                  });
              RemoveInstancesRequest removeInstancesRequest = new RemoveInstancesRequest();
              removeInstancesRequest.setInstanceIds(instanceIds);
              removeInstancesRequest.setScalingGroupId(scalingGroup.getScalingGroupId());
              client.getAcsResponse(removeInstancesRequest);
            }
          }

          String scalingGroupId = scalingGroup.getScalingGroupId();
          disableAlarmsTasks(scalingGroupId, client);
          for (int i = 1; i <= 10; i++) {
            try {
              DisableScalingGroupRequest disableScalingGroupRequest =
                  new DisableScalingGroupRequest();
              disableScalingGroupRequest.setScalingGroupId(scalingGroupId);
              client.getAcsResponse(disableScalingGroupRequest);
              break;
            } catch (ClientException e) {
              if ("IncorrectScalingGroupStatus".equals(e.getErrCode())) {
                Thread.sleep(60 * 1000L);
                log.error("disable scaling group failed,id:{}", scalingGroup.getScalingGroupId());
                if (i == 10) {
                  throw e;
                } else {
                  e.printStackTrace();
                }
              } else {
                throw e;
              }
            }
          }
        }
      }
    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1);
      log.info(e.getMessage());
      throw new AliCloudException(e.getMessage());
    }
    return null;
  }

  private void disableAlarmsTasks(String asgId, IAcsClient client) throws ClientException {
    DescribeAlarmsRequest describeAlarmsRequest = new DescribeAlarmsRequest();
    describeAlarmsRequest.setScalingGroupId(asgId);
    DescribeAlarmsResponse alarmsResponse = client.getAcsResponse(describeAlarmsRequest);
    List<DescribeAlarmsResponse.Alarm> alarmList = alarmsResponse.getAlarmList();
    if (!CollectionUtils.isEmpty(alarmList)) {
      DisableAlarmRequest disableAlarmRequest = new DisableAlarmRequest();
      for (DescribeAlarmsResponse.Alarm alarm : alarmList) {
        disableAlarmRequest.setAlarmTaskId(alarm.getAlarmTaskId());
        client.getAcsResponse(disableAlarmRequest);
      }
    }
  }
}
