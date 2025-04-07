package com.netflix.spinnaker.clouddriver.ecloud.provider.agent;

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE;
import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.INFORMATIVE;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.APPLICATIONS;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.INSTANCES;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.LOAD_BALANCERS;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.ON_DEMAND;

import com.ecloud.sdk.config.Config;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalanceListenerRespResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalancePoolMemberResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListLoadbalanceResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListPoolRespResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListPoolRespResponseHealthMonitorResp;
import com.ecloud.sdk.vlb.v1.model.ListPoolRespResponseL7PolicyResps;
import com.ecloud.sdk.vpc.v1.model.GetVpcDetailRespByRouterIdResponseBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.cats.agent.AccountAware;
import com.netflix.spinnaker.cats.agent.AgentDataType;
import com.netflix.spinnaker.cats.agent.CacheResult;
import com.netflix.spinnaker.cats.agent.CachingAgent;
import com.netflix.spinnaker.cats.agent.DefaultCacheResult;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.DefaultCacheData;
import com.netflix.spinnaker.cats.provider.ProviderCache;
import com.netflix.spinnaker.clouddriver.cache.OnDemandAgent;
import com.netflix.spinnaker.clouddriver.cache.OnDemandMetricsSupport;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudBasicResource;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancer;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerHealth;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerL7Policy;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerListener;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerMember;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerPool;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerRule;
import com.netflix.spinnaker.clouddriver.ecloud.provider.EcloudSearchableProvider;
import com.netflix.spinnaker.clouddriver.ecloud.provider.view.MutableCacheData;
import com.netflix.spinnaker.clouddriver.ecloud.security.EcloudCredentials;
import com.netflix.spinnaker.clouddriver.ecloud.util.EcloudLbUtil;
import com.netflix.spinnaker.clouddriver.ecloud.util.EcloudVpcUtil;
import com.netflix.spinnaker.clouddriver.names.NamerRegistry;
import com.netflix.spinnaker.moniker.Moniker;
import com.netflix.spinnaker.moniker.Namer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

@Slf4j
public class EcloudLoadbalancerCachingAgent implements CachingAgent, AccountAware, OnDemandAgent {
  private EcloudCredentials account;

  private String region;

  ObjectMapper objectMapper;

  OnDemandMetricsSupport metricsSupport;

  Namer<EcloudBasicResource> namer;

  static final Collection<AgentDataType> types =
      Collections.unmodifiableCollection(
          new ArrayList<AgentDataType>() {
            {
              add(AUTHORITATIVE.forType(LOAD_BALANCERS.ns));
              add(AUTHORITATIVE.forType(APPLICATIONS.ns));
              add(INFORMATIVE.forType(INSTANCES.ns));
            }
          });

  public EcloudLoadbalancerCachingAgent(
      EcloudProvider ecloudProvider,
      String region,
      ObjectMapper objectMapper,
      Registry registry,
      EcloudCredentials credentials) {
    this.account = credentials;
    this.region = region;
    this.objectMapper = objectMapper;
    this.metricsSupport =
        new OnDemandMetricsSupport(
            registry,
            this,
            ecloudProvider.getId()
                + ":"
                + ecloudProvider.getId()
                + ":"
                + OnDemandType.LoadBalancer);
    this.namer =
        NamerRegistry.lookup()
            .withProvider(EcloudProvider.ID)
            .withAccount(credentials.getName())
            .withResource(EcloudBasicResource.class);
  }

