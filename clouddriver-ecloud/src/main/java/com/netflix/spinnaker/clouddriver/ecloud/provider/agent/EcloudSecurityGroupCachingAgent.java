package com.netflix.spinnaker.clouddriver.ecloud.provider.agent;

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE;
import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.ON_DEMAND;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.SECURITY_GROUPS;

import com.ecloud.sdk.vpc.v1.model.ListSecGroupResponse;
import com.ecloud.sdk.vpc.v1.model.ListSecGroupResponseContent;
import com.ecloud.sdk.vpc.v1.model.ListSecurityGroupRuleResponseContent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.cats.agent.AccountAware;
import com.netflix.spinnaker.cats.agent.AgentDataType;
import com.netflix.spinnaker.cats.agent.CacheResult;
import com.netflix.spinnaker.cats.agent.CachingAgent;
import com.netflix.spinnaker.cats.agent.DefaultCacheResult;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.DefaultCacheData;
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter;
import com.netflix.spinnaker.cats.provider.ProviderCache;
import com.netflix.spinnaker.clouddriver.cache.OnDemandAgent;
import com.netflix.spinnaker.clouddriver.cache.OnDemandMetricsSupport;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.client.EcloudVirtualPrivateCloudClient;
import com.netflix.spinnaker.clouddriver.ecloud.exception.EcloudException;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudSecurityGroupDescription;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudSecurityGroupRule;
import com.netflix.spinnaker.clouddriver.ecloud.provider.EcloudSearchableProvider;
import com.netflix.spinnaker.clouddriver.ecloud.security.EcloudCredentials;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.CollectionUtils;

@Slf4j
public class EcloudSecurityGroupCachingAgent implements CachingAgent, OnDemandAgent, AccountAware {
  private final ObjectMapper objectMapper;
  private final String region;
  private final String accountName;
  private final EcloudCredentials credentials;
  private final Registry registry;
  private final OnDemandMetricsSupport metricsSupport;
  private String onDemandAgentType;

  private final Set<AgentDataType> providedDataTypes =
      Collections.singleton(AUTHORITATIVE.forType(SECURITY_GROUPS.ns));

  public EcloudSecurityGroupCachingAgent(
      EcloudProvider ecloudProvider,
      EcloudCredentials creds,
      ObjectMapper objectMapper,
      Registry registry,
      String region) {
    this.accountName = creds.getName();
    this.credentials = creds;
    this.region = region;
    this.objectMapper = objectMapper;
    this.registry = registry;
    this.onDemandAgentType = getAgentType() + "-OnDemand";
    this.metricsSupport =
        new OnDemandMetricsSupport(
            registry,
            this,
            ecloudProvider.getId() + ":" + OnDemandAgent.OnDemandType.SecurityGroup);
  }

  @Override
  public Collection<AgentDataType> getProvidedDataTypes() {
    return providedDataTypes;
  }

  @Override
  public CacheResult loadData(ProviderCache providerCache) {
    long currentTime = System.currentTimeMillis();
    Set<EcloudSecurityGroupDescription> securityGroupDescSet = loadSecurityGroupAll();
    log.info(
        "Total SecurityGroup Number = "
            + securityGroupDescSet.size()
            + " in "
            + this.getAgentType());
    return buildCacheResult(providerCache, securityGroupDescSet, currentTime, null, null);
  }

  @Override
  public String getAgentType() {
    return credentials.getName() + "/" + region + "/" + this.getClass().getSimpleName();
  }

  @Override
  public String getProviderName() {
    return EcloudSearchableProvider.class.getName();
  }

  @Override
  public String getOnDemandAgentType() {
    return onDemandAgentType;
  }

  @Override
  public OnDemandMetricsSupport getMetricsSupport() {
    return metricsSupport;
  }

  @Override
  public boolean handles(OnDemandType type, String cloudProvider) {
    return type == OnDemandAgent.OnDemandType.SecurityGroup
        && cloudProvider.equals(EcloudProvider.ID);
  }

