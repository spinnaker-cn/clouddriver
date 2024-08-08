package com.netflix.spinnaker.clouddriver.ecloud.deploy.ops;

import com.alibaba.fastjson2.JSONObject;
import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.ecloud.client.openapi.EcloudOpenApiHelper;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.DestroyEcloudServerGroupDescription;
import com.netflix.spinnaker.clouddriver.ecloud.exception.EcloudException;
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
 * @Description Destroy Ecloud Scaling Group
 * @date 2024/4/11
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
        log.error("Destroy scalingGroup failed with response:" + JSONObject.toJSONString(rsp));
        StringBuffer msg = new StringBuffer();
        msg.append("DestroyServerGroup Failed:").append(rsp.getErrorMessage()).append("(").append(rsp.getRequestId()).append(")");
        getTask().updateStatus(BASE_PHASE,  msg.toString());
        getTask().fail(false);
        return null;
      }
      // check the serverGroup till it's been actually destroyed, expire after 30 minutes
      Long start = System.currentTimeMillis();
      while (System.currentTimeMillis() - start < 30 * 60 * 1000) {
        try {
          Thread.sleep(120000);
        } catch (InterruptedException e) {
          log.error(e.getMessage(), e);
        }
        EcloudRequest checkRequest =
            new EcloudRequest(
                "GET",
                description.getRegion(),
                "/api/v4/autoScaling/scalingGroup/" + scalingGroupId,
                description.getCredentials().getAccessKey(),
                description.getCredentials().getSecretKey());
        EcloudResponse checkResponse = EcloudOpenApiHelper.execute(checkRequest);
        if ("CSLOPENSTACK_AUTOSCALING_SCALING_GROUP_NOT_EXIST"
            .equals(checkResponse.getErrorCode())) {
          break;
        } else if (!StringUtils.isEmpty(rsp.getErrorMessage())) {
          log.error("Check scalingGroup failed with response:" + JSONObject.toJSONString(rsp));
        } else {
          log.info(
              "ScalingGroup:{} may not be destroyed, check again after 120s...", scalingGroupId);
        }
      }
      String scalingConfigId = (String) serverGroup.getLaunchConfig().get("lauchConfigurationId");
      if (scalingConfigId == null) {
        throw new EcloudException("ScalingConfigId Not Found");
      }
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
      EcloudResponse delRsp = EcloudOpenApiHelper.execute(delRequest);
      if (!StringUtils.isEmpty(delRsp.getErrorMessage())) {
        log.error("Destroy scalingConfig failed with response:" + JSONObject.toJSONString(delRsp));
        StringBuffer msg = new StringBuffer();
        msg.append("DeleteScalingConfig Failed:").append(delRsp.getErrorMessage()).append("(").append(delRsp.getRequestId()).append(")");
        getTask().updateStatus(BASE_PHASE, msg.toString());
        getTask().fail(false);
        return null;
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
