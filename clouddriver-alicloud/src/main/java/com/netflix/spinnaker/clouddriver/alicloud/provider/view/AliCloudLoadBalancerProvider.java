/*
 * Copyright 2019 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spinnaker.clouddriver.alicloud.provider.view;

import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.CacheFilter;
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter;
import com.netflix.spinnaker.clouddriver.alicloud.AliCloudProvider;
import com.netflix.spinnaker.clouddriver.alicloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.alicloud.common.HealthHelper;
import com.netflix.spinnaker.clouddriver.alicloud.model.AliCloudLoadBalancer;
import com.netflix.spinnaker.clouddriver.model.HealthState;
import com.netflix.spinnaker.clouddriver.model.LoadBalancerInstance;
import com.netflix.spinnaker.clouddriver.model.LoadBalancerProvider;
import com.netflix.spinnaker.clouddriver.model.LoadBalancerServerGroup;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class AliCloudLoadBalancerProvider implements LoadBalancerProvider<AliCloudLoadBalancer> {

  private final ObjectMapper objectMapper;

  private final Cache cacheView;

  private final AliCloudProvider provider;

  @Value("${alicloud.share.application:}")
  private String shareApplication;

  private static List<String> shareApplications;

  @Autowired
  public AliCloudLoadBalancerProvider(
      ObjectMapper objectMapper, Cache cacheView, AliCloudProvider provider) {
    this.objectMapper = objectMapper;
    this.cacheView = cacheView;
    this.provider = provider;
  }

  @PostConstruct
  private void init() {
    shareApplications = Arrays.asList(shareApplication.split(","));
  }

  private static final String SURVIVE_STATUS = "Active";

  @Override
  public Set<AliCloudLoadBalancer> getApplicationLoadBalancers(String applicationName) {
    Set<String> loadBalancerKeys = new HashSet<>();
    Set<AliCloudLoadBalancer> loadBalances = new HashSet<>();

    Collection<CacheData> applicationServerGroups =
        getServerGroupCacheDataByApplication(applicationName);
    Collection<CacheData> allHealthyDatas = cacheView.getAll(HEALTH.ns);
    Collection<String> allLoadBalancerKeys = cacheView.getIdentifiers(LOAD_BALANCERS.ns);
    Collection<String> loadBalancerKeyMatches =
        allLoadBalancerKeys.stream()
            .filter(tab -> applicationMatcher(tab, applicationName))
            .collect(Collectors.toList());
    loadBalancerKeys.addAll(loadBalancerKeyMatches);
    if (!CollectionUtils.isEmpty(shareApplications)) {
      shareApplications.forEach(
          app -> {
            Collection<String> shareLoadBalancerKeyMatches =
                allLoadBalancerKeys.stream()
                    .filter(tab -> applicationMatcher(tab, app))
                    .collect(Collectors.toList());
            loadBalancerKeys.addAll(shareLoadBalancerKeyMatches);
          });
    }
    Collection<CacheData> loadBalancerData =
        cacheView.getAll(LOAD_BALANCERS.ns, loadBalancerKeys, null);
    Map<String, CacheData> serverGroupsMap = new HashMap<>();
    applicationServerGroups.forEach(
        it -> {
          Collection<String> loadBalancers = it.getRelationships().get("loadBalancers");
          if (!CollectionUtils.isEmpty(loadBalancers)) {
            loadBalancers.forEach(lbName -> serverGroupsMap.put(lbName, it));
          }
        });

    for (CacheData cacheData : loadBalancerData) {
      Map<String, Object> attributes =
          objectMapper.convertValue(cacheData.getAttributes(), Map.class);
      String id = cacheData.getId();
      AliCloudLoadBalancer loadBalancer =
          new AliCloudLoadBalancer(
              String.valueOf(attributes.get("account")),
              String.valueOf(attributes.get("regionIdAlias")),
              String.valueOf(attributes.get("loadBalancerName")),
              String.valueOf(attributes.get("vpcId")),
              String.valueOf(attributes.get("loadBalancerId")));
      for (Map.Entry<String, CacheData> entry : serverGroupsMap.entrySet()) {
        if (id.startsWith(entry.getKey())) {
          addServerGroupToLoadBalancer(allHealthyDatas, loadBalancer, entry.getValue());
          break;
        }
      }
      loadBalances.add(loadBalancer);
    }
    return loadBalances;
  }

  @Override
  public List<ResultDetails> byAccountAndRegionAndName(String account, String region, String name) {
    List<ResultDetails> results = new ArrayList<>();
    Set<String> meragLoadBalancerKeys = new HashSet<>();
    String searchKey = Keys.getLoadBalancerKey(name, account, region, null) + "*";
    Collection<String> allLoadBalancerKeys =
        cacheView.filterIdentifiers(LOAD_BALANCERS.ns, searchKey);
    if (!CollectionUtils.isEmpty(allLoadBalancerKeys)) {
      meragLoadBalancerKeys.addAll(allLoadBalancerKeys);
    }
    // share application
    shareApplications.forEach(
        app -> {
          String shareSearchKey = Keys.getLoadBalancerKey(app, account, region, null) + "*";
          Collection<String> shareLoadBalancerKeys =
              cacheView.filterIdentifiers(LOAD_BALANCERS.ns, shareSearchKey);
          if (!CollectionUtils.isEmpty(allLoadBalancerKeys)) {
            meragLoadBalancerKeys.addAll(shareLoadBalancerKeys);
          }
        });
    Collection<CacheData> loadBalancers =
        cacheView.getAll(LOAD_BALANCERS.ns, meragLoadBalancerKeys, null);
    Collection<CacheData> allHealthyDatass = cacheView.getAll(HEALTH.ns);
    String applicationName = getApplicationByName(name);
    Collection<CacheData> applicationServerGroups =
        getServerGroupCacheDataByApplication(applicationName);
    for (CacheData loadBalancer : loadBalancers) {
      ResultDetails resultDetails = new ResultDetails();
      Set<LoadBalancerServerGroup> serverGroups = new HashSet<>();
      String id = loadBalancer.getId();
      for (CacheData applicationServerGroup : applicationServerGroups) {
        Collection<String> relationships =
            applicationServerGroup.getRelationships().get("loadBalancers");
        for (String loadBalancerId : relationships) {
          if (id.startsWith(loadBalancerId)) {
            LoadBalancerServerGroup loadBalancerServerGroup =
                createLoadBalancerServerGroup(
                    allHealthyDatass, loadBalancerId, applicationServerGroup);
            serverGroups.add(loadBalancerServerGroup);
            break;
          }
        }
      }
      Map<String, Object> attributes = loadBalancer.getAttributes();
      attributes.put("serverGroups", serverGroups);
      resultDetails.setResults(attributes);
      results.add(resultDetails);
    }
    return results;
  }

  @Override
  public String getCloudProvider() {
    return AliCloudProvider.ID;
  }

  @Override
  public List<? extends Item> list() {
    return null;
  }

  @Override
  public Item get(String name) {
    return null;
  }

  private static boolean applicationMatcher(String key, String applicationName) {
    String regex1 = AliCloudProvider.ID + ":.*:" + applicationName + "-.*";
    String regex2 = AliCloudProvider.ID + ":.*:" + applicationName;
    String regex3 = AliCloudProvider.ID + ":.*:" + applicationName + ":.*";
    return Pattern.matches(regex1, key)
        || Pattern.matches(regex2, key)
        || Pattern.matches(regex3, key);
  }

  Collection<CacheData> resolveRelationshipData(
      CacheData source, String relationship, CacheFilter cacheFilter) {
    Map<String, Collection<String>> relationships = source.getRelationships();
    Collection<String> keys = relationships.get(relationship);
    if (!keys.isEmpty()) {
      return cacheView.getAll(relationship, keys, null);
    } else {
      return new ArrayList<CacheData>();
    }
  }

  private LoadBalancerServerGroup createLoadBalancerServerGroup(
      Collection<CacheData> allHealthyDatas,
      String loadBalancerId,
      CacheData applicationServerGroup) {
    LoadBalancerServerGroup loadBalancerServerGroup = new LoadBalancerServerGroup();
    Map<String, Object> attributes = applicationServerGroup.getAttributes();
    loadBalancerServerGroup.setName(String.valueOf(attributes.get("name")));
    loadBalancerServerGroup.setCloudProvider(AliCloudProvider.ID);
    loadBalancerServerGroup.setRegion(String.valueOf(attributes.get("region")));
    loadBalancerServerGroup.setAccount(String.valueOf(attributes.get("account")));
    Map<String, Object> scalingGroup = (Map) attributes.get("scalingGroup");
    String lifecycleState = (String) scalingGroup.get("lifecycleState");
    if (SURVIVE_STATUS.equals(lifecycleState)) {
      loadBalancerServerGroup.setIsDisabled(false);
    } else {
      loadBalancerServerGroup.setIsDisabled(true);
    }
    Set<String> detachedInstances = new HashSet<>();
    Set<LoadBalancerInstance> loadBalancerInstances = new HashSet<>();
    List<Map> instances = (List<Map>) attributes.get("instances");
    for (Map instance : instances) {
      Object id = instance.get("instanceId");
      if (id != null) {
        String instanceId = String.valueOf(id);
        String healthStatus = (String) instance.get("healthStatus");
        boolean flag = "Healthy".equals(healthStatus);
        Map<String, Object> health = new HashMap<>();
        health.put("type", provider.getDisplayName());
        health.put("healthClass", "platform");
        List<String> loadBalancerIds =
            new ArrayList<String>() {
              {
                add(loadBalancerId);
              }
            };
        HealthState healthState =
            HealthHelper.judgeInstanceHealthyState(allHealthyDatas, loadBalancerIds, instanceId);
        health.put(
            "state",
            !"Active".equals(lifecycleState)
                ? "unhealthy"
                : !flag
                    ? "unhealthy"
                    : healthState.equals(HealthState.Up)
                        ? "healthy"
                        : healthState.equals(HealthState.Unknown) ? "unknown" : "unhealthy");
        String zone = (String) instance.get("creationType");
        LoadBalancerInstance loadBalancerInstance = new LoadBalancerInstance();
        loadBalancerInstance.setId(instanceId);
        loadBalancerInstance.setName(instanceId);
        loadBalancerInstance.setZone(zone);
        loadBalancerInstance.setHealth(health);
        loadBalancerInstances.add(loadBalancerInstance);
        detachedInstances.add(instanceId);
      }
    }
    // loadBalancerServerGroup.setDetachedInstances(detachedInstances);
    loadBalancerServerGroup.setInstances(loadBalancerInstances);
    return loadBalancerServerGroup;
  }

  private void addServerGroupToLoadBalancer(
      Collection<CacheData> allHealthyDatas,
      AliCloudLoadBalancer loadBalancer,
      CacheData applicationServerGroup) {
    Set<LoadBalancerServerGroup> serverGroups =
        loadBalancer.getServerGroups() != null ? loadBalancer.getServerGroups() : new HashSet<>();
    LoadBalancerServerGroup serverGroup =
        createLoadBalancerServerGroup(
            allHealthyDatas, loadBalancer.getLoadBalancerId(), applicationServerGroup);
    serverGroups.add(serverGroup);
    loadBalancer.setServerGroups(serverGroups);
  }

  class ResultDetails implements Details {
    Map results;

    public Map getResults() {
      return results;
    }

    public void setResults(Map results) {
      this.results = results;
    }
  }

  private String getApplicationByName(String name) {
    AliCloudLoadBalancer loadBalancer = new AliCloudLoadBalancer(null, null, name, null, null);
    return loadBalancer.getMoniker().getApp();
  }

  private Collection<CacheData> getServerGroupCacheDataByApplication(String applicationName) {
    CacheData application = cacheView.get(APPLICATIONS.ns, Keys.getApplicationKey(applicationName));
    Collection<CacheData> applicationServerGroups = new ArrayList<>();
    if (application != null) {
      applicationServerGroups =
          resolveRelationshipData(
              application,
              SERVER_GROUPS.ns,
              RelationshipCacheFilter.include(INSTANCES.ns, LOAD_BALANCERS.ns));
    }
    return applicationServerGroups;
  }
}
