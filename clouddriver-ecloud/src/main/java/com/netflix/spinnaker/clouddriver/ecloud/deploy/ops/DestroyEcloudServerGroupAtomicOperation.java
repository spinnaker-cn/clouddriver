package com.netflix.spinnaker.clouddriver.ecloud.deploy.ops;

import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.ecloud.client.openapi.EcloudOpenApiHelper;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.DestroyEcloudServerGroupDescription;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudRequest;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudResponse;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudServerGroup;
import com.netflix.spinnaker.clouddriver.ecloud.provider.view.EcloudClusterProvider;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * @author xu.dangling
 * @date 2024/4/11
 * @Description Destroy Ecloud Scaling Group
 */
@Slf4j
public class DestroyEcloudServerGroupAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "DESTROY_SERVER_GROUP";

  @Autowired private EcloudClusterProvider ecloudClusterProvider;

  private DestroyEcloudServerGroupDescription description;

  public DestroyEcloudServerGroupAtomicOperation(DestroyEcloudServerGroupDescription description) {
    this.description = description;
  }

  @Override
  public Void operate(List priorOutputs) {
    StringBuffer status = new StringBuffer();
    status
        .append("Destroying serverGroups ")
        .append(description.getServerGroupName())
        .append(" in region ")
        .append(description.getRegion())
        .append("...");
    getTask().updateStatus(BASE_PHASE, status.toString());
    EcloudServerGroup serverGroup =
        ecloudClusterProvider.getServerGroup(
            description.getAccount(), description.getRegion(), description.getServerGroupName());
    String scalingGroupId = serverGroup.getScalingGroupId();
    if (scalingGroupId != null) {
      EcloudRequest request =
          new EcloudRequest(
              "DELETE",
              description.getRegion(),
              "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/scalingGroup/" + scalingGroupId,
              description.getCredentials().getAccessKey(),
              description.getCredentials().getSecretKey());
      request.setVersion("2016-12-05");
      EcloudResponse rsp = EcloudOpenApiHelper.execute(request);
      if (!StringUtils.isEmpty(rsp.getErrorMessage())) {
        getTask().updateStatus(BASE_PHASE, "DestroyServerGroup Failed:" + rsp.getErrorMessage());
        getTask().fail(false);
        return null;
      }
      try {
        String scalingConfigId = (String) serverGroup.getLaunchConfig().get("lauchConfigurationId");
        if (rsp.isHttpSuccess()) {
          EcloudRequest delRequest =
              new EcloudRequest(
                  "DELETE",
                  description.getRegion(),
                  "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/scalingConfig",
                  description.getCredentials().getAccessKey(),
                  description.getCredentials().getSecretKey());
          Map<String, String> queryParams = new HashMap<>();
          queryParams.put("scalingConfigIds", scalingConfigId);
          delRequest.setQueryParams(queryParams);
          // Wait till the scaling group is destroyed
          Thread.sleep(30000);
          EcloudResponse response = EcloudOpenApiHelper.execute(delRequest);
          if (!StringUtils.isEmpty(response.getErrorMessage())) {
            // scalingConfig may be binded by another serverGroup or the serverGroup has not be
            // fully destroyed yet
            log.error(
                "Failed to delete ScalingConfig :"
                    + response.getErrorMessage()
                    + ", ignore the error");
          }
        }
      } catch (Exception e) {
        log.error("DestroyScalingConfig Failed, Ignore the exception", e);
      }
      status = new StringBuffer();
      status
          .append("ServerGroups ")
          .append(description.getServerGroupName())
          .append(" in region ")
          .append(description.getRegion())
          .append(" was destroyed.");
      getTask().updateStatus(BASE_PHASE, status.toString());
    } else {
      getTask()
          .updateStatus(BASE_PHASE, "ServerGroup Not Found:" + description.getServerGroupName());
      getTask().fail(false);
    }
    return null;
  }

  static Task getTask() {
    return TaskRepository.threadLocalTask.get();
  }
}
