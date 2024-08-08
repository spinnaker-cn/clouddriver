package com.netflix.spinnaker.clouddriver.ecloud.deploy.ops;

import com.alibaba.fastjson2.JSONObject;
import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.ecloud.client.openapi.EcloudOpenApiHelper;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.TerminateEcloudInstancesDescription;
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
 * @Description To terminate an ecs of a scalingGroup is to do the removal
 */
@Slf4j
public class TerminateEcloudInstancesAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "TERMINATE_INSTANCES";

  @Autowired private EcloudClusterProvider ecloudClusterProvider;

  @Autowired private EcloudInstanceProvider ecloudInstanceProvider;

  private TerminateEcloudInstancesDescription description;

  public TerminateEcloudInstancesAtomicOperation(TerminateEcloudInstancesDescription description) {
    this.description = description;
  }

  @Override
  public Void operate(List priorOutputs) {
    String account = description.getAccount();
    String region = description.getRegion();
    StringBuffer status = new StringBuffer();
    status
        .append("Terminating instances from serverGroup ")
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
          log.error("Terminate Instances failed with response:" + JSONObject.toJSONString(rsp));
          StringBuffer msg = new StringBuffer();
          msg.append("TerminateEcloudInstances Failed:")
              .append(rsp.getErrorMessage())
              .append("(")
              .append(rsp.getRequestId())
              .append(")");
          getTask().updateStatus(BASE_PHASE, msg.toString());
          getTask().fail(false);
          return null;
        }
      } else {
        String info = "TerminateEcloudInstances Failed: NodeIdList Empty!";
        log.error(info);
        getTask().updateStatus(BASE_PHASE, info);
        getTask().fail(false);
        return null;
      }
      status = new StringBuffer();
      status
          .append("Complete termination of instances from serverGroups ")
          .append(description.getServerGroupName())
          .append(" in region ")
          .append(description.getRegion())
          .append(".");
      getTask().updateStatus(BASE_PHASE, status.toString());

    } else {
      String info = "ServerGroup Not Found:" + description.getServerGroupName();
      log.error(info);
      getTask().updateStatus(BASE_PHASE, info);
      getTask().fail(false);
    }
    return null;
  }

  public static Task getTask() {
    return TaskRepository.threadLocalTask.get();
  }
}
