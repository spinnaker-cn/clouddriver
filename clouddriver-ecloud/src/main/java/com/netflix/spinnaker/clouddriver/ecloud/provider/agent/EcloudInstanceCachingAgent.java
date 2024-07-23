package com.netflix.spinnaker.clouddriver.ecloud.provider.agent;

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE;
import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.INFORMATIVE;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.agent.AgentDataType;
import com.netflix.spinnaker.cats.agent.CacheResult;
import com.netflix.spinnaker.cats.agent.DefaultCacheResult;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.DefaultCacheData;
import com.netflix.spinnaker.cats.provider.ProviderCache;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.client.openapi.EcloudOpenApiHelper;
import com.netflix.spinnaker.clouddriver.ecloud.exception.EcloudException;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudRequest;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudResponse;
import com.netflix.spinnaker.clouddriver.ecloud.security.EcloudCredentials;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author xu.dangling
 * @date 2024/4/8
 * @Description
 */
@Slf4j
public class EcloudInstanceCachingAgent extends AbstractEcloudCachingAgent {

  static final Collection<AgentDataType> types =
      Collections.unmodifiableCollection(
          new ArrayList<AgentDataType>() {
            {
              add(AUTHORITATIVE.forType(Keys.Namespace.INSTANCES.ns));
              add(INFORMATIVE.forType(Keys.Namespace.SERVER_GROUPS.ns));
            }
          });

  public EcloudInstanceCachingAgent(
      EcloudCredentials account, String region, ObjectMapper objectMapper) {
    super(account, region, objectMapper);
  }

  @Override
  public CacheResult loadData(ProviderCache providerCache) {
    Map<String, Collection<CacheData>> resultMap = new HashMap<>(16);
    // only cache instances of scalingGroup
    Map<String, ScalingGroupNode> scalingGroupNodes = this.loadScalingGroupNodes();
    List<CacheData> instanceDatas = new ArrayList<>();
    EcloudRequest request =
        new EcloudRequest(
            "GET",
            region,
            "/api/openapi-console-web/v3/server/with/network",
            account.getAccessKey(),
            account.getSecretKey());
    request.setVersion("2016-12-05");
    Map<String, String> pageQuery = new HashMap<>();
    request.setQueryParams(pageQuery);
    pageQuery.put("tagNeeded", "true");
    pageQuery.put("size", "50");
    int page = 1;
    int count = 0;
    while (true) {
      pageQuery.put("page", "" + page);
      EcloudResponse rsp = EcloudOpenApiHelper.execute(request);
      if (rsp.getBody() != null) {
        Map body = (Map) rsp.getBody();
        if (body != null) {
          int total = (int) body.get("total");
          List<Map> vmList = (List<Map>) body.get("content");
          if (!CollectionUtils.isEmpty(vmList)) {
            count += vmList.size();
            for (Map attributes : vmList) {
              String id = (String) attributes.get("id");
              attributes.put("provider", EcloudProvider.ID);
              attributes.put("account", account.getName());
              attributes.put("poolId", region);
              ScalingGroupNode node = scalingGroupNodes.get(id);
              if (node != null) {
                attributes.put("asgNodeId", node.getNodeId());
                attributes.put("serverGroupName", node.getServerGroupName());
              }
              CacheData data =
                  new DefaultCacheData(
                      Keys.getInstanceKey(id, account.getName(), region),
                      attributes,
                      new HashMap<>(16));
              instanceDatas.add(data);
            }
            if (count < total) {
              page++;
              continue;
            }
          }
        }
        break;
      }
      break;
    }

    resultMap.put(Keys.Namespace.INSTANCES.ns, instanceDatas);

    return new DefaultCacheResult(resultMap);
  }

  private Map<String, ScalingGroupNode> loadScalingGroupNodes() {
    Map<String, ScalingGroupNode> scalingGroupNodes = new HashMap<>();
    EcloudRequest asgRequest =
        new EcloudRequest(
            "GET",
            region,
            "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/scalingGroup/list",
            account.getAccessKey(),
            account.getSecretKey());
    asgRequest.setVersion("2016-12-05");
    Map<String, String> queryParams = new HashMap<>();
    asgRequest.setQueryParams(queryParams);
    queryParams.put("pageSize", "50");
    int page = 1;
    while (true) {
      queryParams.put("page", "" + page);
      EcloudResponse listRsp = EcloudOpenApiHelper.execute(asgRequest);
      if (listRsp.getBody() != null) {
        Map listBody = (Map) listRsp.getBody();
        int totalPage = (int) listBody.get("totalPages");
        List<Map> scalingGroups = (List<Map>) listBody.get("scalingGroups");
        if (scalingGroups != null && !scalingGroups.isEmpty()) {
          for (Map sg : scalingGroups) {
            // Get Related Instances
            List<Map> nodeList = this.getScalingNodes((String) sg.get("scalingGroupId"));
            for (Map nodeMap : nodeList) {
              ScalingGroupNode node = new ScalingGroupNode();
              String serverId = (String) nodeMap.get("serverId");
              node.setServerGroupName((String) sg.get("scalingGroupName"));
              node.setNodeId((String) nodeMap.get("nodeId"));
              scalingGroupNodes.put(serverId, node);
            }
          }
        }
        if (totalPage > page) {
          page++;
          continue;
        }
      }
      else if (!StringUtils.isEmpty(listRsp.getErrorMessage())) {
        log.error("ListScalingNode Failed:" + JSONObject.toJSONString(listRsp));
        throw new EcloudException("ListScalingNode Failed");
      }
      break;
    }
    return scalingGroupNodes;
  }

  private List<Map> getScalingNodes(String scalingGroupId) {
    EcloudRequest listRequest =
        new EcloudRequest(
            "GET",
            region,
            "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/node/" + scalingGroupId,
            account.getAccessKey(),
            account.getSecretKey());
    listRequest.setVersion("2016-12-05");
    Map<String, String> pageQuery = new HashMap<>();
    listRequest.setQueryParams(pageQuery);
    pageQuery.put("pageSize", "50");
    int page = 1;
    List<Map> nodeList = new ArrayList<>();
    while (true) {
      pageQuery.put("page", "" + page);
      EcloudResponse listRsp = EcloudOpenApiHelper.execute(listRequest);
      if (listRsp.getBody() != null) {
        Map body = (Map) listRsp.getBody();
        int totalPage = (int) body.get("totalPages");
        List<Map> data = (List<Map>) body.get("data");
        if (data != null && !data.isEmpty()) {
          nodeList.addAll(data);
        }
        if (totalPage > page) {
          page++;
          continue;
        }
      }
      break;
    }
    return nodeList;
  }

  @Override
  public Collection<AgentDataType> getProvidedDataTypes() {
    return types;
  }

  static class ScalingGroupNode {
    private String serverGroupName;
    private String nodeId;

    public String getServerGroupName() {
      return serverGroupName;
    }

    public void setServerGroupName(String serverGroupName) {
      this.serverGroupName = serverGroupName;
    }

    public String getNodeId() {
      return nodeId;
    }

    public void setNodeId(String nodeId) {
      this.nodeId = nodeId;
    }
  }
}