  @Nullable
  @Override
  public OnDemandResult handle(ProviderCache providerCache, Map<String, ?> data) {
    log.info("Enter ECloudSecurityGroupCachingAgent handle, params = " + data);
    if (!data.containsKey("securityGroupId")
        || !data.containsKey("account")
        || !data.containsKey("region")
        || !accountName.equals(data.get("account"))
        || !region.equals(data.get("region"))) {
      log.info("ECloudSecurityGroupCachingAgent: input params error!");
      return null;
    }

    EcloudSecurityGroupDescription updatedSecurityGroup = null;
    AtomicReference<EcloudSecurityGroupDescription> evictedSecurityGroup = null;
    String securityGroupId = String.valueOf(data.get("securityGroupId"));

    updatedSecurityGroup = metricsSupport.readData(() -> loadSecurityGroupById(securityGroupId));
    // todo:
    if (updatedSecurityGroup == null) {
      log.info(
          "ECloudSecurityGroupCachingAgent: Can not find securityGroup "
              + securityGroupId
              + " in "
              + region);
      return null;
    }

    EcloudSecurityGroupDescription finalUpdatedSecurityGroup = updatedSecurityGroup;
    CacheResult cacheResult =
        metricsSupport.transformData(
            () -> {
              if (finalUpdatedSecurityGroup != null) {
                return buildCacheResult(providerCache, null, 0, finalUpdatedSecurityGroup, null);
              } else {
                EcloudSecurityGroupDescription eCloudSecurityGroupDescription =
                    new EcloudSecurityGroupDescription(
                        securityGroupId, "unknown", System.currentTimeMillis());
                evictedSecurityGroup.set(eCloudSecurityGroupDescription);
                return buildCacheResult(providerCache, null, 0, null, evictedSecurityGroup.get());
              }
            });
    Map<String, Collection<String>> evictions = new HashMap<>();
    if (evictedSecurityGroup.get() != null) {
      evictions.put(
          SECURITY_GROUPS.ns,
          Collections.singleton(
              Keys.getSecurityGroupKey(
                  evictedSecurityGroup.get().getSecurityGroupId(),
                  evictedSecurityGroup.get().getSecurityGroupName(),
                  accountName,
                  region)));
    }

    log.info(
        "HCloudSecurityGroupCachingAgent: onDemand cache refresh (data: "
            + data
            + ", evictions: "
            + evictions
            + ")");

    return new OnDemandAgent.OnDemandResult(getAgentType(), cacheResult, evictions);
  }

  private CacheResult buildCacheResult(
      ProviderCache providerCache,
      Collection<EcloudSecurityGroupDescription> securityGroups,
      long lastReadTime,
      EcloudSecurityGroupDescription updatedSecurityGroup,
      EcloudSecurityGroupDescription evictedSecurityGroup) {
    if (securityGroups != null && !securityGroups.isEmpty()) {
      List<CacheData> data = new ArrayList<>();
      Collection<String> identifiers =
          providerCache.filterIdentifiers(
              ON_DEMAND.ns, Keys.getSecurityGroupKey("*", "*", accountName, region));
      Collection<CacheData> onDemandCacheResults =
          providerCache.getAll(ON_DEMAND.ns, identifiers, RelationshipCacheFilter.none());

      List<String> evictions = new ArrayList<>();
      Map<String, CacheData> usableOnDemandCacheDatas = new HashMap<>();
      for (CacheData it : onDemandCacheResults) {
        if ((long) it.getAttributes().get("cachedTime") < lastReadTime) {
          evictions.add(it.getId());
        } else {
          usableOnDemandCacheDatas.put(it.getId(), it);
        }
      }

      for (EcloudSecurityGroupDescription item : securityGroups) {
        EcloudSecurityGroupDescription securityGroup = item;
        String sgKey =
            Keys.getSecurityGroupKey(
                securityGroup.getSecurityGroupId(),
                securityGroup.getSecurityGroupName(),
                accountName,
                region);

        CacheData onDemandSG = usableOnDemandCacheDatas.get(sgKey);
        if (onDemandSG != null) {
          if ((long) onDemandSG.getAttributes().get("cachedTime")
              > securityGroup.getLastReadTime()) {
            try {
              securityGroup =
                  objectMapper.readValue(
                      (String) onDemandSG.getAttributes().get("securityGroup"),
                      EcloudSecurityGroupDescription.class);
            } catch (JsonProcessingException e) {
              throw new EcloudException(e.getMessage(), e);
            }
          } else {
            securityGroup = null;
          }
          usableOnDemandCacheDatas.remove(sgKey);
        }
        if (securityGroup != null) {
          data.add(buildCacheData(securityGroup));
        }
      }

      log.info("Caching " + data.size() + " items in " + this.getAgentType());
      Map<String, Collection<CacheData>> nsDataMap = new HashMap<>();
      nsDataMap.put(SECURITY_GROUPS.ns, data);
      Map<String, Collection<String>> nsEvictionsMap = new HashMap<>();
      nsEvictionsMap.put(ON_DEMAND.ns, evictions);
      return new DefaultCacheResult(nsDataMap, nsEvictionsMap);
    } else {
      if (updatedSecurityGroup != null) {
        if (updateCache(providerCache, updatedSecurityGroup, "OnDemandUpdated")) {
          CacheData data = buildCacheData(updatedSecurityGroup);
          log.info("Caching 1 OnDemand updated item in " + this.getAgentType());
          return new DefaultCacheResult(
              Collections.singletonMap(SECURITY_GROUPS.ns, Collections.singletonList(data)));
        } else {
          return null;
        }
      }

      if (evictedSecurityGroup != null) {
        if (updateCache(providerCache, evictedSecurityGroup, "OnDemandEvicted")) {
          log.info("Caching 1 OnDemand evicted item in " + this.getAgentType());
          return new DefaultCacheResult(
              Collections.singletonMap(SECURITY_GROUPS.ns, Collections.emptyList()));
        } else {
          return null;
        }
      }
    }

    return new DefaultCacheResult(
        Collections.singletonMap(SECURITY_GROUPS.ns, Collections.emptyList()));
  }

