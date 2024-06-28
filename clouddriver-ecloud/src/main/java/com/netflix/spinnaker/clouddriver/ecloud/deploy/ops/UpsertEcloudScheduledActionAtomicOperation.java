package com.netflix.spinnaker.clouddriver.ecloud.deploy.ops;

import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.UpsertEcloudScheduledActionDescription;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudRequest;
import com.netflix.spinnaker.clouddriver.ecloud.provider.view.EcloudClusterProvider;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

public class UpsertEcloudScheduledActionAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "UPSERT_SCHEDULED_ACTIONS";

  UpsertEcloudScheduledActionDescription description;

  @Autowired EcloudClusterProvider ecloudClusterProvider;

  UpsertEcloudScheduledActionAtomicOperation(UpsertEcloudScheduledActionDescription description) {
    this.description = description;
  }

  @Override
  public Void operate(List priorOutputs) {
    String region = description.getRegion();
    String serverGroupName = description.getServerGroupName();
    String accountName = description.getAccountName();
    String scalingGroupId =
        ecloudClusterProvider.getServerGroupIdByName(accountName, region, serverGroupName);
    if (scalingGroupId != null) {
      getTask()
          .updateStatus(
              BASE_PHASE,
              "Initializing upsert scheduled action " + serverGroupName + " in " + region);
      if (description
          .getOperationType()
          .equals(UpsertEcloudScheduledActionDescription.OperationType.CREATE)) {

        EcloudRequest request =
            new EcloudRequest(
                "POST",
                description.getRegion(),
                "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/scheduledTask",
                description.getCredentials().getAccessKey(),
                description.getCredentials().getSecretKey());
        // TODO
      }
      getTask().updateStatus(BASE_PHASE, "Complete upsert scheduled action.");
    }
    return null;
  }

  private static Task getTask() {
    return TaskRepository.threadLocalTask.get();
  }
}
