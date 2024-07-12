package com.netflix.spinnaker.clouddriver.ecloud.provider.view;

import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.APPLICATIONS;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.LOAD_BALANCERS;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.SERVER_GROUPS;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancer;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerPool;
import com.netflix.spinnaker.clouddriver.model.LoadBalancerProvider;
import com.netflix.spinnaker.clouddriver.model.LoadBalancerServerGroup;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-09
 */
@Slf4j
@Component
public class EcloudLoadBalancerProvider implements LoadBalancerProvider<EcloudLoadBalancer> {
  private Cache cacheView;

  ObjectMapper objectMapper;

  public EcloudLoadBalancerProvider(Cache cacheView, ObjectMapper objectMapper) {
    this.cacheView = cacheView;
    this.objectMapper = objectMapper;
  }

  public Set<EcloudLoadBalancer> getAll() {
    return getAllMatchingKeyPattern(Keys.getLoadBalancerKey("*", "*", "*"));
  }

  private Set<EcloudLoadBalancer> getAllMatchingKeyPattern(String pattern) {
    log.info("Enter getAllMatchingKeyPattern patten = " + pattern);
    return loadResults(cacheView.filterIdentifiers(LOAD_BALANCERS.ns, pattern));
  }

  private Set<EcloudLoadBalancer> loadResults(Collection<String> identifiers) {
    log.info("Enter loadResults id = " + identifiers);
    Collection<CacheData> data =
        cacheView.getAll(LOAD_BALANCERS.ns, identifiers, RelationshipCacheFilter.none());
    Set<EcloudLoadBalancer> transformed =
        data.stream().map(this::fromCacheData).collect(Collectors.toSet());
    return transformed;
  }

  private EcloudLoadBalancer fromCacheData(CacheData cacheData) {
    EcloudLoadBalancer loadBalancer =
        objectMapper.convertValue(cacheData.getAttributes(), EcloudLoadBalancer.class);
    LoadBalancerServerGroup lb = getLoadBalancerServerGroups(cacheData);
    if (lb != null) {
      loadBalancer.getServerGroups().add(lb);
    }
    return loadBalancer;
  }

  //  private Set<LoadBalancerServerGroup> getLoadBalancerServerGroup(CacheData loadBalancerCache) {
  //    Object originServerGroupsObj = loadBalancerCache.getAttributes().get("serverGroups");
  //    if (originServerGroupsObj != null) {
  //      try {
  //        String jsonString = objectMapper.writeValueAsString(originServerGroupsObj);
  //        List<EcloudLoadBalancerPool> originServerGroups =
  //          objectMapper.readValue(
  //            jsonString, new TypeReference<List<EcloudLoadBalancerPool>>() {});
  //
  //        return new HashSet<>(originServerGroups);
  //      } catch (JsonProcessingException e) {
  //        log.error(e.getMessage());
  //      }
  //    }
  //    return new HashSet<>();
  //  }

  public LoadBalancerServerGroup getLoadBalancerServerGroups(CacheData loadBalancerCache) {
    Collection<String> serverGroupKeys = loadBalancerCache.getRelationships().get(SERVER_GROUPS.ns);
    if (serverGroupKeys != null && !serverGroupKeys.isEmpty()) {
      // basically a copy work
      // only pick the first one and still dont know why
      String serverGroupKey = new ArrayList<>(serverGroupKeys).get(0);
      Map<String, String> parts = Keys.parse(serverGroupKey);
      LoadBalancerServerGroup lbServerGroup = new LoadBalancerServerGroup();
      lbServerGroup.setCloudProvider(EcloudProvider.ID);
      lbServerGroup.setAccount(parts.get("account"));
      lbServerGroup.setName(parts.get("name"));
      lbServerGroup.setRegion(parts.get("region"));
      //      Set<LoadBalancerInstance> lbInstances = new HashSet<>();
      //      List<EcloudLoadBalancerMember> allMemebers = new ArrayList<>();
      //      Object originServerGroupsObj = loadBalancerCache.getAttributes().get("pools");
      //      if (originServerGroupsObj != null) {
      //        try {
      //           List<EcloudLoadBalancerPool> pools =
      // objectMapper.readValue(objectMapper.writeValueAsString(originServerGroupsObj),
      //            new TypeReference<List<EcloudLoadBalancerPool>>() {
      //            });
      //          if (!CollectionUtils.isEmpty(pools)) {
      //            for (EcloudLoadBalancerPool pool : pools) {
      //              if (!CollectionUtils.isEmpty(pool.getMembers())) {
      //                allMemebers.addAll(pool.getMembers());
      //              }
      //            }
      //
      //          }
      //        } catch (JsonProcessingException e) {
      //          log.error(e.getMessage());
      //        }
      //        for (EcloudLoadBalancerMember member : allMemebers) {
      //          LoadBalancerInstance lbInstance = new LoadBalancerInstance();
      //          lbInstance.setId(member.getId());
      //          lbInstance.setName(member.getVmName());
      //          lbInstance.setZone(member.getRegion());
      //          lbInstances.add(lbInstance);
      //          break;
      //        }
      //        lbServerGroup.setInstances(lbInstances);
      //      }
      return lbServerGroup;
    }
    return null;
  }