  private CacheData buildCacheData(EcloudSecurityGroupDescription securityGroup) {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(SECURITY_GROUPS.ns, securityGroup);
    String key =
        Keys.getSecurityGroupKey(
            securityGroup.getSecurityGroupId(),
            securityGroup.getSecurityGroupName(),
            accountName,
            region);
    return new DefaultCacheData(key, attributes, new HashMap<>());
  }

  @Override
  public Collection<Map> pendingOnDemandRequests(ProviderCache providerCache) {
    List<Map> resultList = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    resultList.add(map);
    return resultList;
  }

  @Override
  public String getAccountName() {
    return credentials.getName();
  }

  private EcloudSecurityGroupDescription loadSecurityGroupById(String securityGroupId) {
    EcloudVirtualPrivateCloudClient vpcClient =
        new EcloudVirtualPrivateCloudClient(credentials, region);

    ListSecGroupResponse listSecGroupResponse = vpcClient.getSecurityGroupById(securityGroupId);
    long currentTime = System.currentTimeMillis();
    if (listSecGroupResponse != null
        && !CollectionUtils.isEmpty(listSecGroupResponse.getBody().getContent())) {
      ListSecGroupResponseContent securityGroup =
          listSecGroupResponse.getBody().getContent().get(0);
      EcloudSecurityGroupDescription securityGroupDesc =
          new EcloudSecurityGroupDescription(
              securityGroup.getId(), securityGroup.getName(), currentTime);
      setInRuleAndOutRule(vpcClient, securityGroupDesc.getSecurityGroupId(), securityGroupDesc);
      return securityGroupDesc;
    }
    return null;
  }

