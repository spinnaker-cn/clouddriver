package com.netflix.spinnaker.clouddriver.ecloud.provider.agent;

import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.HEALTH_CHECKS;

import com.ecloud.sdk.config.Config;
import com.ecloud.sdk.vlb.v1.Client;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalanceListenerRespResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalancePoolMemberResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListLoadbalanceResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListPoolRespResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListPoolRespResponseHealthMonitorResp;
import com.ecloud.sdk.vlb.v1.model.ListPoolRespResponseL7PolicyResps;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.agent.AccountAware;
import com.netflix.spinnaker.cats.agent.AgentDataType;
import com.netflix.spinnaker.cats.agent.CacheResult;
import com.netflix.spinnaker.cats.agent.CachingAgent;
import com.netflix.spinnaker.cats.agent.DefaultCacheResult;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.DefaultCacheData;
import com.netflix.spinnaker.cats.provider.ProviderCache;
import com.netflix.spinnaker.clouddriver.core.provider.agent.HealthProvidingCachingAgent;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancer;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerHealth;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerHealthCheck;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerL7Policy;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerListener;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerMember;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerPool;
import com.netflix.spinnaker.clouddriver.ecloud.provider.EcloudSearchableProvider;
import com.netflix.spinnaker.clouddriver.ecloud.security.EcloudCredentials;
import com.netflix.spinnaker.clouddriver.ecloud.util.EcloudLbUtil;
import com.netflix.spinnaker.clouddriver.model.HealthState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-09
 */