  @Override
  public String getCloudProvider() {
    return EcloudProvider.ID;
  }

  @Override
  public List<? extends Item> list() {
    String searchKey = Keys.getLoadBalancerKey("*", "*", "*");
    Collection<String> identifiers = cacheView.filterIdentifiers(LOAD_BALANCERS.ns, searchKey);
    return getSummaryForLoadBalancers(identifiers).values().stream().collect(Collectors.toList());
  }

  @Override
  public Item get(String id) {
    String searchKey = Keys.getLoadBalancerKey(id, "*", "*");
    Collection<String> identifiers =
        cacheView.filterIdentifiers(LOAD_BALANCERS.ns, searchKey).stream()
            .filter(
                it -> {
                  Map<String, String> keyMap = Keys.parse(it);
                  return id.equals(keyMap.get("id"));
                })
            .collect(Collectors.toList());
    return getSummaryForLoadBalancers(identifiers).get(id);
  }

  private Map<String, ECloudLoadBalancerSummary> getSummaryForLoadBalancers(
      Collection<String> loadBalancerKeys) {
    Map<String, ECloudLoadBalancerSummary> map = new HashMap<>();
    Collection<CacheData> loadBalancerData = cacheView.getAll(LOAD_BALANCERS.ns, loadBalancerKeys);
    if (loadBalancerData != null) {
      Map<String, CacheData> loadBalancers =
          loadBalancerData.stream().collect(Collectors.toMap(CacheData::getId, it -> it));
      for (String lb : loadBalancerKeys) {
        CacheData loadBalancerFromCache = loadBalancers.get(lb);
        if (loadBalancerFromCache != null) {
          Map<String, String> parts = Keys.parse(lb);
          Map<String, Object> attributes = loadBalancerFromCache.getAttributes();
          String name = "";
          if (attributes != null && !attributes.isEmpty()) {
            name = (String) loadBalancerFromCache.getAttributes().get("name");
          }

          String region = parts.get("region");
          String account = parts.get("account");

          ECloudLoadBalancerSummary summary = map.get(name);
          if (summary == null) {
            summary = new ECloudLoadBalancerSummary(name);
            map.put(name, summary);
          }

          ECloudLoadBalancerDetail loadBalancer = new ECloudLoadBalancerDetail();
          loadBalancer.setAccount(parts.get("account"));
          loadBalancer.setRegion(parts.get("region"));
          loadBalancer.setId(parts.get("id"));
          loadBalancer.setSubnetId((String) loadBalancerFromCache.getAttributes().get("subnetId"));
          loadBalancer.setVpcId((String) loadBalancerFromCache.getAttributes().get("vpcId"));
          loadBalancer.setName((String) loadBalancerFromCache.getAttributes().get("name"));
          loadBalancer.setPools(
              (List<EcloudLoadBalancerPool>) loadBalancerFromCache.getAttributes().get("pools"));
          summary
              .getOrCreateAccount(account)
              .getOrCreateRegion(region)
              .getLoadBalancers()
              .add(loadBalancer);
        }
      }
    }
    return map;
  }

