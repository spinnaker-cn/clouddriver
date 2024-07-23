package com.netflix.spinnaker.clouddriver.ecloud.provider.agent;

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.SUBNETS;

import com.ecloud.sdk.config.Config;
import com.ecloud.sdk.vpc.v1.Client;
import com.ecloud.sdk.vpc.v1.model.ListSubnetsResponseContent;
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
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudSubnet;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudVpc;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudZone;
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
 * @description
 * @date 2024-04-08
 */
@Slf4j
public class EcloudSubNetworkCachingAgent implements CachingAgent, AccountAware {

  private EcloudCredentials account;

  private String region;

  ObjectMapper objectMapper;

  static final Collection<AgentDataType> types =
      Collections.unmodifiableCollection(
          new ArrayList<AgentDataType>() {
            {
              add(AUTHORITATIVE.forType(SUBNETS.ns));
            }
          });

  public EcloudSubNetworkCachingAgent(
      EcloudCredentials account, String region, ObjectMapper objectMapper) {
    this.account = account;
    this.region = region;
    this.objectMapper = objectMapper;
  }

  @Override
  public CacheResult loadData(ProviderCache providerCache) {
    Config config = new Config();
    config.setAccessKey(this.account.getAccessKey());
    config.setSecretKey(this.account.getSecretKey());
    config.setPoolId(this.region);
    Client client = new Client(config);

    Map<String, Collection<CacheData>> resultMap = new HashMap<>(16);
    List<CacheData> datas = new ArrayList<>();

    // 根据VPC查询子网
    List<EcloudVpc> vpcs = getEcloudVpcs(client);
    for (EcloudVpc vpc : vpcs) {
      List<EcloudSubnet> currSubnets = getEcloudSubnetsByVpcId(vpc.getId(), client);
      currSubnets.forEach(
          it -> {
            it.setVpcId(vpc.getId());
            Map<String, Object> attributes = objectMapper.convertValue(it, Map.class);
            CacheData data =
                new DefaultCacheData(
                    Keys.getSubnetKey(it.getId(), account.getName(), region),
                    attributes,
                    new HashMap<>(16));
            datas.add(data);
          });
    }
    log.info("Caching data.size={} items in agentType={}", datas.size(), this.getAgentType());
    resultMap.put(SUBNETS.ns, datas);
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

  private List<EcloudVpc> getEcloudVpcs(Client client) {
    List<ListVpcResponseContent> vpcList = EcloudVpcUtil.getVpcList(client);

    List<EcloudVpc> vpcs = new ArrayList<>();
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
      vpcs.add(vpc);
    }
    return vpcs;
  }

  private List<EcloudSubnet> getEcloudSubnetsByVpcId(String vpcId, Client client) {
    List<ListSubnetsResponseContent> orginSubNets = EcloudVpcUtil.getSubnetsByVpcId(vpcId, client);

    List<EcloudSubnet> subnets = new ArrayList<>();
    for (ListSubnetsResponseContent one : orginSubNets) {
      EcloudSubnet subnet = new EcloudSubnet();
      subnet.setId(one.getId());
      subnet.setAccount(this.getAccountName());
      subnet.setRegion(this.region);
      subnet.setType(EcloudProvider.ID);
      subnet.setCidr(one.getCidr());
      subnet.setCidrBlock(one.getCidr());
      subnet.setCreatedTime(one.getCreatedTime());
      subnet.setDeleted(one.getDeleted());
      subnet.setEdge(one.getEdge());
      subnet.setGatewayIp(one.getGatewayIp());
      subnet.setIpVersion(one.getIpVersion().getValue());
      subnet.setName(one.getName());
      subnet.setNetworkId(one.getNetworkId());
      subnet.setNetworkType(one.getNetworkType());
      subnet.setVPoolId(one.getVpoolId());
      subnet.setProvider(EcloudProvider.ID);
      subnet.setZone(one.getRegion());
      EcloudZone ecloudZone =
          EcloudZoneHelper.getEcloudZone(this.getAccountName(), this.region, one.getRegion());
      subnet.setZoneName(ecloudZone == null ? one.getRegion() : ecloudZone.getName());
      subnets.add(subnet);
    }
    return subnets;
  }
}
