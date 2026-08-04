package com.netflix.spinnaker.clouddriver.ecloud.deploy.ops;

import com.alibaba.fastjson2.JSONObject;
import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.ecloud.client.openapi.EcloudOpenApiHelper;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.DisableEcloudServerGroupDescription;
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
 * @author xu.dangling @Description Disable Ecloud Scaling Group
 * @date 2024/4/11
 */
@Slf4j
public class DisableEcloudServerGroupAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "DISABLE_SERVER_GROUP";

  /** 单次 batchDelete 接口的 memberIds 数量上限(loadBalanceProperties.getBatchCreateMemberLimit,默认 20) */
  private static final int LB_MEMBER_BATCH_SIZE = 20;

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
      log.info(
          "DisableServerGroup start: serverGroup={}, account={}, region={}, scalingGroupId={}",
          description.getServerGroupName(),
          description.getAccount(),
          description.getRegion(),
          sg.getScalingGroupId());
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
        StringBuffer info = new StringBuffer();
        info.append("QueryServerGroup Failed:")
            .append(checkRsp.getErrorMessage())
            .append("(")
            .append(checkRsp.getRequestId())
            .append(")");
        getTask().updateStatus(BASE_PHASE, info.toString());
        getTask().fail(false);
        return null;
      }
      Map checkBody = (Map) checkRsp.getBody();
      if (checkBody != null && "INACTIVE".equalsIgnoreCase((String) checkBody.get("status"))) {
        status
            .append("ServerGroup ")
            .append(description.getServerGroupName())
            .append(" in region ")
            .append(description.getRegion())
            .append(" is already disabled.");
        getTask().updateStatus(BASE_PHASE, status.toString());
        return null;
      }
      List<EcloudServerGroup.ForwardLoadBalancer> lbs = sg.getForwardLoadBalancers();
      if (!CollectionUtils.isEmpty(lbs)) {
        Set<EcloudInstance> instanceSet = sg.getInstances();
        log.info(
            "DisableServerGroup {}: found {} forwardLoadBalancer(s), {} instance(s) to remove from pool.",
            description.getServerGroupName(),
            lbs.size(),
            instanceSet.size());
        for (EcloudServerGroup.ForwardLoadBalancer lb : lbs) {
          List<String> members = new ArrayList<>();
          for (EcloudInstance inst : instanceSet) {
            if (inst.getLbMemberMap() != null
                && inst.getLbMemberMap().get(lb.getPoolId()) != null) {
              members.add(inst.getLbMemberMap().get(lb.getPoolId()));
            } else {
              log.error(
                  "DisableServerGroup {}: instance {} has no lb member mapping for poolId={}, abort.",
                  description.getServerGroupName(),
                  inst.getName(),
                  lb.getPoolId());
              getTask()
                  .updateStatus(
                      BASE_PHASE,
                      "DisableEcloudServerGroup Failed: instance "
                          + inst.getName()
                          + "Not Found at poolId:"
                          + lb.getPoolId());
              getTask().fail(false);
              return null;
            }
          }
          // 按 LB_MEMBER_BATCH_SIZE 分批删除,避免单次 memberIds 超过接口上限触发异常
          int batchCount = (members.size() + LB_MEMBER_BATCH_SIZE - 1) / LB_MEMBER_BATCH_SIZE;
          log.info(
              "DisableServerGroup {}: poolId={} has {} member(s) to delete, split into {} batch(es) of up to {}.",
              description.getServerGroupName(),
              lb.getPoolId(),
              members.size(),
              batchCount,
              LB_MEMBER_BATCH_SIZE);
          getTask()
              .updateStatus(
                  BASE_PHASE,
                  "Removing "
                      + members.size()
                      + " member(s) from pool "
                      + lb.getPoolId()
                      + ", split into "
                      + batchCount
                      + " batch(es) of up to "
                      + LB_MEMBER_BATCH_SIZE
                      + ".");
          for (int i = 0; i < members.size(); i += LB_MEMBER_BATCH_SIZE) {
            List<String> batch =
                members.subList(i, Math.min(i + LB_MEMBER_BATCH_SIZE, members.size()));
            int batchIndex = i / LB_MEMBER_BATCH_SIZE + 1;
            log.info(
                "DisableServerGroup {}: poolId={} deleting batch {}/{}, memberIds={}",
                description.getServerGroupName(),
                lb.getPoolId(),
                batchIndex,
                batchCount,
                batch);
            getTask()
                .updateStatus(
                    BASE_PHASE,
                    "Deleting lb member batch "
                        + batchIndex
                        + "/"
                        + batchCount
                        + " from pool "
                        + lb.getPoolId()
                        + " ("
                        + batch.size()
                        + " member(s))...");
            if (!deleteLbMemberBatch(lb.getPoolId(), batch)) {
              log.error(
                  "DisableServerGroup {}: poolId={} batch {}/{} failed, abort remaining batches.",
                  description.getServerGroupName(),
                  lb.getPoolId(),
                  batchIndex,
                  batchCount);
              return null;
            }
            log.info(
                "DisableServerGroup {}: poolId={} batch {}/{} done.",
                description.getServerGroupName(),
                lb.getPoolId(),
                batchIndex,
                batchCount);
            getTask()
                .updateStatus(
                    BASE_PHASE,
                    "Batch "
                        + batchIndex
                        + "/"
                        + batchCount
                        + " done for pool "
                        + lb.getPoolId()
                        + ".");
          }
        }
      }
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
        log.error(
            "Disable scalingGroup failed with response:" + JSONObject.toJSONString(disableRsp));
        StringBuffer info = new StringBuffer();
        info.append("DisableEcloudServerGroup Failed:")
            .append(disableRsp.getErrorMessage())
            .append("(")
            .append(disableRsp.getRequestId())
            .append(")");
        getTask().updateStatus(BASE_PHASE, info.toString());
        getTask().fail(false);
        return null;
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

  /**
   * 删除单个 pool 的一批 member(不超过 {@link #LB_MEMBER_BATCH_SIZE} 个)。
   *
   * @return true=本批提交成功(轮询失败仅告警,不视为提交失败);false=提交失败,已 task.fail,调用方应中断
   */
  private boolean deleteLbMemberBatch(String poolId, List<String> memberIds) {
    EcloudRequest memberRequest =
        new EcloudRequest(
            "DELETE",
            description.getRegion(),
            "/api/openapi-vlb/lb-console/acl/v3/member/" + poolId + "/member/batchDelete",
            description.getCredentials().getAccessKey(),
            description.getCredentials().getSecretKey());
    Map<String, Object> memberBody = new HashMap<>();
    memberBody.put("memberIds", memberIds);
    memberRequest.setBodyParams(memberBody);
    EcloudResponse memberRsp = EcloudOpenApiHelper.execute(memberRequest);
    if (!StringUtils.isEmpty(memberRsp.getErrorMessage())) {
      log.error("Delete LbMemeber failed with response:" + JSONObject.toJSONString(memberRsp));
      StringBuffer info = new StringBuffer();
      info.append("DeleteLbMember Failed:")
          .append(memberRsp.getErrorMessage())
          .append("(")
          .append(memberRsp.getRequestId())
          .append(")");
      getTask().updateStatus(BASE_PHASE, info.toString());
      getTask().fail(false);
      return false;
    }
    log.info(
        "DeleteLbMember submitted: poolId={}, requestId={}, memberIds={}, start polling task status.",
        poolId,
        memberRsp.getRequestId(),
        memberIds);
    boolean lbOk =
        EcloudLbUtil.checkLbTaskStatus(
            description.getRegion(),
            description.getCredentials().getAccessKey(),
            description.getCredentials().getSecretKey(),
            memberRsp.getRequestId());
    if (!lbOk) {
      log.error(
          "Check LoadBalance Status Failed. RemoveMemberFromLb Operation may fail later. poolId={}, requestId={}, memberIds={}",
          poolId,
          memberRsp.getRequestId(),
          memberIds);
    } else {
      log.info(
          "DeleteLbMember confirmed success: poolId={}, requestId={}, memberIds={}",
          poolId,
          memberRsp.getRequestId(),
          memberIds);
    }
    return true;
  }

  static Task getTask() {
    return TaskRepository.threadLocalTask.get();
  }
}