  @Override
  public List<? extends Details> byAccountAndRegionAndName(
      String account, String region, String id) {
    log.info(
        "Get loadBalancer byAccountAndRegionAndName: account={},region={},id={}",
        account,
        region,
        id);
    String lbKey = Keys.getLoadBalancerKey(id, account, region);
    Collection<CacheData> lbCache = cacheView.getAll(LOAD_BALANCERS.ns, lbKey);
    List<ECloudLoadBalancerDetail> lbDetails = new ArrayList<>();
    for (CacheData it : lbCache) {
      ECloudLoadBalancerDetail lbDetail = new ECloudLoadBalancerDetail();
      lbDetail.setId((String) it.getAttributes().get("id"));
      lbDetail.setName((String) it.getAttributes().get("name"));
      lbDetail.setAccount((String) it.getAttributes().get("account"));
      lbDetail.setRegion((String) it.getAttributes().get("region"));
      lbDetail.setSubnetId((String) it.getAttributes().get("subnetId"));
      lbDetail.setVpcId((String) it.getAttributes().get("vpcId"));
      lbDetail.setCreateTime((String) it.getAttributes().get("createdTime"));
      lbDetail.setPools((List<EcloudLoadBalancerPool>) it.getAttributes().get("pools"));
      lbDetails.add(lbDetail);
    }
    return lbDetails;
  }

  @Override
  public Set<EcloudLoadBalancer> getApplicationLoadBalancers(String applicationName) {
    log.info("Enter hecloud getApplicationLoadBalancers " + applicationName);
    CacheData application =
        cacheView.get(
            APPLICATIONS.ns,
            Keys.getApplicationKey(applicationName),
            RelationshipCacheFilter.include(LOAD_BALANCERS.ns));
    if (application != null) {
      Collection<String> loadBalancerKeys = application.getRelationships().get(LOAD_BALANCERS.ns);
      if (loadBalancerKeys != null) {
        Collection<CacheData> loadBalancers = cacheView.getAll(LOAD_BALANCERS.ns, loadBalancerKeys);
        Set<EcloudLoadBalancer> loadBalancerSet =
            translateLoadBalancersFromCacheData(loadBalancers);
        loadBalancerSet.forEach(
            it -> {
              it.setApplication(applicationName);
            });
        return loadBalancerSet;
      }
    }
    return null;
  }

  private Set<EcloudLoadBalancer> translateLoadBalancersFromCacheData(
      Collection<CacheData> loadBalancerData) {
    Set<EcloudLoadBalancer> transformed =
        loadBalancerData.stream().map(this::fromCacheData).collect(Collectors.toSet());
    return transformed;
  }

  @Data
  static class ECloudLoadBalancerSummary implements LoadBalancerProvider.Item {
    private Map<String, ECloudLoadBalancerAccount> mappedAccounts = new HashMap<>();

    private String name;

    public ECloudLoadBalancerSummary(String name) {
      this.name = name;
    }

    public ECloudLoadBalancerAccount getOrCreateAccount(String name) {
      if (!mappedAccounts.containsKey(name)) {
        mappedAccounts.put(name, new ECloudLoadBalancerAccount(name));
      }

      return mappedAccounts.get(name);
    }

    @Override
    @JsonProperty("accounts")
    public List<ECloudLoadBalancerAccount> getByAccounts() {
      return new ArrayList<>(mappedAccounts.values());
    }
  }

  @Data
  static class ECloudLoadBalancerAccount implements LoadBalancerProvider.ByAccount {
    private Map<String, ECloudLoadBalancerAccountRegion> mappedRegions = new HashMap<>();

    private String name;

    public ECloudLoadBalancerAccount(String name) {
      this.name = name;
    }

    public ECloudLoadBalancerAccountRegion getOrCreateRegion(String name) {
      if (!mappedRegions.containsKey(name)) {
        mappedRegions.put(name, new ECloudLoadBalancerAccountRegion(name, new ArrayList<>()));
      }
      return mappedRegions.get(name);
    }

    @Override
    @JsonProperty("regions")
    public List<ECloudLoadBalancerAccountRegion> getByRegions() {
      return new ArrayList<>(mappedRegions.values());
    }
  }

  @Data
  static class ECloudLoadBalancerAccountRegion implements LoadBalancerProvider.ByRegion {
    private String name;

    private List<ECloudLoadBalancerDetail> loadBalancers;

    public ECloudLoadBalancerAccountRegion(
        String name, ArrayList<ECloudLoadBalancerDetail> objects) {
      this.name = name;
      this.loadBalancers = objects;
    }
  }

  @Data
  static class ECloudLoadBalancerDetail implements LoadBalancerProvider.Details {
    private String account;

    private String region;

    private String name;

    private String id; // locadBalancerId

    private String type = EcloudProvider.ID;

    private String subnetId;

    private String vpcId;

    private String createTime;

    private List<EcloudLoadBalancerPool> pools;
  }
}
