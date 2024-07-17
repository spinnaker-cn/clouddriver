package com.netflix.spinnaker.clouddriver.ecloud.deploy.ops;

import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.ecloud.client.openapi.EcloudOpenApiHelper;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.RebootEcloudInstancesDescription;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudRequest;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudResponse;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * @author xu.dangling
 * @date 2024/4/12 
 * @Description Reboot Ecloud Instances
 */
@Slf4j
public class RebootEcloudInstancesAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "REBOOT_INSTANCES";

  private RebootEcloudInstancesDescription description;

  public RebootEcloudInstancesAtomicOperation(RebootEcloudInstancesDescription description) {
    this.description = description;
  }

  @Override
  public Void operate(List priorOutputs) {
    String region = description.getRegion();
    String serverGroupName = description.getServerGroupName();
    List<String> instanceIds = description.getInstanceIds();
    StringBuffer status = new StringBuffer();
    status
        .append("Rebooting instances (")
        .append(String.join(", ", instanceIds))
        .append(") in ")
        .append(region)
        .append(":")
        .append(serverGroupName)
        .append("...");
    getTask().updateStatus(BASE_PHASE, status.toString());
    EcloudRequest request =
        new EcloudRequest(
            "PUT",
            description.getRegion(),
            "/api/openapi-ecs/acl/v3/server/batch/reboot",
            description.getCredentials().getAccessKey(),
            description.getCredentials().getSecretKey());
    request.setVersion("2016-12-05");
    Map<String, Object> body = new HashMap<>();
    body.put("serverIds", description.getInstanceIds());
    request.setBodyParams(body);
    EcloudResponse rsp = EcloudOpenApiHelper.execute(request);
    if (!StringUtils.isEmpty(rsp.getErrorMessage())) {
      String info = "RebootEcloudInstances Failed:" + rsp.getErrorMessage();
      log.error(info);
      getTask().updateStatus(BASE_PHASE, info);
      getTask().fail(false);
      return null;
    }
    getTask().updateStatus(BASE_PHASE, "Complete reboot of instance.");
    return null;
  }

  public static Task getTask() {
    return TaskRepository.threadLocalTask.get();
  }
}
