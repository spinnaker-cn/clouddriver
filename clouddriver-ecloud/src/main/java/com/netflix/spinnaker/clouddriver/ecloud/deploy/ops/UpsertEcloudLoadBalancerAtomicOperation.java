package com.netflix.spinnaker.clouddriver.ecloud.deploy.ops;

import com.alibaba.fastjson2.JSON;
import com.ecloud.sdk.config.Config;
import com.ecloud.sdk.vlb.v1.Client;
import com.ecloud.sdk.vlb.v1.model.CreateLoadBalanceListenerAsyncBody;
import com.ecloud.sdk.vlb.v1.model.CreateLoadBalanceListenerAsyncRequest;
import com.ecloud.sdk.vlb.v1.model.CreateLoadBalanceListenerAsyncResponse;
import com.ecloud.sdk.vlb.v1.model.DeleteListenerPath;
import com.ecloud.sdk.vlb.v1.model.DeleteListenerRequest;
import com.ecloud.sdk.vlb.v1.model.DeleteListenerResponse;
import com.ecloud.sdk.vlb.v1.model.ElbOrderCreateAsyncBody;
import com.ecloud.sdk.vlb.v1.model.ElbOrderCreateAsyncRequest;
import com.ecloud.sdk.vlb.v1.model.ElbOrderCreateAsyncResponse;
import com.ecloud.sdk.vlb.v1.model.GetLoadBalanceDetailRespPath;
import com.ecloud.sdk.vlb.v1.model.GetLoadBalanceDetailRespRequest;
import com.ecloud.sdk.vlb.v1.model.GetLoadBalanceDetailRespResponse;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalanceListenerRespResponseContent;
import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.UpsertEcloudLoadBalancerDescription;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerListener;
import com.netflix.spinnaker.clouddriver.ecloud.util.EcloudLbUtil;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-11
 */
@Slf4j
public class UpsertEcloudLoadBalancerAtomicOperation implements AtomicOperation<Map> {

  private static final String BASE_PHASE = "UPSERT_LOAD_BALANCER";

  private UpsertEcloudLoadBalancerDescription description;

  public UpsertEcloudLoadBalancerAtomicOperation(UpsertEcloudLoadBalancerDescription description) {
    this.description = description;
  }

  @Override
  public Map operate(List priorOutputs) {
    Config config = new Config();
    config.setAccessKey(this.description.getCredentials().getAccessKey());
    config.setSecretKey(this.description.getCredentials().getSecretKey());
    config.setPoolId(this.description.getRegion());
    Client client = new Client(config);

    getTask()
        .updateStatus(
            BASE_PHASE,
            "Initializing upsert of Ecloud loadBalancer loadBalancerName="
                + description.getLoadBalancerName()
                + " in region="
                + description.getRegion()
                + "...");
    log.info("params = {}", JSON.toJSONString(description));
    try {
      String loadBalancerId = description.getLoadBalancerId();
      if (loadBalancerId != null && loadBalancerId.length() > 0) {
        updateLoadBalancer(description, client);
      } else { // create new loadBalancer
        insertLoadBalancer(description, client);
      }
    } catch (Exception e) {
      log.error(e.getMessage());
    }

    Map<String, String> innerMap = new HashMap<>();
    innerMap.put("name", description.getLoadBalancerName());

    Map<String, Map<String, String>> outerMap = new HashMap<>();
    outerMap.put(description.getRegion(), innerMap);
    return outerMap;
  }

