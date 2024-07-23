package com.netflix.spinnaker.clouddriver.ecloud.provider.agent;

import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.HEALTH_CHECKS;

import com.ecloud.sdk.config.Config;
import com.ecloud.sdk.vlb.v1.Client;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalanceListenersRespResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalancerPoolMemberResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListLoadbalanceRespResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListPoolResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListPoolResponseHealthMonitorResp;
import com.ecloud.sdk.vlb.v1.model.ListPoolResponseL7PolicyResps;
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
    return healthChecks;
  }

  private List<EcloudLoadBalancer> loadLoadBalancerData() {
    Config config = new Config();
    config.setAccessKey(this.account.getAccessKey());
    config.setSecretKey(this.account.getSecretKey());
    config.setPoolId(this.region);
    Client client = new Client(config);

    List<ListLoadbalanceRespResponseContent> lbList = EcloudLbUtil.getAllLoadBalancer(client);

    List<String> lbIds = new ArrayList<>();
    for (ListLoadbalanceRespResponseContent lb : lbList) {
      lbIds.add(lb.getId());
    }
    List<ListLoadBalanceListenersRespResponseContent> listenerList =
        EcloudLbUtil.getListenerByLbList(client, lbIds);

    List<ListPoolResponseContent> poolList = EcloudLbUtil.getPoolByLbList(client, lbIds);

    List<ListLoadBalancerPoolMemberResponseContent> memberList =
        EcloudLbUtil.getMemberByPoolIdList(client, poolList);

    Map<String, List<ListLoadBalanceListenersRespResponseContent>> listenerMapGroupByLbId =
        listenerList.stream()
            .collect(
                Collectors.groupingBy(
                  ListLoadBalanceListenersRespResponseContent::getLoadBalanceId));

    Map<String, List<ListPoolResponseContent>> poolMapGroupByLbId =
        poolList.stream()
            .collect(Collectors.groupingBy(ListPoolResponseContent::getLoadBalanceId));

    Map<String, List<ListLoadBalancerPoolMemberResponseContent>> membersMapGroupByPoolId =
        memberList.stream()
            .collect(Collectors.groupingBy(ListLoadBalancerPoolMemberResponseContent::getPoolId));

    List<EcloudLoadBalancer> elbList = new ArrayList<>();
    if (lbList == null || lbList.isEmpty()) {
      log.warn("lb response data is null");
      return elbList;
    }

    for (ListLoadbalanceRespResponseContent it : lbList) {
      EcloudLoadBalancer loadBalancer = EcloudLbUtil.createEcloudLoadBalancer(it);
      loadBalancer.setRegion(this.region);
      loadBalancer.setAccountName(this.getAccountName());

      List<ListLoadBalanceListenersRespResponseContent> originListeners =
          listenerMapGroupByLbId.get(it.getId());
      if (originListeners != null && !originListeners.isEmpty()) {
        List<EcloudLoadBalancerListener> queryListeners = new ArrayList<>();
        for (ListLoadBalanceListenersRespResponseContent tempListener : originListeners) {
          EcloudLoadBalancerListener e =
              EcloudLbUtil.createEcloudLoadBalancerListener(tempListener);
          queryListeners.add(e);
        }
        loadBalancer.setListeners(queryListeners);
      }

      List<ListPoolResponseContent> originPools = poolMapGroupByLbId.get(it.getId());
      if (originPools != null && !originPools.isEmpty()) {
        List<EcloudLoadBalancerPool> queryPools = new ArrayList<>();
        for (ListPoolResponseContent pool : originPools) {
          EcloudLoadBalancerPool epool = EcloudLbUtil.createEcloudLoadBalancerPool(pool);
          //          epool.setName(pool.getPoolName());
          //          epool.setRegion(this.region);
          //          epool.setCloudProvider(EcloudProvider.ID);

          ListPoolResponseHealthMonitorResp poolHealth = pool.getHealthMonitorResp();
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

          List<ListLoadBalancerPoolMemberResponseContent> originMembers =
              membersMapGroupByPoolId.get(pool.getPoolId());
          if (originMembers != null && !originMembers.isEmpty()) {
            List<EcloudLoadBalancerMember> queryMembers = new ArrayList<>();
            for (ListLoadBalancerPoolMemberResponseContent mem : originMembers) {
              EcloudLoadBalancerMember eMem = EcloudLbUtil.createEcloudLoadBalancerMember(mem);
              eMem.setRegion(this.region);
              queryMembers.add(eMem);
            }
            epool.setMembers(queryMembers);
          }

          List<EcloudLoadBalancerL7Policy> eL7s = new ArrayList<>();
          List<ListPoolResponseL7PolicyResps> pL7s = pool.getL7PolicyResps();
          if (pL7s != null && !pL7s.isEmpty()) {
            for (ListPoolResponseL7PolicyResps pL7 : pL7s) {
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
    return healthId;
  }
}
