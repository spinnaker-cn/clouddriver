package com.netflix.spinnaker.clouddriver.ecloud.deploy.ops;

import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.ecloud.client.openapi.EcloudOpenApiHelper;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.EnableEcloudServerGroupDescription;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudInstance;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudRequest;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudResponse;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudServerGroup;
import com.netflix.spinnaker.clouddriver.ecloud.provider.view.EcloudClusterProvider;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import java.util.ArrayList;
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
 * @Description Enable Ecloud Scaling Group
 */
@Slf4j
public class EnableEcloudServerGroupAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "ENABLE_SERVER_GROUP";

  @Autowired private EcloudClusterProvider ecloudClusterProvider;

  private EnableEcloudServerGroupDescription description;

  public EnableEcloudServerGroupAtomicOperation(EnableEcloudServerGroupDescription description) {
    this.description = description;
  }

  @Override
  public Void operate(List priorOutputs) {
    StringBuffer status = new StringBuffer();
    status
        .append("Enable serverGroups ")
        .append(description.getServerGroupName())
        .append(" in region ")
        .append(description.getRegion())
        .append("...");
    getTask().updateStatus(BASE_PHASE, status.toString());
    EcloudServerGroup sg =
        ecloudClusterProvider.getServerGroup(
            description.getAccount(), description.getRegion(), description.getServerGroupName());
    if (sg != null) {
      EcloudRequest enableRequest =
          new EcloudRequest(
              "PUT",
              description.getRegion(),
              "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/scalingGroup/"
                  + sg.getScalingGroupId(),
              description.getCredentials().getAccessKey(),
              description.getCredentials().getSecretKey());
      Map<String, String> query = new HashMap<>();
      query.put("action", "enable");
      enableRequest.setQueryParams(query);
      EcloudResponse enableRsp = EcloudOpenApiHelper.execute(enableRequest);
      if (!StringUtils.isEmpty(enableRsp.getErrorMessage())) {
        String info = "EnableEcloudServerGroup Failed:" + enableRsp.getErrorMessage();
        log.error(info);
        getTask().updateStatus(BASE_PHASE, info);
        getTask().fail(false);
        return null;
      }
      List<EcloudServerGroup.ForwardLoadBalancer> lbs = sg.getForwardLoadBalancers();
      if (!CollectionUtils.isEmpty(lbs)) {
        Set<EcloudInstance> instanceSet = sg.getInstances();
        List<Map> poolMemberCreateReqs = new ArrayList<>();
        for (EcloudInstance instance : instanceSet) {
          Map one = new HashMap();
          one.put("ip", instance.getPrivateIpAddresses().get(0));
          one.put("type", 1);
          one.put("vmHostId", instance.getName());
          poolMemberCreateReqs.add(one);
        }
        for (EcloudServerGroup.ForwardLoadBalancer lb : lbs) {
          poolMemberCreateReqs.forEach(
              one -> {
                one.put("port", lb.getPort());
                one.put("weight", lb.getWeight());
              });
          EcloudRequest memberRequest =
              new EcloudRequest(
                  "POST",
                  description.getRegion(),
                  "/api/openapi-vlb/lb-console/acl/v3/member/" + lb.getPoolId() + "/openApiMembers",
                  description.getCredentials().getAccessKey(),
                  description.getCredentials().getSecretKey());
          Map<String, Object> memberBody = new HashMap<>();
          memberBody.put("poolMemberCreateReqs", poolMemberCreateReqs);
          memberRequest.setBodyParams(memberBody);
          EcloudResponse memberRsp = EcloudOpenApiHelper.execute(memberRequest);
          if (!StringUtils.isEmpty(memberRsp.getErrorMessage())) {
            String info = "AddMemberToLb Failed:" + memberRsp.getErrorMessage();
            log.error(info);
            getTask().updateStatus(BASE_PHASE, info);
            getTask().fail(false);
            return null;
          }
        }
      }
      status = new StringBuffer();
      status
          .append("Complete of enable serverGroups ")
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

  static Task getTask() {
    return TaskRepository.threadLocalTask.get();
  }
}