@Slf4j
public class EcloudLoadbalancerInstanceStateCachingAgent
    implements CachingAgent, HealthProvidingCachingAgent, AccountAware {

  private String region;

  ObjectMapper objectMapper;

  EcloudCredentials account;

  public static String healthId = "ecloud-load-balancer-health";

  public EcloudLoadbalancerInstanceStateCachingAgent(
      EcloudCredentials account, ObjectMapper objectMapper, String region) {
    this.account = account;
    this.region = region;
    this.objectMapper = objectMapper;
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
  public Collection<AgentDataType> getProvidedDataTypes() {
    return types;
  }

  @Override
  public CacheResult loadData(ProviderCache providerCache) {
    log.info("Enter loadData in agentType={}", this.getAgentType());
    List<EcloudLoadBalancerHealthCheck> targetHealths = getLoadBalancerTargetHealth();
    Collection<String> evictions =
        providerCache.filterIdentifiers(
            HEALTH_CHECKS.ns,
            Keys.getTargetHealthKey("*", "*", "*", this.getAccountName(), region));

    List<CacheData> data = new ArrayList<>();
    for (EcloudLoadBalancerHealthCheck targetHealth : targetHealths) {
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("targetHealth", targetHealth);

      String targetHealthKey =
          Keys.getTargetHealthKey(
              targetHealth.getLoadbalancerId(),
              targetHealth.getPoolId(),
              targetHealth.getInstanceId(),
              this.getAccountName(),
              region);
      String keepKey = null;
      for (String eviction : evictions) {
        if (eviction.equals(targetHealthKey)) {
          keepKey = eviction;
          break;
        }
      }
      if (keepKey != null) {
        evictions.remove(keepKey);
      }
      data.add(new DefaultCacheData(targetHealthKey, attributes, new HashMap<>()));
    }

    log.info(
        "Caching data size={} items evictions size={} items in agentType={}",
        data.size(),
        evictions.size(),
        this.getAgentType());

    Map<String, Collection<CacheData>> dataMap = new HashMap<>();
    dataMap.put(HEALTH_CHECKS.ns, data);
    Map<String, Collection<String>> evictionsMap = new HashMap<>();
    evictionsMap.put(HEALTH_CHECKS.ns, evictions);
    DefaultCacheResult result = new DefaultCacheResult(dataMap, evictionsMap);
    return result;
  }

  private List<EcloudLoadBalancerHealthCheck> getLoadBalancerTargetHealth() {
    List<EcloudLoadBalancerHealthCheck> healthChecks = new ArrayList<>();
    try {
      List<EcloudLoadBalancer> lbList = loadLoadBalancerData();
      if (lbList == null || lbList.isEmpty()) {
        return healthChecks;
      }

      for (EcloudLoadBalancer lb : lbList) {
        List<EcloudLoadBalancerPool> serverGroupSet = lb.getPools();
        if (serverGroupSet == null || serverGroupSet.isEmpty()) {
          continue;
        }
        for (EcloudLoadBalancerPool serverGroup : serverGroupSet) {
          List<EcloudLoadBalancerMember> members = serverGroup.getMembers();
          if (members == null || members.isEmpty()) {
            continue;
          }
          for (EcloudLoadBalancerMember member : members) {
            EcloudLoadBalancerHealthCheck healthCheck = new EcloudLoadBalancerHealthCheck();
            healthCheck.setLoadbalancerId(lb.getLoadBalancerId());
            healthCheck.setPoolId(serverGroup.getPoolId());
            healthCheck.setListenerId(serverGroup.getListenerId());
            healthCheck.setInstanceId(member.getVmHostId());
            healthCheck.setMemberId(member.getId());
            healthCheck.setPort(member.getPort());
            healthCheck.setHealthStatus(HealthState.fromString(member.getHealthStatus()));
            healthCheck.setHealthDelay(serverGroup.getHealthMonitorResp().getHealthDelay());
            healthCheck.setHealthExpectedCode(
                serverGroup.getHealthMonitorResp().getHealthExpectedCode());
            healthCheck.setHealthMaxRetries(
                serverGroup.getHealthMonitorResp().getHealthMaxRetries());
            healthCheck.setHealthHttpMethod(
                serverGroup.getHealthMonitorResp().getHealthHttpMethod());
            healthCheck.setHealthId(serverGroup.getHealthMonitorResp().getHealthId());
            healthCheck.setHealthType(serverGroup.getHealthMonitorResp().getHealthType());
            healthCheck.setHealthUrlPath(serverGroup.getHealthMonitorResp().getHealthUrlPath());
            healthCheck.setHealthTimeout(serverGroup.getHealthMonitorResp().getHealthTimeout());
            healthChecks.add(healthCheck);
          }
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return healthChecks;
  }

  private List<EcloudLoadBalancer> loadLoadBalancerData() {
    Config config = new Config();
    config.setAccessKey(this.account.getAccessKey());
    config.setSecretKey(this.account.getSecretKey());
    config.setPoolId(this.region);
    Client client = new Client(config);

    List<ListLoadbalanceResponseContent> lbList = EcloudLbUtil.getAllLoadBalancer(client);

    List<String> lbIds = new ArrayList<>();
    for (ListLoadbalanceResponseContent lb : lbList) {
      lbIds.add(lb.getId());
    }
    List<ListLoadBalanceListenerRespResponseContent> listenerList =
        EcloudLbUtil.getListenerByLbList(client, lbIds);

    List<ListPoolRespResponseContent> poolList = EcloudLbUtil.getPoolByLbList(client, lbIds);

    List<ListLoadBalancePoolMemberResponseContent> memberList =
        EcloudLbUtil.getMemberByPoolIdList(client, poolList);

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
      EcloudLoadBalancer loadBalancer = EcloudLbUtil.createEcloudLoadBalancer(it);
      loadBalancer.setRegion(this.region);
      loadBalancer.setAccountName(this.getAccountName());

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

      List<ListPoolRespResponseContent> originPools = poolMapGroupByLbId.get(it.getId());
      if (originPools != null && !originPools.isEmpty()) {
        List<EcloudLoadBalancerPool> queryPools = new ArrayList<>();
        for (ListPoolRespResponseContent pool : originPools) {
          EcloudLoadBalancerPool epool = EcloudLbUtil.createEcloudLoadBalancerPool(pool);
          //          epool.setName(pool.getPoolName());
          //          epool.setRegion(this.region);
          //          epool.setCloudProvider(EcloudProvider.ID);

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
            }
            epool.setL7PolicyResps(eL7s);
          }
          queryPools.add(epool);
        }
        loadBalancer.setPools(queryPools);
      }
      elbList.add(loadBalancer);
    }
    return elbList;
  }

  @Override
  public String getHealthId() {
    return this.healthId;
  }
}
