package com.netflix.spinnaker.clouddriver.ecloud.deploy.ops;

import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.ecloud.client.openapi.EcloudOpenApiHelper;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.UpsertEcloudScalingPolicyDescription;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudRequest;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudResponse;
import com.netflix.spinnaker.clouddriver.ecloud.provider.view.EcloudClusterProvider;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

@Slf4j
public class UpsertEcloudScalingPolicyAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "RESIZE_SERVER_GROUP";

  @Autowired EcloudClusterProvider ecloudClusterProvider;

  private final UpsertEcloudScalingPolicyDescription description;

  public UpsertEcloudScalingPolicyAtomicOperation(
      UpsertEcloudScalingPolicyDescription description) {
    this.description = description;
  }

  @Override
  public Void operate(List priorOutputs) {
    getTask()
        .updateStatus(
            BASE_PHASE,
            "Initializing Upsert of scaling policy of "
                + description.getServerGroupName()
                + " in "
                + description.getRegion());
    String accountName = description.getAccountName();
    String region = description.getRegion();
    String serverGroupName = description.getServerGroupName();
    String scalingGroupId =
        ecloudClusterProvider.getServerGroupIdByName(accountName, region, serverGroupName);
    if (scalingGroupId != null) {
      EcloudRequest request =
          new EcloudRequest(
              "POST",
              description.getRegion(),
              "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/scalingRule",
              description.getCredentials().getAccessKey(),
              description.getCredentials().getSecretKey());
      Map<String, Object> body = new HashMap<>();
      body.put("scalingGroupId", scalingGroupId);
      body.put("scalingRuleType", "SIMPLE_SCALING_RULE");
      body.put("scalingRuleName", description.getPolicyName());
      body.put("adjustmentType", description.getAdjustmentType());
      body.put("adjustmentValue", description.getAdjustmentValue());
      body.put("minAdjustmentValue", description.getMinAdjustmentValue());
      body.put("coolDown", description.getCooldown());
      request.setBodyParams(body);
      request.setVersion("2016-12-05");
      EcloudResponse rsp = EcloudOpenApiHelper.execute(request);
      if (!StringUtils.isEmpty(rsp.getErrorMessage())) {
        log.error("UpsertEcloudScalingPolicy Failed:" + rsp.getErrorMessage());
      }
      getTask()
          .updateStatus(
              BASE_PHASE,
              "Complete  Upsert of scaling policy of "
                  + description.getServerGroupName()
                  + " in "
                  + description.getRegion());
    } else {
      log.error("ServerGroup Not Found:" + description.getServerGroupName());
    }

    return null;
  }

  private static Task getTask() {
    return TaskRepository.threadLocalTask.get();
  }
}