  private String insertLoadBalancer(
      UpsertEcloudLoadBalancerDescription description, Client client) {
    getTask()
        .updateStatus(
            BASE_PHASE,
            "Start create new loadBalancer loadBalancerName="
                + description.getLoadBalancerName()
                + "...");

    ElbOrderCreateAsyncRequest request = new ElbOrderCreateAsyncRequest();
    ElbOrderCreateAsyncBody body = new ElbOrderCreateAsyncBody();
    body.setDuration(description.getDuration());
    body.setFlavor(description.getFlavor());
    body.setLoadBalanceName(description.getLoadBalancerName());
    body.setSubnetId(description.getSubnetId());
    body.setChargePeriodEnum(
        ElbOrderCreateAsyncBody.ChargePeriodEnumEnum.valueOf(description.getChargePeriod()));
    body.setIpId(description.getIpId());
    body.setIpAddress(description.getIpAddress());
    body.setAutoRenew(description.getAutoRenew());
    body.setReturnUrl(description.getReturnUrl());
    body.setProductType(
        ElbOrderCreateAsyncBody.ProductTypeEnum.valueOf(description.getForwardType()));

    request.setElbOrderCreateAsyncBody(body);
    ElbOrderCreateAsyncResponse result = client.elbOrderCreateAsync(request);

    if (result == null
        || !ElbOrderCreateAsyncResponse.StateEnum.OK.getValue().equals(result.getState().getValue())
        || result.getBody() == null) {
      getTask()
          .updateStatus(
              BASE_PHASE,
              "Create new loadBalancer loadBalancerName="
                  + description.getLoadBalancerName()
                  + " failed!");
      return "";
    }

    String loadBalancerId = "";
    // String loadBalancerId = lbClient.createLoadBalancer(description)[0]
    // Thread.sleep(3000) //wait for create loadBalancer success
    // def loadBalancer = lbClient.getLoadBalancerById(loadBalancerId) //query is create success
    // if (loadBalancer.isEmpty()) {
    // getTask().updateStatus(BASE_PHASE, "Create new loadBalancer
    // loadBalancerName="+description.getLoadBalancerName()+" failed!");
    // return "";
    // }
    getTask()
        .updateStatus(
            BASE_PHASE,
            "Create new loadBalancer loadBalancerName="
                + description.getLoadBalancerName()
                + " success, id is loadBalancerId="
                + loadBalancerId
                + ".");

    // set securityGroups to loadBalancer
    // if (description.loadBalancerType.equals("OPEN") && (description.securityGroups?.size() > 0))
    // {
    // task.updateStatus(BASE_PHASE, "Start set securityGroups ${description.securityGroups} to
    // loadBalancer
    // ${loadBalancerId} ...")
    // lbClient.setLBSecurityGroups(loadBalancerId, description.securityGroups)
    // task.updateStatus(BASE_PHASE, "set securityGroups toloadBalancer ${loadBalancerId} end")
    // }

    // create listener
    List<EcloudLoadBalancerListener> listeners = description.getListeners();
    if (listeners != null && !listeners.isEmpty()) {
      listeners.forEach(
          it -> {
            insertListener(loadBalancerId, it, client);
          });
    }
    getTask()
        .updateStatus(BASE_PHASE, "Create new loadBalancer ${description.loadBalancerName} end");
    return "";
  }

