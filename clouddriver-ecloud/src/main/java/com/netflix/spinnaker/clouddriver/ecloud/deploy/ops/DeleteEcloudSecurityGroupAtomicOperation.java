package com.netflix.spinnaker.clouddriver.ecloud.deploy.ops;

import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.ecloud.client.EcloudVirtualPrivateCloudClient;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.DeleteEcloudSecurityGroupDescription;
import com.netflix.spinnaker.clouddriver.ecloud.exception.ExceptionUtils;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteEcloudSecurityGroupAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "DELETE_SECURITY_GROUP";
  private DeleteEcloudSecurityGroupDescription description;

  public DeleteEcloudSecurityGroupAtomicOperation(
      DeleteEcloudSecurityGroupDescription description) {
    this.description = description;
  }

  @Override
  public Void operate(List priorOutputs) {
    getTask()
        .updateStatus(
            BASE_PHASE,
            "Initializing delete of Tencent securityGroup "
                + description.getSecurityGroupId()
                + " in "
                + description.getRegion()
                + "...");
    try {
      EcloudVirtualPrivateCloudClient vpcClient =
          new EcloudVirtualPrivateCloudClient(
              description.getCredentials(), description.getRegion());
      String securityGroupId = description.getSecurityGroupId();
      getTask().updateStatus(BASE_PHASE, "Start delete securityGroup " + securityGroupId + "...");
      vpcClient.deleteSecurityGroup(securityGroupId);
    } catch (Exception e) {
      ExceptionUtils.registerMetric(e, AlarmLevelEnum.LEVEL_1, this.getClass());
      throw e;
    }
    getTask()
        .updateStatus(
            BASE_PHASE, "Delete securityGroup " + description.getSecurityGroupId() + " end");

    return null;
  }

  private static Task getTask() {
    return TaskRepository.threadLocalTask.get();
  }
}