  @Override
  public CacheResult loadData(ProviderCache providerCache) {
    List<EcloudLoadBalancer> lbList = loadLoadBalancerData(null);
    if (lbList == null || lbList.isEmpty()) {
      log.warn("lb response data is null");
      return null;
    }
    log.info("Total ECloudLoadbalance Number = {} in {}", lbList.size(), this.getAgentType());
    Long start = System.currentTimeMillis();
    Set<String> loadBalancerKeys =
        lbList.stream()
            .map(lb -> Keys.getLoadBalancerKey(lb.getId(), this.getAccountName(), region))
            .collect(Collectors.toSet());

    Set<String> pendingOnDemandRequestKeys =
        providerCache
            .filterIdentifiers(
                ON_DEMAND.ns, Keys.getLoadBalancerKey("*", this.getAccountName(), region))
            .stream()
            .filter(loadBalancerKeys::contains)
            .collect(Collectors.toSet());

    Collection<CacheData> pendingOnDemandRequestsForLoadBalancer =
        providerCache.getAll(ON_DEMAND.ns, pendingOnDemandRequestKeys);

    List<CacheData> toEvictOnDemandCacheData = new ArrayList<>();
    List<CacheData> toKeepOnDemandCacheData = new ArrayList<>();

    pendingOnDemandRequestsForLoadBalancer.forEach(
        it -> {
          long currentProcessedCount =
              it.getAttributes().get("processedCount") != null
                  ? Long.parseLong(String.valueOf(it.getAttributes().get("processedCount")))
                  : 0L;
          long cacheTime =
              it.getAttributes().get("cacheTime") != null
                  ? Long.parseLong(String.valueOf(it.getAttributes().get("cacheTime")))
                  : 0L;
          if (cacheTime < start && currentProcessedCount > 0) {
            toEvictOnDemandCacheData.add(it);
          } else {
            toKeepOnDemandCacheData.add(it);
          }
        });

    CacheResult result =
        buildCacheResult(lbList, toKeepOnDemandCacheData, toEvictOnDemandCacheData);
    Collection<CacheData> onDemandRequests = result.getCacheResults().get(ON_DEMAND.ns);

    if (onDemandRequests != null) {
      for (CacheData request : onDemandRequests) {
        Map<String, Object> attributes = request.getAttributes();
        long currentProcessedCount =
            attributes.get("processedCount") != null
                ? Long.parseLong(String.valueOf(attributes.get("processedCount")))
                : 0L;
        attributes.put("processedTime", System.currentTimeMillis());
        attributes.put("processedCount", currentProcessedCount + 1);
      }
    }
    return result;
  }

  private CacheResult buildCacheResult(
      List<EcloudLoadBalancer> lbList,
      Collection<CacheData> toKeepOnDemandCacheData,
      Collection<CacheData> toEvictOnDemandCacheData) {

    Map<String, Collection<String>> evictions = new HashMap<>();
    if (toEvictOnDemandCacheData != null && !toEvictOnDemandCacheData.isEmpty()) {
      evictions.put(
          ON_DEMAND.ns,
          toEvictOnDemandCacheData.stream().map(cd -> cd.getId()).collect(Collectors.toList()));
    }

    Map<String, Collection<CacheData>> cacheResults = new HashMap<>();

    Map<String, Map<String, CacheData>> namespaceCache = new HashMap<>();

    for (EcloudLoadBalancer eclb : lbList) {
      Moniker moniker = namer.deriveMoniker(eclb);
      String applicationName = moniker.getApp();
      if (applicationName == null) {
        continue;
      }

      String loadBalancerKey = Keys.getLoadBalancerKey(eclb.getId(), this.getAccountName(), region);
      String appKey = Keys.getApplicationKey(applicationName);
      // Application
      namespaceCache
          .computeIfAbsent(APPLICATIONS.ns, k -> new HashMap<>())
          .computeIfAbsent(appKey, k -> new MutableCacheData(appKey))
          .getAttributes()
          .put("name", applicationName);
      namespaceCache
          .get(APPLICATIONS.ns)
          .get(appKey)
          .getRelationships()
          .computeIfAbsent(LOAD_BALANCERS.ns, k -> new HashSet<>())
          .add(loadBalancerKey);

      // LoadBalancer
      CacheData lbCacheData =
          namespaceCache
              .computeIfAbsent(LOAD_BALANCERS.ns, k -> new HashMap<>())
              .computeIfAbsent(loadBalancerKey, k -> new MutableCacheData(loadBalancerKey));
      Map<String, Object> attributes = objectMapper.convertValue(eclb, Map.class);
      attributes.put("provider", EcloudProvider.ID);
      attributes.put("account", account.getName());
      attributes.put("poolId", region);
      attributes.put("application", applicationName);
      attributes.put("loadBalacnerVips", eclb.getPrivateIp());
      lbCacheData.getAttributes().putAll(attributes);

      // List<String> securityGroups =
      // eclb().stream().map(String::new).collect(Collectors.toList());
      // attributes.put("securityGroups", securityGroups);

      lbCacheData
          .getRelationships()
          .computeIfAbsent(APPLICATIONS.ns, k -> new HashSet<>())
          .add(appKey);
    }
    namespaceCache.forEach(
        (namespace, cacheDataMap) -> {
          cacheResults.put(namespace, new ArrayList<>(cacheDataMap.values()));
        });
    cacheResults.put(ON_DEMAND.ns, toKeepOnDemandCacheData);

    return new DefaultCacheResult(cacheResults, evictions);
  }

