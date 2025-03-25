package com.netflix.spinnaker.clouddriver.ecloud.deploy.ops;

import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.deploy.DeploymentResult;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.EcloudDeployDescription;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.handlers.EcloudDeployHandler;
import com.netflix.spinnaker.clouddriver.ecloud.exception.ExceptionUtils;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

public class CloneEcloudServerGroupAtomicOperation implements AtomicOperation<DeploymentResult> {

  private static final String BASE_PHASE = "CLONE_SERVER_GROUP";

  private EcloudDeployDescription description;

  @Autowired
  private EcloudDeployHandler ecloudDeployHandler;

  public CloneEcloudServerGroupAtomicOperation(EcloudDeployDescription description) {
    this.description = description;
  }

  @Override
  public DeploymentResult operate(List priorOutputs) {
    Task task = getTask();
    task.updateStatus(BASE_PHASE, "Initializing of Cloning Ecloud ServerGroup...");
    DeploymentResult result;
    try {
      result = ecloudDeployHandler.handle(description, priorOutputs);
    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.getClass());
      throw e;
    }
    return result;
  }

  private static Task getTask() {
    return TaskRepository.threadLocalTask.get();
  }
}
