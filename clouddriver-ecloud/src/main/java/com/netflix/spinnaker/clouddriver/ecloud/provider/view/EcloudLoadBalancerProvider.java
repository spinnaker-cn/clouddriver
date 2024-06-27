package com.netflix.spinnaker.clouddriver.ecloud.provider.view;

import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.APPLICATIONS;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.LOAD_BALANCERS;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancer;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerListener;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerPool;
import com.netflix.spinnaker.clouddriver.model.LoadBalancerProvider;
import com.netflix.spinnaker.clouddriver.model.LoadBalancerServerGroup;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
    loadBalancer.setServerGroups(getLoadBalancerServerGroup(cacheData));
    return loadBalancer;
  }

  private Set<LoadBalancerServerGroup> getLoadBalancerServerGroup(CacheData loadBalancerCache) {
    Object originServerGroupsObj = loadBalancerCache.getAttributes().get("serverGroups");
    if (originServerGroupsObj != null) {
      try {
        String jsonString = objectMapper.writeValueAsString(originServerGroupsObj);
        List<EcloudLoadBalancerPool> originServerGroups =
            objectMapper.readValue(
                jsonString, new TypeReference<List<EcloudLoadBalancerPool>>() {});

        return new HashSet<>(originServerGroups);
      } catch (JsonProcessingException e) {
        log.error(e.getMessage());
      }
    }
    return new HashSet<>();
  }

  @Override
  public String getCloudProvider() {
    return EcloudProvider.ID;
  }

  @Override
  public List<? extends Item> list() {
    log.info("Enter list loadBalancer");
    String searchKey = Keys.getLoadBalancerKey("*", "*", "*");
    Collection<String> identifiers = cacheView.filterIdentifiers(LOAD_BALANCERS.ns, searchKey);
    return getSummaryForLoadBalancers(identifiers).values().stream().collect(Collectors.toList());
  }

  @Override
  public Item get(String id) {
    log.info("Enter Get loadBalancer id={}", id);
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
      lbDetail.setLoadBalancerVip((String) it.getAttributes().get("privateIp"));
      lbDetail.setListeners((List<EcloudLoadBalancerListener>) it.getAttributes().get("listeners"));
      lbDetail.setServerGroups(
          (new HashSet<>((List<EcloudLoadBalancerPool>) it.getAttributes().get("serverGroups"))));
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

    private String loadBalancerVip;

    private List<EcloudLoadBalancerListener> listeners;

    private Set<LoadBalancerServerGroup> serverGroups;
  }
}