  @Override
  public Collection<AgentDataType> getProvidedDataTypes() {
    return types;
  }

  @Nullable
  @Override
  public OnDemandResult handle(ProviderCache providerCache, Map<String, ?> data) {
    try {
      if (!data.containsKey("loadBalancerId")
          || !data.containsKey("account")
          || !data.containsKey("region")
          || !this.getAccountName().equals(data.get("accountName"))
          || !this.region.equals(data.get("region"))) {
        return null;
      }

      EcloudLoadBalancer loadBalancer =
          metricsSupport.readData(
              () -> {
                List<EcloudLoadBalancer> loadBalancers =
                    loadLoadBalancerData(String.valueOf(data.get("loadBalancerId")));
                return loadBalancers.isEmpty() ? null : loadBalancers.get(0);
              });

      if (loadBalancer == null) {
        log.info("Can not find loadBalancer " + data.get("loadBalancerId"));
        return null;
      }

      CacheResult cacheResult =
          metricsSupport.transformData(
              () -> {
                return buildCacheResult(Collections.singletonList(loadBalancer), null, null);
              });

      String cacheResultAsJson = objectMapper.writeValueAsString(cacheResult.getCacheResults());
      String loadBalancerKey =
          Keys.getLoadBalancerKey(
              String.valueOf(data.get("loadBalancerId")), this.getAccountName(), this.region);

      if (!cacheResult.getCacheResults().values().stream()
          .flatMap(Collection::stream)
          .findAny()
          .isPresent()) {
        providerCache.evictDeletedItems(ON_DEMAND.ns, Collections.singletonList(loadBalancerKey));
      } else {
        metricsSupport.onDemandStore(
            () -> {
              Map<String, Object> attributes = new HashMap<>();
              attributes.put("cacheTime", new Date());
              attributes.put("cacheResults", cacheResultAsJson);
              CacheData cacheData =
                  new DefaultCacheData(
                      loadBalancerKey, 10 * 60, attributes, Collections.emptyMap());
              providerCache.putCacheData(ON_DEMAND.ns, cacheData);
              return null;
            });
      }

      Map<String, Collection<String>> evictions = new HashMap<>();
      evictions.put(LOAD_BALANCERS.ns, Collections.singletonList(loadBalancerKey));
      return new OnDemandResult(getOnDemandAgentType(), cacheResult, evictions);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return null;
  }

  @Override
  public String getAccountName() {
    return account.getName();
  }

  @Override
  public String getAgentType() {
    return account.getName() + "/" + region + "/" + this.getClass().getSimpleName();
  }

  @Override
  public String getProviderName() {
    return EcloudSearchableProvider.class.getName();
  }

  @Override
  public String getOnDemandAgentType() {
    return this.getAgentType() + "-OnDemand";
  }

  @Override
  public OnDemandMetricsSupport getMetricsSupport() {
    return null;
  }

  @Override
  public boolean handles(OnDemandType type, String cloudProvider) {
    return type == OnDemandType.LoadBalancer && Objects.equals(cloudProvider, EcloudProvider.ID);
  }

  @Override
  public Collection<Map> pendingOnDemandRequests(ProviderCache providerCache) {
    return null;
  }

  private List<EcloudLoadBalancer> loadLoadBalancerData(String loadBalancerId) {
    Config config = new Config();
    config.setAccessKey(this.account.getAccessKey());
    config.setSecretKey(this.account.getSecretKey());
    config.setPoolId(this.region);
    com.ecloud.sdk.vlb.v1.Client lbClient = new com.ecloud.sdk.vlb.v1.Client(config);

    com.ecloud.sdk.vpc.v1.Client vpcClient = new com.ecloud.sdk.vpc.v1.Client(config);

    List<ListLoadbalanceResponseContent> lbList = null;
    if (loadBalancerId != null && !loadBalancerId.isEmpty()) {
      // TODO
    } else {
      lbList = EcloudLbUtil.getAllLoadBalancer(lbClient);
    }

    List<String> lbIds = new ArrayList<>();
    for (ListLoadbalanceResponseContent lb : lbList) {
      lbIds.add(lb.getId());
    }

    List<ListLoadBalanceListenerRespResponseContent> listenerList =
        EcloudLbUtil.getListenerByLbList(lbClient, lbIds);

    List<ListPoolRespResponseContent> poolList = EcloudLbUtil.getPoolByLbList(lbClient, lbIds);

    List<ListLoadBalancePoolMemberResponseContent> memberList =
        EcloudLbUtil.getMemberByPoolIdList(lbClient, poolList);

    Map<String, List<ListLoadBalanceListenerRespResponseContent>> listenerMapGroupByLbId =
        listenerList.stream()
            .collect(
                Collectors.groupingBy(
                    ListLoadBalanceListenerRespResponseContent::getLoadBalanceId));

    Map<String, List<ListPoolRespResponseContent>> poolMapGroupByLbId =
        poolList.stream()
            .collect(Collectors.groupingBy(ListPoolRespResponseContent::getLoadBalanceId));

    Map<String, List<ListLoadBalancePoolMemberResponseContent>> membersMapGroupByPoolId =
        memberList.stream()
            .collect(Collectors.groupingBy(ListLoadBalancePoolMemberResponseContent::getPoolId));

    List<EcloudLoadBalancer> elbList = new ArrayList<>();
    if (lbList == null || lbList.isEmpty()) {
      log.warn("lb response data is null");
      return elbList;
    }

    for (ListLoadbalanceResponseContent it : lbList) {
      // create lb
      EcloudLoadBalancer loadBalancer = EcloudLbUtil.createEcloudLoadBalancer(it);
      loadBalancer.setRegion(this.region);
      loadBalancer.setAccountName(this.getAccountName());

      // 补充vpcId到lb
      GetVpcDetailRespByRouterIdResponseBody vpc =
          EcloudVpcUtil.getVpcInfoByRouteId(it.getRouterId(), vpcClient);
      if (vpc != null) {
        loadBalancer.setVpcId(vpc.getId());
      }

      // set listener
      List<ListLoadBalanceListenerRespResponseContent> originListeners =
          listenerMapGroupByLbId.get(it.getId());
      if (originListeners != null && !originListeners.isEmpty()) {
        List<EcloudLoadBalancerListener> queryListeners = new ArrayList<>();
        for (ListLoadBalanceListenerRespResponseContent tempListener : originListeners) {
          EcloudLoadBalancerListener e =
              EcloudLbUtil.createEcloudLoadBalancerListener(tempListener);
          queryListeners.add(e);
        }
        loadBalancer.setListeners(queryListeners);
      }

      List<EcloudLoadBalancerRule> rules = new ArrayList<>();
      // set serverGroup
      List<ListPoolRespResponseContent> originPools = poolMapGroupByLbId.get(it.getId());
      if (originPools != null && !originPools.isEmpty()) {
        List<EcloudLoadBalancerPool> queryPools = new ArrayList<>();
        for (ListPoolRespResponseContent pool : originPools) {
          EcloudLoadBalancerPool epool = EcloudLbUtil.createEcloudLoadBalancerPool(pool);
          ListPoolRespResponseHealthMonitorResp poolHealth = pool.getHealthMonitorResp();
          if (poolHealth != null) {
            EcloudLoadBalancerHealth ehealth = new EcloudLoadBalancerHealth();
            ehealth.setHealthDelay(poolHealth.getHealthDelay());
            ehealth.setHealthExpectedCode(poolHealth.getHealthExpectedCode());
            ehealth.setHealthMaxRetries(poolHealth.getHealthMaxRetries());
            ehealth.setHealthHttpMethod(poolHealth.getHealthHttpMethod());
            ehealth.setHealthId(poolHealth.getHealthId());
            ehealth.setHealthType(
                poolHealth.getHealthType() != null ? poolHealth.getHealthType().getValue() : "");
            ehealth.setHealthUrlPath(poolHealth.getHealthUrlPath());
            ehealth.setHealthTimeout(poolHealth.getHealthTimeout());
            epool.setHealthMonitorResp(ehealth);
          }

          // set member
          List<ListLoadBalancePoolMemberResponseContent> originMembers =
              membersMapGroupByPoolId.get(pool.getPoolId());
          if (originMembers != null && !originMembers.isEmpty()) {
            List<EcloudLoadBalancerMember> queryMembers = new ArrayList<>();
            for (ListLoadBalancePoolMemberResponseContent mem : originMembers) {
              EcloudLoadBalancerMember eMem = EcloudLbUtil.createEcloudLoadBalancerMember(mem);
              eMem.setRegion(this.region);
              queryMembers.add(eMem);
            }
            epool.setMembers(queryMembers);
          }

          List<EcloudLoadBalancerL7Policy> eL7s = new ArrayList<>();
          List<ListPoolRespResponseL7PolicyResps> pL7s = pool.getL7PolicyResps();
          if (pL7s != null && !pL7s.isEmpty()) {
            for (ListPoolRespResponseL7PolicyResps pL7 : pL7s) {
              EcloudLoadBalancerL7Policy eL7 = EcloudLbUtil.createEcloudLoadBalancerL7Policy(pL7);
              eL7s.add(eL7);

              EcloudLoadBalancerRule rule = new EcloudLoadBalancerRule();
              rule.setDomain(eL7.getL7PolicyDomainName());
              rule.setUrl(eL7.getL7PolicyUrl());
              rule.setL7PolicyId(eL7.getL7PolicyId());
              rule.setListenerId(eL7.getListenerId());
              rules.add(rule);
            }
            epool.setL7PolicyResps(eL7s);
          }
          queryPools.add(epool);
        }

        if (loadBalancer.getListeners() != null && !loadBalancer.getListeners().isEmpty()) {
          Map<String, List<EcloudLoadBalancerRule>> ruleMapGroupByListenerId =
              rules.stream().collect(Collectors.groupingBy(EcloudLoadBalancerRule::getListenerId));
          loadBalancer
              .getListeners()
              .forEach(
                  itl -> {
                    itl.setRules(ruleMapGroupByListenerId.get(itl.getListenerId()));
                  });
        }
        loadBalancer.setPools(queryPools);
      }
      /*List<EcloudLoadBalancerHealthCheck> healthChecks = new ArrayList<>();
      if (loadBalancer.getPools() == null || loadBalancer.getPools().isEmpty()) {
        continue;
      }
      for (EcloudLoadBalancerPool serverGroup : loadBalancer.getPools()) {
        List<EcloudLoadBalancerMember> members = serverGroup.getMembers();
        if (members == null || members.isEmpty()) {
          continue;
        }

        for (EcloudLoadBalancerMember member : members) {
          EcloudLoadBalancerHealthCheck healthCheck = new EcloudLoadBalancerHealthCheck();
          healthCheck.setLoadbalancerId(loadBalancer.getLoadBalancerId());
          healthCheck.setPoolId(serverGroup.getPoolId());
          healthCheck.setListenerId(serverGroup.getListenerId());
          healthCheck.setInstanceId(member.getVmHostId());
          healthCheck.setMemberId(member.getId());
          healthCheck.setPort(member.getPort());
          healthCheck.setHealthStatus(HealthState.fromString(member.getHealthStatus()));
          healthCheck.setHealthDelay(serverGroup.getHealthMonitorResp().getHealthDelay());
          healthCheck.setHealthExpectedCode(
              serverGroup.getHealthMonitorResp().getHealthExpectedCode());
          healthCheck.setHealthMaxRetries(serverGroup.getHealthMonitorResp().getHealthMaxRetries());
          healthCheck.setHealthHttpMethod(serverGroup.getHealthMonitorResp().getHealthHttpMethod());
          healthCheck.setHealthId(serverGroup.getHealthMonitorResp().getHealthId());
          healthCheck.setHealthType(serverGroup.getHealthMonitorResp().getHealthType());
          healthCheck.setHealthUrlPath(serverGroup.getHealthMonitorResp().getHealthUrlPath());
          healthCheck.setHealthTimeout(serverGroup.getHealthMonitorResp().getHealthTimeout());
          healthChecks.add(healthCheck);
        }
      }
      loadBalancer.setHealthChecks(healthChecks);*/
      elbList.add(loadBalancer);
    }
    return elbList;
  }
}