  private String updateLoadBalancer(
      UpsertEcloudLoadBalancerDescription description, Client client) {
    getTask()
        .updateStatus(
            BASE_PHASE,
            "Start update loadBalancer loadBalancerId=" + description.getLoadBalancerId() + " ...");
    GetLoadBalanceDetailRespRequest getLoadBalanceDetailRespRequest =
        new GetLoadBalanceDetailRespRequest();
    GetLoadBalanceDetailRespPath getLoadBalanceDetailRespPath = new GetLoadBalanceDetailRespPath();
    getLoadBalanceDetailRespPath.setLoadBalanceId(description.getLoadBalancerId());
    GetLoadBalanceDetailRespResponse result =
        client.getLoadBalanceDetailResp(getLoadBalanceDetailRespRequest);
    if (result == null
        || !GetLoadBalanceDetailRespResponse.StateEnum.OK
            .getValue()
            .equals(result.getState().getValue())
        || result.getBody() == null) {
      getTask()
          .updateStatus(
              BASE_PHASE,
              "LoadBalancer loadBalancerId=" + description.getLoadBalancerId() + " not exist!");
      return "";
    }

    // update securityGroup
    // if (loadBalancer[0].loadBalancerType.equals("OPEN")) {
    // getTask().updateStatus(BASE_PHASE, "Start update securityGroups
    // ${description.securityGroups} to loadBalancer
    // ${loadBalancerId} ...")
    // lbClient.setLBSecurityGroups(loadBalancerId, description.securityGroups)
    // getTask().updateStatus(BASE_PHASE, "update securityGroups to loadBalancer
    // ${loadBalancerId} end")
    // }

    List<EcloudLoadBalancerListener> newListeners = description.getListeners();

    List<ListLoadBalanceListenerRespResponseContent> queryListeners =
        EcloudLbUtil.getListenerByLbList(
            client, Collections.singletonList(description.getLoadBalancerId()));

    //        List<String> listenerIdList =
    // queryListeners.stream().map(ListLoadBalanceListenerRespResponseContent::getId)
    //            .collect(Collectors.toList());

    // def queryLBTargetList = lbClient.getLBTargetList(loadBalancerId, listenerIdList)

    // delete listener
    for (ListLoadBalanceListenerRespResponseContent oldListener : queryListeners) {
      Optional<EcloudLoadBalancerListener> keepListener =
          newListeners.stream()
              .filter(newListener -> newListener.getId().equals(oldListener.getId()))
              .findFirst();
      if (!keepListener.isPresent()) { // 如果没有找到匹配的监听器
        getTask()
            .updateStatus(
                BASE_PHASE,
                "Start delete listener "
                    + oldListener.getId()
                    + " in "
                    + description.getLoadBalancerId()
                    + " ...");
        DeleteListenerRequest deleteListenerRequest = new DeleteListenerRequest();
        DeleteListenerPath deleteListenerPath = new DeleteListenerPath();
        deleteListenerPath.setListenerId(oldListener.getId());
        DeleteListenerResponse ret = client.deleteListener(deleteListenerRequest);
        getTask()
            .updateStatus(
                BASE_PHASE,
                "Delete listener "
                    + oldListener.getId()
                    + " in "
                    + description.getLoadBalancerId()
                    + ret
                    + " end");
      }
    }

    if (newListeners != null && !newListeners.isEmpty()) {
      for (EcloudLoadBalancerListener inputListener : newListeners) {
        if (inputListener.getId() != null && !inputListener.getId().isEmpty()) {
          Optional<ListLoadBalanceListenerRespResponseContent> oldListenerOpt =
              queryListeners.stream()
                  .filter(listener -> listener.getId().equals(inputListener.getId()))
                  .findFirst();

          if (oldListenerOpt.isPresent()) {
            // ListLoadBalanceListenerRespResponseContent oldListener = oldListenerOpt.get();
            // Optional<Target> oldTargetsOpt = queryLBTargetList.stream()
            // .filter(target -> target.getId().equals(inputListener.getId())).findFirst();
            // // Assuming updateListener method accepts Optional<Target> for oldTargets
            // updateListener(description.getLoadBalancerId(), oldListener, inputListener,
            // oldTargetsOpt.orElse(null),client);
          } else {
            getTask()
                .updateStatus(
                    BASE_PHASE, "Input listener " + inputListener.getId() + " not exist!");
          }
        } else { // no listener id, create new
          insertListener(description.getLoadBalancerId(), inputListener, client);
        }
      }
    }

    getTask()
        .updateStatus(
            BASE_PHASE,
            "Update loadBalancer loadBalancerId=" + description.getLoadBalancerId() + " end");
    return "";
  }

