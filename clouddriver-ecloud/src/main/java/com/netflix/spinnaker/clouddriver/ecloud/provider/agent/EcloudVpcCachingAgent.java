package com.netflix.spinnaker.clouddriver.ecloud.provider.agent;

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.NETWORKS;

import com.ecloud.sdk.config.Config;
import com.ecloud.sdk.vpc.v1.Client;
import com.ecloud.sdk.vpc.v1.model.ListVpcResponseContent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.agent.AccountAware;
import com.netflix.spinnaker.cats.agent.AgentDataType;
import com.netflix.spinnaker.cats.agent.CacheResult;
import com.netflix.spinnaker.cats.agent.CachingAgent;
import com.netflix.spinnaker.cats.agent.DefaultCacheResult;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.DefaultCacheData;
import com.netflix.spinnaker.cats.provider.ProviderCache;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudVpc;
import com.netflix.spinnaker.clouddriver.ecloud.provider.EcloudSearchableProvider;
import com.netflix.spinnaker.clouddriver.ecloud.security.EcloudCredentials;
import com.netflix.spinnaker.clouddriver.ecloud.util.EcloudVpcUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * @author han.pengfei
 * @description vpc/network
 * @date 2024-04-08
 */
@Slf4j
public class EcloudVpcCachingAgent implements CachingAgent, AccountAware {

  private EcloudCredentials account;

  private String region;

  ObjectMapper objectMapper;

  static final Collection<AgentDataType> types =
      Collections.unmodifiableCollection(
          new ArrayList<AgentDataType>() {
            {
              add(AUTHORITATIVE.forType(NETWORKS.ns));
            }
          });

  public EcloudVpcCachingAgent(
      EcloudCredentials account, String region, ObjectMapper objectMapper) {
    this.account = account;
    this.region = region;
    this.objectMapper = objectMapper;
  }

  @Override
  public CacheResult loadData(ProviderCache providerCache) {
    log.info("Describing items in agentType=", this.getAgentType());
    Config config = new Config();
    config.setAccessKey(this.account.getAccessKey());
    config.setSecretKey(this.account.getSecretKey());
    config.setPoolId(this.region);
    Client client = new Client(config);

    Map<String, Collection<CacheData>> resultMap = new HashMap<>(16);
    List<CacheData> networkDatas = new ArrayList<>();
    List<ListVpcResponseContent> vpcList = EcloudVpcUtil.getVpcList(client);

    for (ListVpcResponseContent one : vpcList) {
      EcloudVpc vpc = new EcloudVpc();

      vpc.setOrderType(one.getOrderType() != null ? one.getOrderType().getValue() : "");
      vpc.setEcStatus(one.getEcStatus() != null ? one.getEcStatus().getValue() : "");
      vpc.setVpoolId(one.getVpoolId());
      vpc.setDescription(one.getDescription());
      vpc.setScale(one.getScale() != null ? one.getScale().getValue() : "");
      vpc.setUserName(one.getUserName());
      vpc.setUserId(one.getUserId());
      vpc.setVaz(one.getVaz());
      vpc.setSpecial(one.getSpecial());
      vpc.setEdge(one.getEdge());
      vpc.setDeleted(one.getDeleted());
      vpc.setRouterId(one.getRouterId());
      vpc.setVpcName(one.getName());
      vpc.setName(one.getName());
      vpc.setCreatedTime(one.getCreatedTime());
      vpc.setVpcExtraSpecification(one.getVpcExtraSpecification());
      vpc.setId(one.getId());
      vpc.setVpcId(one.getId());
      vpc.setAdminStateUp(one.getAdminStateUp());
      vpc.setRegion(this.region);
      vpc.setAccount(this.getAccountName());
      vpc.setCloudProvider(EcloudProvider.ID);

      Map<String, Object> attributes = objectMapper.convertValue(vpc, Map.class);
      CacheData networkData =
          new DefaultCacheData(
              Keys.getNetworkKey(vpc.getId(), account.getName(), region),
              attributes,
              new HashMap<>(16));
      networkDatas.add(networkData);
    }

    log.info(
        "Caching networks data.size={} items in agentType={}",
        networkDatas.size(),
        this.getAgentType());
    resultMap.put(NETWORKS.ns, networkDatas);

    return new DefaultCacheResult(resultMap);
  }

  @Override
  public String getProviderName() {
    return EcloudSearchableProvider.class.getName();
  }

  @Override
  public String getAgentType() {
    return account.getName() + "/" + region + "/" + this.getClass().getSimpleName();
  }

  @Override
  public String getAccountName() {
    return account.getName();
  }

  @Override
  public Collection<AgentDataType> getProvidedDataTypes() {
    return types;
  }
}
