package com.netflix.spinnaker.clouddriver.ecloud.deploy.ops;

import com.alibaba.fastjson2.JSONObject;
import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.ecloud.client.openapi.EcloudOpenApiHelper;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.ResizeEcloudServerGroupDescription;
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

/**
 * @author xu.dangling
 * @Description Update Ecloud Scaling Group
 * @date 2024/4/12
 */
@Slf4j
public class ResizeEcloudServerGroupAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "RESIZE_SERVER_GROUP";

  @Autowired EcloudClusterProvider ecloudClusterProvider;

  private final ResizeEcloudServerGroupDescription description;

  public ResizeEcloudServerGroupAtomicOperation(ResizeEcloudServerGroupDescription description) {
    this.description = description;
  }

  @Override
  public Void operate(List priorOutputs) {
    getTask()
        .updateStatus(
            BASE_PHASE,
            "Initializing resize of server group "
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
              "PUT",
              description.getRegion(),
              "/api/v4/autoScaling/scalingGroup",
              description.getCredentials().getAccessKey(),
              description.getCredentials().getSecretKey());
      Map<String, Object> body = new HashMap<>();
      body.put("scalingGroupId", scalingGroupId);
      body.put("maxSize", description.getCapacity().getMax());
      body.put("minSize", description.getCapacity().getMin());
      body.put("desiredSize", description.getCapacity().getDesired());
      request.setBodyParams(body);
      EcloudResponse rsp = EcloudOpenApiHelper.execute(request);
      if (!StringUtils.isEmpty(rsp.getErrorMessage())) {
        log.error("Resize server group failed with response:" + JSONObject.toJSONString(rsp));
        StringBuffer msg = new StringBuffer();
        msg.append("Resize server group failed:")
            .append(rsp.getErrorMessage())
            .append("(")
            .append(rsp.getRequestId())
            .append(")");
        getTask().updateStatus(BASE_PHASE, msg.toString());
        getTask().fail(false);
        return null;
      }
      getTask()
          .updateStatus(
              BASE_PHASE,
              "Complete resize of server group "
                  + description.getServerGroupName()
                  + " in "
                  + description.getRegion());

    } else {
      String info = "ServerGroup Not Found:" + description.getServerGroupName();
      log.error(info);
      getTask().updateStatus(BASE_PHASE, info);
      getTask().fail(false);
    }
    return null;
  }

  private static Task getTask() {
    return TaskRepository.threadLocalTask.get();
  }
}