  private String insertListener(
      String loadBalancerId, EcloudLoadBalancerListener listener, Client client) {
    getTask()
        .updateStatus(
            BASE_PHASE,
            "Start create new protocol="
                + listener.getProtocol()
                + " listener in loadBalancerId="
                + loadBalancerId
                + " ...");

    CreateLoadBalanceListenerAsyncRequest request = new CreateLoadBalanceListenerAsyncRequest();
    CreateLoadBalanceListenerAsyncBody body = new CreateLoadBalanceListenerAsyncBody();
    body.setHealthDelay(listener.getHealthDelay());
    body.setGroupType(
        CreateLoadBalanceListenerAsyncBody.GroupTypeEnum.valueOf(listener.getGroupType()));
    body.setRedirectToListenerId(listener.getRedirectToListenerId());
    body.setSniContainerIds(listener.getSniContainerIdList());
    body.setDescription(listener.getDescription());
    body.setIsMultiAz(listener.getIsMultiAz());
    body.setProtocol(
        CreateLoadBalanceListenerAsyncBody.ProtocolEnum.valueOf(listener.getProtocol()));
    body.setHttp2(listener.getHttp2());
    body.setDefaultTlsContainerId(listener.getDefaultTlsContainerId());
    body.setMutualAuthenticationUp(listener.getMutualAuthenticationUp());
    body.setCookieName(listener.getCookieName());
    body.setPoolName(listener.getPoolName());
    body.setSniUp(listener.getSniUp());
    body.setLbAlgorithm(
        CreateLoadBalanceListenerAsyncBody.LbAlgorithmEnum.valueOf(listener.getLbAlgorithm()));
    body.setHealthHttpMethod(listener.getHealthHttpMethod());
    body.setHealthType(
        CreateLoadBalanceListenerAsyncBody.HealthTypeEnum.valueOf(listener.getHealthType()));
    body.setLoadBalanceId(listener.getLoadBalanceId());
    body.setProtocolPort(listener.getProtocolPort());
    body.setHealthExpectedCode(listener.getHealthExpectedCode());
    body.setConnectionLimit(listener.getConnectionLimit());
    body.setHealthMaxRetries(listener.getHealthMaxRetries());
    body.setName(listener.getName());
    body.setPoolId(listener.getPoolId());
    body.setSessionPersistence(
        CreateLoadBalanceListenerAsyncBody.SessionPersistenceEnum.valueOf(
            listener.getSessionPersistence()));
    body.setGroupEnabled(listener.getGroupEnabled());
    body.setHealthUrlPath(listener.getHealthUrlPath());
    body.setCaContainerId(listener.getCaContainerId());
    body.setControlGroupId(listener.getControlGroupId());
    body.setHealthTimeout(listener.getHealthTimeout());
    body.setMultiAzUuid(listener.getMultiAzUuid());

    request.setCreateLoadBalanceListenerAsyncBody(body);
    CreateLoadBalanceListenerAsyncResponse result = client.createLoadBalanceListenerAsync(request);

    //
    // def listenerId = lbClient.createLBListener(loadBalancerId, listener)[0];
    // if (listenerId?.length() > 0) {
    // task.updateStatus(BASE_PHASE, "Create new ${listener.protocol} listener in ${loadBalancerId}
    // success, id is
    // ${listenerId}.")
    // if (listener.protocol in ["TCP", "UDP"]) { //tcp/udp 4 layer
    // def targets = listener.targets
    // if (targets?.size() > 0) {
    // task.updateStatus(BASE_PHASE, "Start Register targets to listener ${listenerId} ...")
    // def ret = lbClient.registerTarget4Layer(loadBalancerId, listenerId, targets)
    // task.updateStatus(BASE_PHASE, "Register targets to listener ${listenerId} ${ret} end.")
    // }
    // } else if (listener.protocol in ["HTTP", "HTTPS"]) { //http/https 7 layer
    // def rules = listener.rules
    // if (rules?.size() > 0) {
    // rules.each {
    // insertLBListenerRule(lbClient, loadBalancerId, listenerId, it)
    // }
    // }
    // }
    // } else {
    // getTask().updateStatus(BASE_PHASE, "Create new listener failed!");
    // return "";
    // }
    getTask()
        .updateStatus(
            BASE_PHASE,
            "Create new protocol="
                + listener.getProtocol()
                + " listener in loadBalancerId="
                + loadBalancerId
                + " end");
    return "";
  }

  private static Task getTask() {
    return TaskRepository.threadLocalTask.get();
  }
}