  private Boolean updateCache(
      ProviderCache providerCache,
      EcloudSecurityGroupDescription securityGroup,
      String onDemandCacheType) {
    final Boolean[] foundUpdatedOnDemandSG = {false};
    if (securityGroup != null) {
      // Get the current list of all OnDemand requests from the cache
      String securityGroupKey =
          Keys.getSecurityGroupKey(
              securityGroup.getSecurityGroupId(),
              securityGroup.getSecurityGroupName(),
              accountName,
              region);
      Collection<CacheData> cacheResults =
          providerCache.getAll(ON_DEMAND.ns, Collections.singletonList(securityGroupKey));
      if (cacheResults != null && !cacheResults.isEmpty()) {
        cacheResults.forEach(
            it -> {
              // cacheResults.forEach should only return one item which is matching the given
              // security group details
              if (Long.parseLong(it.getAttributes().get("cachedTime") + "")
                  > securityGroup.getLastReadTime()) {
                // Found a newer matching entry in the cache when compared with the current OnDemand
                // request
                foundUpdatedOnDemandSG[0] = true;
              }
            });
      }

      if (!foundUpdatedOnDemandSG[0]) {
        Map<String, Object> attribs = new HashMap<>();
        try {
          attribs.put("securityGroup", objectMapper.writeValueAsString(securityGroup));
        } catch (JsonProcessingException e) {
          log.error(e.getMessage(), e);
        }
        attribs.put("cachedTime", securityGroup.getLastReadTime());
        attribs.put("onDemandCacheType", onDemandCacheType);
        DefaultCacheData cacheData =
            new DefaultCacheData(securityGroupKey, attribs, new HashMap<>());
        providerCache.putCacheData(ON_DEMAND.ns, cacheData);
        return true;
      }
    }
    return false;
  }

  private Set<EcloudSecurityGroupDescription> loadSecurityGroupAll() {
    EcloudVirtualPrivateCloudClient vpcClient =
        new EcloudVirtualPrivateCloudClient(credentials, region);
    List<ListSecGroupResponseContent> securityGroupList = vpcClient.getSecurityGroupsAll();
    Set<EcloudSecurityGroupDescription> securityGroupDescriptionSet = new HashSet<>();

    for (ListSecGroupResponseContent sg : securityGroupList) {
      EcloudSecurityGroupDescription securityGroupDesc = new EcloudSecurityGroupDescription();
      securityGroupDesc.setSecurityGroupId(sg.getId());
      securityGroupDesc.setSecurityGroupName(sg.getName());
      securityGroupDesc.setSecurityGroupDesc(sg.getDescription());
      setInRuleAndOutRule(vpcClient, securityGroupDesc.getSecurityGroupId(), securityGroupDesc);
      securityGroupDesc.setLastReadTime(System.currentTimeMillis());
      securityGroupDescriptionSet.add(securityGroupDesc);
    }

    return securityGroupDescriptionSet;
  }

  private void setInRuleAndOutRule(
      EcloudVirtualPrivateCloudClient vpcClient,
      String securityGroupId,
      EcloudSecurityGroupDescription securityGroupDesc) {
    List<ListSecurityGroupRuleResponseContent> securityGroupRules =
        vpcClient.getSecurityGroupPolicies(securityGroupId).getBody().getContent();
    List<ListSecurityGroupRuleResponseContent> ingressRules = new ArrayList<>();
    List<ListSecurityGroupRuleResponseContent> egressRules = new ArrayList<>();
    for (ListSecurityGroupRuleResponseContent rule : securityGroupRules) {
      String directionValue = rule.getDirection().getValue();
      if ("ingress".equals(directionValue)) {
        ingressRules.add(rule);
      } else if ("egress".equals(directionValue)) {
        egressRules.add(rule);
      }
    }
    securityGroupDesc.setInRules(
        ingressRules.stream()
            .map(
                ingress -> {
                  EcloudSecurityGroupRule inRule = new EcloudSecurityGroupRule(ingress.getId());
                  inRule.setProtocol(ingress.getProtocol().getValue());
                  inRule.setMinPortRange(ingress.getMinPortRange());
                  inRule.setMaxPortRange(ingress.getMaxPortRange());
                  inRule.setCidrBlock(ingress.getRemoteIpPrefix());
                  inRule.setDirection(ingress.getDirection().getValue());
                  return inRule;
                })
            .collect(Collectors.toList()));

    securityGroupDesc.setOutRules(
        egressRules.stream()
            .map(
                egress -> {
                  EcloudSecurityGroupRule outRule = new EcloudSecurityGroupRule(egress.getId());
                  outRule.setProtocol(egress.getProtocol().getValue());
                  outRule.setMinPortRange(egress.getMinPortRange());
                  outRule.setMaxPortRange(egress.getMaxPortRange());
                  outRule.setCidrBlock(egress.getRemoteIpPrefix());
                  outRule.setDirection(egress.getDirection().getValue());
                  return outRule;
                })
            .collect(Collectors.toList()));
  }
}
