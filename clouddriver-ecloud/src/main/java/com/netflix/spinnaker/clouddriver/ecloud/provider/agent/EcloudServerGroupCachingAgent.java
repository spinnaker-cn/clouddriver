package com.netflix.spinnaker.clouddriver.ecloud.provider.agent;

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.frigga.Names;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.cats.agent.AgentDataType;
import com.netflix.spinnaker.cats.agent.CacheResult;
import com.netflix.spinnaker.cats.agent.DefaultCacheResult;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.DefaultCacheData;
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter;
import com.netflix.spinnaker.cats.provider.ProviderCache;
import com.netflix.spinnaker.clouddriver.cache.OnDemandAgent;
import com.netflix.spinnaker.clouddriver.cache.OnDemandMetricsSupport;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.client.openapi.EcloudOpenApiHelper;
import com.netflix.spinnaker.clouddriver.ecloud.exception.EcloudException;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudRequest;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudResponse;
import com.netflix.spinnaker.clouddriver.ecloud.provider.view.MutableCacheData;
import com.netflix.spinnaker.clouddriver.ecloud.security.EcloudCredentials;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.springframework.util.StringUtils;

/**
 * @author xu.dangling
 * @date 2024/4/9 @Description
 */
public class EcloudServerGroupCachingAgent extends AbstractEcloudCachingAgent
    implements OnDemandAgent {

  private static final Collection<AgentDataType> types =
      Collections.unmodifiableCollection(
          new ArrayList<AgentDataType>() {
            {
              add(AUTHORITATIVE.forType(Keys.Namespace.SERVER_GROUPS.ns));
              add(AUTHORITATIVE.forType(Keys.Namespace.CLUSTERS.ns));
              add(AUTHORITATIVE.forType(Keys.Namespace.APPLICATIONS.ns));
            }
          });

  private OnDemandMetricsSupport metricsSupport;

  public EcloudServerGroupCachingAgent(
      EcloudCredentials account, String region, Registry registry, ObjectMapper objectMapper) {
    super(account, region, objectMapper);
    metricsSupport =
        new OnDemandMetricsSupport(
            registry, this, EcloudProvider.ID + ":" + OnDemandType.ServerGroup);
  }

  @Override
  public Collection<AgentDataType> getProvidedDataTypes() {
    return types;
  }

  @Override
  public CacheResult loadData(ProviderCache providerCache) {
    long start = System.currentTimeMillis();

    List<Map> scalingGroupDataList = this.loadScalingGroups(null);
    List<CacheData> toEvictOnDemandCacheData = new ArrayList<>();
    List<CacheData> toKeepOnDemandCacheData = new ArrayList<>();

    Set<String> scalingGroupNames =
        scalingGroupDataList.stream()
            .map(
                sg -> {
                  return Keys.getServerGroupKey(
                      (String) sg.get("scalingGroupName"), account.getName(), region);
                })
            .collect(Collectors.toSet());

    Set<String> pendingOnDemandRequestKeys =
        providerCache
            .filterIdentifiers(
                Keys.Namespace.ON_DEMAND.ns,
                Keys.getServerGroupKey("*", "*", account.getName(), region))
            .stream()
            .filter(scalingGroupNames::contains)
            .collect(Collectors.toSet());

    Collection<CacheData> pendingOnDemandRequestsForServerGroups =
        providerCache.getAll(Keys.Namespace.ON_DEMAND.ns, pendingOnDemandRequestKeys);
    pendingOnDemandRequestsForServerGroups.forEach(
        it -> {
          Date cacheTime = (Date) it.getAttributes().get("cacheTime");
          Integer processedCount = (Integer) it.getAttributes().get("processedCount");
          if ((cacheTime != null && cacheTime.getTime() >= start)
              || (processedCount != null && processedCount <= 0)) {
            toKeepOnDemandCacheData.add(it);
          } else {
            toEvictOnDemandCacheData.add(it);
          }
        });

    CacheResult result =
        buildCacheResult(scalingGroupDataList, toKeepOnDemandCacheData, toEvictOnDemandCacheData);

    result
        .getCacheResults()
        .get(Keys.Namespace.ON_DEMAND.ns)
        .forEach(
            it -> {
              it.getAttributes().put("processedTime", System.currentTimeMillis());
              it.getAttributes()
                  .put(
                      "processedCount",
                      it.getAttributes().get("processedCount") == null
                          ? 0
                          : (int) it.getAttributes().get("processedCount") + 1);
            });

    return result;
  }

  @Override
  public String getOnDemandAgentType() {
    return this.getAgentType() + "-OnDemand";
  }

  @Override
  public OnDemandMetricsSupport getMetricsSupport() {
    return metricsSupport;
  }

  @Override
  public boolean handles(OnDemandType type, String cloudProvider) {
    return type == OnDemandType.ServerGroup && cloudProvider.equals(EcloudProvider.ID);
  }

  @Nullable
  @Override
  public OnDemandResult handle(ProviderCache providerCache, Map<String, ?> data) {
    if (!data.containsKey("serverGroupName")
        || !account.getName().equals(data.get("accountName"))
        || !region.equals(data.get("region"))) {
      return null;
    }
    String serverGroupName = (String) data.get("serverGroupName");
    Map scalingGroup =
        metricsSupport.readData(
            () -> {
              List<Map> scalingGroups = this.loadScalingGroups(serverGroupName);

              Map sgInfo = null;
              if (scalingGroups.size() == 1) {
                sgInfo = scalingGroups.get(0);
              } else {
                // Barely happens: more than one scalingGroup were found, locate the right one.
                sgInfo =
                    scalingGroups.stream()
                        .filter(one -> serverGroupName.equals(one.get("scalingGroupName")))
                        .collect(Collectors.toList())
                        .get(0);
              }
              return sgInfo;
            });

    CacheResult cacheResult =
        metricsSupport.transformData(
            () -> buildCacheResult(Collections.singletonList(scalingGroup), null, null));

    String cacheResultAsJson;
    try {
      cacheResultAsJson = objectMapper.writeValueAsString(cacheResult.getCacheResults());
    } catch (Exception e) {
      logger.error("Error serializing cache result", e);
      return null;
    }

    String serverGroupKey = Keys.getServerGroupKey(serverGroupName, account.getName(), region);

    if (cacheResult.getCacheResults().values().stream().allMatch(Collection::isEmpty)) {
      providerCache.evictDeletedItems(
          Keys.Namespace.ON_DEMAND.ns, Collections.singletonList(serverGroupKey));
    } else {
      metricsSupport.onDemandStore(
          () -> {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("cacheTime", new Date());
            attributes.put("cacheResults", cacheResultAsJson);

            DefaultCacheData cacheData =
                new DefaultCacheData(
                    serverGroupKey,
                    10 * 60, // TTL
                    attributes,
                    new HashMap<>());
            providerCache.putCacheData(Keys.Namespace.ON_DEMAND.ns, cacheData);
            return null;
          });
    }

    Map<String, Collection<String>> evictions =
        scalingGroup != null
            ? new HashMap<>()
            : Collections.singletonMap(
                Keys.Namespace.SERVER_GROUPS.ns, Collections.singletonList(serverGroupKey));

    return new OnDemandResult(getOnDemandAgentType(), cacheResult, evictions);
  }

  @Override
  public Collection<Map> pendingOnDemandRequests(ProviderCache providerCache) {
    Collection<String> keys =
        providerCache.filterIdentifiers(
            Keys.Namespace.ON_DEMAND.ns,
            Keys.getServerGroupKey("*", "*", account.getName(), region));
    return fetchPendingOnDemandRequests(providerCache, keys);
  }

  private Collection<Map> fetchPendingOnDemandRequests(
      ProviderCache providerCache, Collection<String> keys) {
    return providerCache.getAll(Keys.Namespace.ON_DEMAND.ns, keys, RelationshipCacheFilter.none())
        .stream()
        .map(
            item -> {
              Map<String, String> details = Keys.parse(item.getId());

              Map<String, Object> map = new HashMap<>();
              map.put("id", item.getId());
              map.put("details", details);
              map.put("moniker", convertOnDemandDetails(details));
              map.put("cacheTime", item.getAttributes().get("cacheTime"));
              map.put("processedCount", item.getAttributes().get("processedCount"));
              map.put("processedTime", item.getAttributes().get("processedTime"));

              return map;
            })
        .collect(Collectors.toList());
  }

  private List<Map> loadScalingGroups(String keyword) {
    List<Map> allSgDatas = new ArrayList<>();
    Map<String, Map> scalingConfigMap = new HashMap<>(16);
    EcloudRequest listRequest =
        new EcloudRequest(
            "GET",
            region,
            "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/scalingGroup/list",
            account.getAccessKey(),
            account.getSecretKey());
    listRequest.setVersion("2016-12-05");
    Map<String, String> queryParams = new HashMap<>();
    listRequest.setQueryParams(queryParams);
    queryParams.put("pageSize", "50");
    if (keyword != null) {
      queryParams.put("keyword", keyword);
    }
    int page = 1;
    while (true) {
      queryParams.put("page", "" + page);
      EcloudResponse listRsp = EcloudOpenApiHelper.execute(listRequest);
      if (listRsp.getBody() != null) {
        Map listBody = (Map) listRsp.getBody();
        int totalPage = (int) listBody.get("totalPages");
        List<Map> scalingGroups = (List<Map>) listBody.get("scalingGroups");
        if (scalingGroups != null && !scalingGroups.isEmpty()) {
          for (Map sg : scalingGroups) {
            String scalingGroupId = (String) sg.get("scalingGroupId");
            String scalingConfigId = (String) sg.get("scalingConfigId");
            // Get VpcInfo
            String routerId = (String) sg.get("vpcId");
            Map vpcInfo = this.getVpcInfo(routerId);
            sg.putAll(vpcInfo);
            // Get Tags
            List<Map> tagList = this.getScalingGroupTags(scalingGroupId);
            sg.put("tagList", tagList);
            // Get Related Instances
            List<Map> nodeList = this.getScalingNodes(scalingGroupId);
            sg.put("nodeList", nodeList);
            // Get Scaling Config
            Map sc = null;
            if (scalingConfigId != null && scalingConfigMap.get(scalingConfigId) == null) {
              sc = this.getScalingConfig(scalingConfigId);
              scalingConfigMap.put(scalingConfigId, sc);
            }
            sg.put("scalingConfig", sc);
            // Get Scaling Rules
            Map<String, Map> scalingRules = this.getScalingRules(scalingGroupId);
            sg.put("scalingRules", scalingRules);
            // Get Alarm Tasks
            List<Map> alarmTasks = this.getAlarmTasks(scalingGroupId);
            sg.put("alarmTasks", alarmTasks);
            // Get Scheduled Task
            List<Map> scheduledTasks = this.getScheduledTask(scalingGroupId);
            sg.put("scheduledTasks", scheduledTasks);
            // Get LoadBalancers
            List<Map> lbs = this.getLoadBalancers(scalingGroupId);
            sg.put("loadBalancers", lbs);
            // Get scheduledActions (the last 20 actions)
            List<Map> scheduleActions = this.getScheduledActions(scalingGroupId);
            sg.put("scheduledActions", scheduleActions);
            allSgDatas.add(sg);
          }
        }
        if (totalPage > page) {
          page++;
          continue;
        }
      } else {
        logger.error("ListScalingGroup Failed:" + JSONObject.toJSONString(listRsp));
        throw new EcloudException("ListScalingGroup Failed");
      }
      break;
    }
    return allSgDatas;
  }

  private Map getVpcInfo(String routerId) {
    EcloudRequest request =
        new EcloudRequest(
            "GET",
            region,
            "/api/openapi-vpc/customer/v3/vpc/router/" + routerId,
            account.getAccessKey(),
            account.getSecretKey());
    EcloudResponse listRsp = EcloudOpenApiHelper.execute(request);
    Map body = (Map) listRsp.getBody();
    Map vpcInfo = new HashMap();
    vpcInfo.put("realVpcId", body.get("id"));
    vpcInfo.put("vpcName", body.get("name"));
    return vpcInfo;
  }

  private Map getScalingConfig(String scalingConfigId) {
    EcloudRequest request =
        new EcloudRequest(
            "GET",
            region,
            "/api/v4/autoScaling/scalingConfig/" + scalingConfigId,
            account.getAccessKey(),
            account.getSecretKey());
    request.setVersion("2016-12-05");
    EcloudResponse listRsp = EcloudOpenApiHelper.execute(request);
    return (Map) listRsp.getBody();
  }

  private List<Map> getScalingGroupTags(String scalingGroupId) {
    EcloudRequest listRequest =
        new EcloudRequest(
            "GET",
            region,
            "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/scalingGroup/tag",
            account.getAccessKey(),
            account.getSecretKey());
    listRequest.setVersion("2016-12-05");
    Map<String, String> queryParam = new HashMap<>();
    queryParam.put("scalingGroupIds", scalingGroupId);
    listRequest.setQueryParams(queryParam);
    EcloudResponse listRsp = EcloudOpenApiHelper.execute(listRequest);
    List<Map> body = (List<Map>) listRsp.getBody();
    if (body != null && !body.isEmpty()) {
      Map one = body.get(0);
      List<Map> tags = (List<Map>) one.get("tag");
      if (tags != null) {
        return tags;
      }
    }
    return new ArrayList<>();
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
      } else {
        logger.error("ListScalingNodes Failed:" + JSONObject.toJSONString(listRsp));
        throw new EcloudException("ListScalingNodes Failed");
      }
      break;
    }
    return nodeList;
  }

  private Map<String, Map> getScalingRules(String scalingGroupId) {
    EcloudRequest listRequest =
        new EcloudRequest(
            "GET",
            region,
            "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/scalingRule/list",
            account.getAccessKey(),
            account.getSecretKey());
    listRequest.setVersion("2016-12-05");
    Map<String, String> queryParams = new HashMap<>();
    listRequest.setQueryParams(queryParams);
    queryParams.put("pageSize", "50");
    queryParams.put("page", "1");
    queryParams.put("scalingGroupId", scalingGroupId);
    EcloudResponse listRsp = EcloudOpenApiHelper.execute(listRequest);
    if (listRsp.getBody() != null) {
      Map body = (Map) listRsp.getBody();
      if (body != null && body.get("scalingRules") != null) {
        Map<String, Map> scalingRuleMap = new HashMap<>();
        List<Map> scalingRules = (List<Map>) body.get("scalingRules");
        for (Map scalingRule : scalingRules) {
          scalingRuleMap.put((String) scalingRule.get("scalingRuleId"), scalingRule);
        }
        return scalingRuleMap;
      }
    }
    if (!StringUtils.isEmpty(listRsp.getErrorMessage())) {
      logger.error("ListScalingRules Failed:" + JSONObject.toJSONString(listRsp));
      throw new EcloudException("ListScalingRules Failed");
    }
    return null;
  }

  private List<Map> getAlarmTasks(String scalingGroupId) {
    EcloudRequest listRequest =
        new EcloudRequest(
            "GET",
            region,
            "/api/v4/autoScaling/alarmTask/" + scalingGroupId,
            account.getAccessKey(),
            account.getSecretKey());
    listRequest.setVersion("2016-12-05");
    Map<String, String> queryParams = new HashMap<>();
    listRequest.setQueryParams(queryParams);
    queryParams.put("page", "1");
    queryParams.put("pageSize", "50");
    EcloudResponse listRsp = EcloudOpenApiHelper.execute(listRequest);
    if (listRsp.getBody() != null) {
      Map body = (Map) listRsp.getBody();
      if (body != null && body.get("content") != null) {
        return (List<Map>) body.get("content");
      }
    }
    if (!StringUtils.isEmpty(listRsp.getErrorMessage())) {
      logger.error("ListAlarmTasks Failed:" + JSONObject.toJSONString(listRsp));
      throw new EcloudException("ListAlarmTasks Failed");
    }
    return null;
  }

  private List<Map> getScheduledTask(String scalingGroupId) {
    EcloudRequest listRequest =
        new EcloudRequest(
            "GET",
            region,
            "/api/openapi-eas-v2/customer/v3/autoScaling/cloudApi/scheduledTask/"
                + scalingGroupId
                + "/list",
            account.getAccessKey(),
            account.getSecretKey());
    listRequest.setVersion("2016-12-05");
    Map<String, String> queryParams = new HashMap<>();
    listRequest.setQueryParams(queryParams);
    queryParams.put("page", "1");
    queryParams.put("pageSize", "50");
    EcloudResponse listRsp = EcloudOpenApiHelper.execute(listRequest);
    if (listRsp.getBody() != null) {
      Map body = (Map) listRsp.getBody();
      if (body != null && body.get("content") != null) {
        return (List<Map>) body.get("content");
      }
    }
    if (!StringUtils.isEmpty(listRsp.getErrorMessage())) {
      logger.error("ListScheduledTasks Failed:" + JSONObject.toJSONString(listRsp));
      throw new EcloudException("ListScheduledTasks Failed");
    }
    return null;
  }

  private List<Map> getLoadBalancers(String scalingGroupId) {
    EcloudRequest listRequest =
        new EcloudRequest(
            "GET",
            region,
            "/api/v4/autoScaling/scalingGroup/loadBalancer/" + scalingGroupId,
            account.getAccessKey(),
            account.getSecretKey());
    listRequest.setVersion("2016-12-05");
    EcloudResponse listRsp = EcloudOpenApiHelper.execute(listRequest);
    if (listRsp.getBody() != null) {
      List<Map> body = (List<Map>) listRsp.getBody();
      return body;
    }
    if (!StringUtils.isEmpty(listRsp.getErrorMessage())) {
      logger.error("ListLoadBalancer Failed:" + JSONObject.toJSONString(listRsp));
      throw new EcloudException("ListLoadBalancer Failed");
    }
    return null;
  }

  private List<Map> getScheduledActions(String scalingGroupId) {
    EcloudRequest listRequest =
        new EcloudRequest(
            "GET",
            region,
            "/api/v4/autoScaling/scalingActivity/list/page/" + scalingGroupId,
            account.getAccessKey(),
            account.getSecretKey());
    listRequest.setVersion("2016-12-05");
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("page", "1");
    queryParams.put("size", "10");
    listRequest.setQueryParams(queryParams);
    EcloudResponse listRsp = EcloudOpenApiHelper.execute(listRequest);
    if (listRsp.getBody() != null) {
      Map body = (Map) listRsp.getBody();
      if (body != null && body.get("content") != null) {
        return (List<Map>) body.get("content");
      }
    }
    if (!StringUtils.isEmpty(listRsp.getErrorMessage())) {
      logger.error("ListScalingActivity Failed:" + JSONObject.toJSONString(listRsp));
    }
    return null;
  }

  private CacheResult buildCacheResult(
      List<Map> scalingGroupList,
      Collection<CacheData> toKeepOnDemandCacheData,
      Collection<CacheData> toEvictOnDemandCacheData) {

    Map<String, Collection<CacheData>> cacheResults = new HashMap<>();
    Map<String, Collection<String>> evictions = new HashMap<>();
    evictions.put(
        Keys.Namespace.ON_DEMAND.ns,
        toEvictOnDemandCacheData.stream().map(CacheData::getId).collect(Collectors.toList()));
    Map<String, Map<String, CacheData>> namespaceCache = new HashMap<>();
    for (Map sg : scalingGroupList) {
      String serverGroupName = (String) sg.get("scalingGroupName");
      Names parsed = Names.parseName(serverGroupName);
      String applicationName = parsed.getApp();
      String clusterName = parsed.getCluster();

      if (applicationName == null || clusterName == null) {
        continue;
      }
      String serverGroupKey = Keys.getServerGroupKey(serverGroupName, account.getName(), region);
      String clusterKey = Keys.getClusterKey(clusterName, applicationName, account.getName());
      String appKey = Keys.getApplicationKey(applicationName);
      Set<String> instanceKeys = this.getInstanceKeysFromScalingGroup(sg);
      Set<String> loadBalancerKeys = this.getLoadBalanceKeysFromScalingGroup(sg);

      // application
      namespaceCache.computeIfAbsent(Keys.Namespace.APPLICATIONS.ns, k -> new HashMap<>());
      CacheData applications =
          namespaceCache
              .get(Keys.Namespace.APPLICATIONS.ns)
              .computeIfAbsent(appKey, MutableCacheData::new);
      this.addCommonAttributes(applications.getAttributes());
      applications.getAttributes().put("name", applicationName);
      applications
          .getRelationships()
          .computeIfAbsent(Keys.Namespace.CLUSTERS.ns, k -> new HashSet<>())
          .add(clusterKey);
      applications
          .getRelationships()
          .computeIfAbsent(Keys.Namespace.SERVER_GROUPS.ns, k -> new HashSet<>())
          .add(serverGroupKey);

      // cluster
      namespaceCache.computeIfAbsent(Keys.Namespace.CLUSTERS.ns, k -> new HashMap<>());
      CacheData cluster =
          namespaceCache
              .get(Keys.Namespace.CLUSTERS.ns)
              .computeIfAbsent(clusterKey, MutableCacheData::new);
      this.addCommonAttributes(applications.getAttributes());
      cluster.getAttributes().put("name", clusterName);
      cluster
          .getRelationships()
          .computeIfAbsent(Keys.Namespace.APPLICATIONS.ns, k -> new HashSet<>())
          .add(appKey);
      cluster
          .getRelationships()
          .computeIfAbsent(Keys.Namespace.SERVER_GROUPS.ns, k -> new HashSet<>())
          .add(serverGroupKey);
      cluster
          .getRelationships()
          .computeIfAbsent(Keys.Namespace.INSTANCES.ns, k -> new HashSet<>())
          .addAll(instanceKeys);
      cluster
          .getRelationships()
          .computeIfAbsent(Keys.Namespace.LOAD_BALANCERS.ns, k -> new HashSet<>())
          .addAll(loadBalancerKeys);

      // server group
      CacheData onDemandServerGroupCache =
          toKeepOnDemandCacheData.stream()
              .filter(c -> c.getAttributes().get("name").equals(serverGroupKey))
              .findFirst()
              .orElse(null);

      if (onDemandServerGroupCache != null) {
        mergeOnDemandCache(onDemandServerGroupCache, namespaceCache);
      } else {
        CacheData serverGroupCacheData =
            namespaceCache
                .computeIfAbsent(Keys.Namespace.SERVER_GROUPS.ns, k -> new HashMap<>())
                .computeIfAbsent(serverGroupKey, MutableCacheData::new);
        Map<String, Object> serverGroupAttr = objectMapper.convertValue(sg, Map.class);
        serverGroupAttr = addCommonAttributes(serverGroupAttr);
        serverGroupAttr.put("cluster", clusterName);
        serverGroupAttr.put("app", applicationName);
        serverGroupCacheData.getAttributes().putAll(serverGroupAttr);
        serverGroupCacheData
            .getRelationships()
            .computeIfAbsent(Keys.Namespace.APPLICATIONS.ns, k -> new HashSet<>())
            .add(appKey);
        serverGroupCacheData
            .getRelationships()
            .computeIfAbsent(Keys.Namespace.CLUSTERS.ns, k -> new HashSet<>())
            .add(clusterKey);
        serverGroupCacheData
            .getRelationships()
            .computeIfAbsent(Keys.Namespace.INSTANCES.ns, k -> new HashSet<>())
            .addAll(instanceKeys);
        serverGroupCacheData
            .getRelationships()
            .computeIfAbsent(Keys.Namespace.LOAD_BALANCERS.ns, k -> new HashSet<>())
            .addAll(loadBalancerKeys);

        // loadBalancer
        for (String lbKey : loadBalancerKeys) {
          CacheData lbCache =
              namespaceCache
                  .computeIfAbsent(Keys.Namespace.LOAD_BALANCERS.ns, k -> new HashMap<>())
                  .computeIfAbsent(lbKey, MutableCacheData::new);
          lbCache
              .getRelationships()
              .computeIfAbsent(Keys.Namespace.SERVER_GROUPS.ns, k -> new HashSet<>())
              .add(serverGroupKey);
        }
      }
    }

    namespaceCache.forEach(
        (namespace, cacheDataMap) -> {
          cacheResults.put(namespace, new ArrayList<>(cacheDataMap.values()));
        });
    cacheResults.put(Keys.Namespace.ON_DEMAND.ns, toKeepOnDemandCacheData);

    return new DefaultCacheResult(cacheResults, evictions);
  }

  public void mergeOnDemandCache(
      CacheData onDemandServerGroupCache, Map<String, Map<String, CacheData>> namespaceCache) {
    try {
      String jsonCacheResults =
          (String) onDemandServerGroupCache.getAttributes().get("cacheResults");
      Map<String, List<MutableCacheData>> onDemandCache =
          objectMapper.readValue(
              jsonCacheResults, new TypeReference<Map<String, List<MutableCacheData>>>() {});
      onDemandCache.forEach(
          (namespace, cacheDataList) -> {
            if (!"onDemand".equalsIgnoreCase(namespace)) {
              cacheDataList.forEach(
                  cacheData -> {
                    CacheData existingCacheData =
                        namespaceCache
                            .computeIfAbsent(namespace, k -> new HashMap<>())
                            .get(cacheData.getId());
                    if (existingCacheData == null) {
                      namespaceCache.get(namespace).put(cacheData.getId(), cacheData);
                    } else {
                      existingCacheData.getAttributes().putAll(cacheData.getAttributes());
                      cacheData
                          .getRelationships()
                          .forEach(
                              (relationshipName, relationships) -> {
                                existingCacheData
                                    .getRelationships()
                                    .computeIfAbsent(relationshipName, k -> new HashSet<>())
                                    .addAll(relationships);
                              });
                    }
                  });
            }
          });
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
  }

  private Set<String> getInstanceKeysFromScalingGroup(Map sg) {
    Set<String> instanceKeys = new HashSet<>();
    if (sg.get("nodeList") != null) {
      List<Map> nodes = (List<Map>) sg.get("nodeList");
      for (Map node : nodes) {
        instanceKeys.add(
            Keys.getInstanceKey((String) node.get("serverId"), account.getName(), region));
      }
    }
    return instanceKeys;
  }

  private Set<String> getLoadBalanceKeysFromScalingGroup(Map sg) {
    Set<String> lbKeys = new HashSet<>();
    if (sg.get("loadBalancers") != null) {
      List<Map> lbs = (List<Map>) sg.get("loadBalancers");
      for (Map lb : lbs) {
        lbKeys.add(
            Keys.getLoadBalancerKey((String) lb.get("loadBalancerId"), account.getName(), region));
      }
    }
    return lbKeys;
  }

  private Map<String, Object> addCommonAttributes(Map<String, Object> attributes) {
    attributes.put("provider", EcloudProvider.ID);
    attributes.put("account", account.getName());
    attributes.put("poolId", region);
    return attributes;
  }
}
