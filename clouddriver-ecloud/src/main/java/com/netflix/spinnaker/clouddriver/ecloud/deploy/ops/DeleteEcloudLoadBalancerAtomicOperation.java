package com.netflix.spinnaker.clouddriver.ecloud.deploy.ops;

import com.alibaba.fastjson2.JSON;
import com.ecloud.sdk.config.Config;
import com.ecloud.sdk.vlb.v1.Client;
import com.ecloud.sdk.vlb.v1.model.DeleteLoadBalanceListenerPath;
import com.ecloud.sdk.vlb.v1.model.DeleteLoadBalanceListenerRequest;
import com.ecloud.sdk.vlb.v1.model.DeleteLoadBalanceListenerResponse;
import com.ecloud.sdk.vlb.v1.model.ElbOrderDeleteQuery;
import com.ecloud.sdk.vlb.v1.model.ElbOrderDeleteRequest;
import com.ecloud.sdk.vlb.v1.model.ElbOrderDeleteResponse;
import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.DeleteEcloudLoadBalancerDescription;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerListener;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-11
 */
@Slf4j
public class DeleteEcloudLoadBalancerAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "DELETE_LOAD_BALANCER";

  private DeleteEcloudLoadBalancerDescription description;

  public DeleteEcloudLoadBalancerAtomicOperation(DeleteEcloudLoadBalancerDescription description) {
    this.description = description;
  }

  @Override
  public Void operate(List priorOutputs) {
    Config config = new Config();
    config.setAccessKey(this.description.getCredentials().getAccessKey());
    config.setSecretKey(this.description.getCredentials().getSecretKey());
    config.setPoolId(this.description.getRegion());
    Client client = new Client(config);

    getTask()
        .updateStatus(
            BASE_PHASE,
            "Initializing delete of Ecloud loadBalancer loadBalancerId="
                + description.getLoadBalancerId()
                + "in region="
                + description.getRegion()
                + "...");
    log.info("params = {}", JSON.toJSONString(description));

    List<EcloudLoadBalancerListener> listeners = description.getListener();
    if (listeners != null && !listeners.isEmpty()) {
      for (EcloudLoadBalancerListener listener : listeners) {
        deleteListener(description.getLoadBalancerId(), listener.getId(), client);
      }
    } else {
      deleteLoadBalancer(description.getLoadBalancerId(), client);
    }
    return null;
  }

  private void deleteLoadBalancer(String loadBalancerId, Client client) {
    getTask()
        .updateStatus(
            BASE_PHASE, "Start delete loadBalancer loadBalancerId=" + loadBalancerId + " ...");
    ElbOrderDeleteRequest elbOrderDeleteRequest = new ElbOrderDeleteRequest();
    ElbOrderDeleteQuery elbOrderDeleteQuery = new ElbOrderDeleteQuery();
    elbOrderDeleteQuery.setLoadBalanceId(loadBalancerId);
    elbOrderDeleteRequest.setElbOrderDeleteQuery(elbOrderDeleteQuery);
    ElbOrderDeleteResponse result = client.elbOrderDelete(elbOrderDeleteRequest);
    getTask()
        .updateStatus(
            BASE_PHASE,
            "Delete loadBalancer loadBalancerId="
                + loadBalancerId
                + ",res="
                + JSON.toJSONString(result)
                + " end");
  }

  private void deleteListener(String loadBalancerId, String listenerId, Client client) {
    getTask()
        .updateStatus(
            BASE_PHASE,
            "Start delete Listener loadBalancerId="
                + loadBalancerId
                + ",listenerId="
                + listenerId
                + " ...");

    DeleteLoadBalanceListenerRequest deleteListenerRequest = new DeleteLoadBalanceListenerRequest();
    DeleteLoadBalanceListenerPath deleteListenerPath = new DeleteLoadBalanceListenerPath();
    deleteListenerPath.setListenerId(listenerId);
    DeleteLoadBalanceListenerResponse result = client.deleteLoadBalanceListener(deleteListenerRequest);
    getTask()
        .updateStatus(
            BASE_PHASE,
            "Delete loadBalancer listener loadBalancerId="
                + loadBalancerId
                + ",listenerId="
                + listenerId
                + ",res="
                + JSON.toJSONString(result)
                + " end");
  }

  private static Task getTask() {
    return TaskRepository.threadLocalTask.get();
  }
}
