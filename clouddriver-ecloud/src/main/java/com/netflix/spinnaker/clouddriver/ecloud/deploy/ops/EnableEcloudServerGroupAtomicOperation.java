package com.netflix.spinnaker.clouddriver.ecloud.deploy.ops;

import com.alibaba.fastjson2.JSONObject;
import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.ecloud.client.openapi.EcloudOpenApiHelper;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.EnableEcloudServerGroupDescription;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudInstance;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudRequest;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudResponse;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudServerGroup;
import com.netflix.spinnaker.clouddriver.ecloud.provider.view.EcloudClusterProvider;
import com.netflix.spinnaker.clouddriver.ecloud.util.EcloudLbUtil;
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
      // check the state of sg
      EcloudRequest checkReq =
        new EcloudRequest(
          "GET",
          description.getRegion(),
          "/api/v4/autoScaling/scalingGroup/" + sg.getScalingGroupId(),
          description.getCredentials().getAccessKey(),
          description.getCredentials().getSecretKey());
      EcloudResponse checkRsp = EcloudOpenApiHelper.execute(checkReq);
      if (!StringUtils.isEmpty(checkRsp.getErrorMessage())) {
        log.error("Check ServerGroup failed with response:" + JSONObject.toJSONString(checkRsp));
        getTask()
          .updateStatus(BASE_PHASE, "QueryServerGroup Failed:" + checkRsp.getErrorMessage());
        getTask().fail(false);
        return null;
      }
      Map checkBody = (Map) checkRsp.getBody();
      if (checkBody != null && "ACTIVE".equalsIgnoreCase((String) checkBody.get("status"))) {
        status
            .append("ServerGroup ")
            .append(description.getServerGroupName())
            .append(" in region ")
            .append(description.getRegion())
            .append(" is already enabled.");
        getTask().updateStatus(BASE_PHASE, status.toString());
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
          Map<String, Object> queryParams = new HashMap<>();
          queryParams.put("needBatch", true);
          Map<String, Object> memberBody = new HashMap<>();
          memberBody.put("poolMemberCreateReqs", poolMemberCreateReqs);
          memberRequest.setBodyParams(memberBody);
          EcloudResponse memberRsp = EcloudOpenApiHelper.execute(memberRequest);
          if (!StringUtils.isEmpty(memberRsp.getErrorMessage())) {
            log.error("Add Lb Member failed with response:" + JSONObject.toJSONString(memberRsp));
            if ("CSLOPENSTACK_LB_LOAD_BALANCE_BIZ_MEMBER_CREATE_SAME_PORT_OF_VM_HAS_ADDED".equals(memberRsp.getErrorCode())) {
              // already added, ignore the exception
              continue;
            }
            getTask().updateStatus(BASE_PHASE, "AddMemberToLb Failed:" + memberRsp.getErrorMessage());
            getTask().fail(false);
            return null;
          }
          boolean lbOk = EcloudLbUtil.checkLbTaskStatus(description.getRegion(), description.getCredentials().getAccessKey(),
            description.getCredentials().getSecretKey(), memberRsp.getRequestId());
          if (!lbOk) {
            getTask().updateStatus(BASE_PHASE, "Check LoadBalance Status Failed. Operation interupted.");
            getTask().fail(false);
            return null;
          }
        }
      }
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
        log.error("Enable ServerGroup failed with response:" + JSONObject.toJSONString(enableRsp));
        getTask()
          .updateStatus(
            BASE_PHASE, "EnableEcloudServerGroup Failed:" + enableRsp.getErrorMessage());
        getTask().fail(false);
        return null;
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
