package com.netflix.spinnaker.clouddriver.ecloud.deploy.ops;

import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.ecloud.client.openapi.EcloudOpenApiHelper;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.RemoveEcloudServerGroupInstancesDescription;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudInstance;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudRequest;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudResponse;
import com.netflix.spinnaker.clouddriver.ecloud.provider.view.EcloudClusterProvider;
import com.netflix.spinnaker.clouddriver.ecloud.provider.view.EcloudInstanceProvider;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * @author xu.dangling
 * @date 2024/4/12
 * @Description Remove Ecloud Server Group Instances
 */
@Slf4j
public class RemoveEcloudServerGroupInstancesAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "REMOVE_INSTANCES";

  @Autowired private EcloudClusterProvider ecloudClusterProvider;

  @Autowired private EcloudInstanceProvider ecloudInstanceProvider;

  private RemoveEcloudServerGroupInstancesDescription description;

  public RemoveEcloudServerGroupInstancesAtomicOperation(
      RemoveEcloudServerGroupInstancesDescription description) {
    this.description = description;
  }

  @Override
  public Void operate(List priorOutputs) {
    String account = description.getAccount();
    String region = description.getRegion();
    StringBuffer status = new StringBuffer();
    status
        .append("Removing instances from serverGroup ")
        .append(description.getServerGroupName())
        .append(" in region ")
        .append(description.getRegion())
        .append("...");
    getTask().updateStatus(BASE_PHASE, status.toString());
    String scalingGroupId =
        ecloudClusterProvider.getServerGroupIdByName(
            account, region, description.getServerGroupName());
    if (scalingGroupId != null) {
      EcloudRequest request =
          new EcloudRequest(
              "DELETE",
              description.getRegion(),
              "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/node/remove",
              description.getCredentials().getAccessKey(),
              description.getCredentials().getSecretKey());
      request.setVersion("2016-12-05");
      Map<String, Object> body = new HashMap<>();
      List<String> nodeIdList = new ArrayList<>();
      for (String serverId : description.getInstanceIds()) {
        EcloudInstance instance = ecloudInstanceProvider.getInstance(account, region, serverId);
        if (instance.getAsgNodeId() != null) {
          nodeIdList.add(instance.getAsgNodeId());
        }
      }
      if (!nodeIdList.isEmpty()) {
        body.put("nodeIdList", nodeIdList);
        body.put("scalingGroupId", scalingGroupId);
        request.setBodyParams(body);
        EcloudResponse rsp = EcloudOpenApiHelper.execute(request);
        if (!StringUtils.isEmpty(rsp.getErrorMessage())) {
          log.error("RemoveEcloudServerGroupInstances Failed:" + rsp.getErrorMessage());
        }
      } else {
        log.error("RemoveEcloudServerGroupInstances Failed: NodeIdList Empty!");
      }
      status = new StringBuffer();
      status
          .append("Complete removal of instances from serverGroups ")
          .append(description.getServerGroupName())
          .append(" in region ")
          .append(description.getRegion())
          .append(".");
      getTask().updateStatus(BASE_PHASE, status.toString());

    } else {
      log.error("ServerGroup Not Found:" + description.getServerGroupName());
    }
    return null;
  }

  public static Task getTask() {
    return TaskRepository.threadLocalTask.get();
  }
}
