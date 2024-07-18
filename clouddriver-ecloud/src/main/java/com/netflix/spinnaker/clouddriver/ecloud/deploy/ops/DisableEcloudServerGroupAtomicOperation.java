package com.netflix.spinnaker.clouddriver.ecloud.deploy.ops;

import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.ecloud.client.openapi.EcloudOpenApiHelper;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.DisableEcloudServerGroupDescription;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudInstance;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudRequest;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudResponse;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudServerGroup;
import com.netflix.spinnaker.clouddriver.ecloud.provider.view.EcloudClusterProvider;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author xu.dangling
 * @date 2024/4/11
 * @Description Disable Ecloud Scaling Group
 */
@Slf4j
public class DisableEcloudServerGroupAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "DISABLE_SERVER_GROUP";

  @Autowired private EcloudClusterProvider ecloudClusterProvider;

  private DisableEcloudServerGroupDescription description;

  public DisableEcloudServerGroupAtomicOperation(DisableEcloudServerGroupDescription description) {
    this.description = description;
  }

  @Override
  public Void operate(List priorOutputs) {
    StringBuffer status = new StringBuffer();
    status
        .append("Disable serverGroups ")
        .append(description.getServerGroupName())
        .append(" in region ")
        .append(description.getRegion())
        .append("...");
    getTask().updateStatus(BASE_PHASE, status.toString());
    EcloudServerGroup sg =
        ecloudClusterProvider.getServerGroup(
            description.getAccount(), description.getRegion(), description.getServerGroupName());
    if (sg != null) {
      EcloudRequest disableRequest =
          new EcloudRequest(
              "PUT",
              description.getRegion(),
              "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/scalingGroup/"
                  + sg.getScalingGroupId(),
              description.getCredentials().getAccessKey(),
              description.getCredentials().getSecretKey());
      Map<String, String> query = new HashMap<>();
      query.put("action", "disable");
      disableRequest.setQueryParams(query);
      EcloudResponse disableRsp = EcloudOpenApiHelper.execute(disableRequest);
      if (!StringUtils.isEmpty(disableRsp.getErrorMessage())) {
        getTask()
            .updateStatus(
                BASE_PHASE, "DisableEcloudServerGroup Failed:" + disableRsp.getErrorMessage());
        getTask().fail(false);
        return null;
      }
      List<EcloudServerGroup.ForwardLoadBalancer> lbs = sg.getForwardLoadBalancers();
      if (!CollectionUtils.isEmpty(lbs)) {
        Set<EcloudInstance> instanceSet = sg.getInstances();
        for (EcloudServerGroup.ForwardLoadBalancer lb : lbs) {
          for (EcloudInstance inst : instanceSet) {
            if (inst.getLbMemberMap() != null
                && inst.getLbMemberMap().get(lb.getPoolId()) != null) {
              String memberId = inst.getLbMemberMap().get(lb.getPoolId());
              EcloudRequest memberRequest =
                  new EcloudRequest(
                      "DELETE",
                      description.getRegion(),
                      "/api/openapi-vlb/lb-console/acl/v3/member/"
                          + lb.getPoolId()
                          + "/member/"
                          + memberId,
                      description.getCredentials().getAccessKey(),
                      description.getCredentials().getSecretKey());
              EcloudResponse memberRsp = EcloudOpenApiHelper.execute(memberRequest);
              if (!StringUtils.isEmpty(memberRsp.getErrorMessage())) {
                String info = "DeleteLbMember Failed:" + disableRsp.getErrorMessage();
                log.error(info);
                getTask().updateStatus(BASE_PHASE, info);
                getTask().fail(false);
                return null;
              }
            } else {
              String info =
                  "DisableEcloudServerGroup Failed: Lb MemberId Not Found at poolId:"
                      + lb.getPoolId();
              log.error(info);
              getTask().updateStatus(BASE_PHASE, info);
              getTask().fail(false);
              return null;
            }
          }
        }
      }
      status = new StringBuffer();
      status
          .append("Complete of disable serverGroups ")
          .append(description.getServerGroupName())
          .append(" in region ")
          .append(description.getRegion())
          .append(".");
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
