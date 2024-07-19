package com.netflix.spinnaker.clouddriver.ecloud.util;

import com.alibaba.fastjson2.JSON;
import com.ecloud.sdk.vlb.v1.Client;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalanceListenerRespPath;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalanceListenerRespQuery;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalanceListenerRespRequest;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalanceListenerRespResponse;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalanceListenerRespResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalancePoolMemberPath;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalancePoolMemberQuery;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalancePoolMemberRequest;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalancePoolMemberResponse;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalancePoolMemberResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListLoadbalanceQuery;
import com.ecloud.sdk.vlb.v1.model.ListLoadbalanceRequest;
import com.ecloud.sdk.vlb.v1.model.ListLoadbalanceResponse;
import com.ecloud.sdk.vlb.v1.model.ListLoadbalanceResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListPoolRespPath;
import com.ecloud.sdk.vlb.v1.model.ListPoolRespQuery;
import com.ecloud.sdk.vlb.v1.model.ListPoolRespRequest;
import com.ecloud.sdk.vlb.v1.model.ListPoolRespResponse;
import com.ecloud.sdk.vlb.v1.model.ListPoolRespResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListPoolRespResponseL7PolicyResps;
import com.netflix.spinnaker.clouddriver.ecloud.enums.LbSpecEnum;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancer;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerL7Policy;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerListener;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerMember;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerPool;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-18
 */
@Slf4j
public final class EcloudLbUtil {
  private EcloudLbUtil() {}

  public static List<ListLoadbalanceResponseContent> getAllLoadBalancer(Client client) {
    List<ListLoadbalanceResponseContent> lbList = new ArrayList<>();
    int page = 1;
    int size = 50;
    while (true) {
      ListLoadbalanceRequest request = new ListLoadbalanceRequest();
      ListLoadbalanceQuery query = new ListLoadbalanceQuery();
      query.setPage(page);
      query.setPageSize(size);
      request.setListLoadbalanceQuery(query);
      ListLoadbalanceResponse rsp = null;
      try {
        rsp = client.listLoadbalance(request);
      } catch (Exception e) {
        log.error(e.getMessage());
      }
      if (rsp != null
          && rsp.getBody() != null
          && ListLoadbalanceResponse.StateEnum.OK.getValue().equals(rsp.getState().getValue())) {
        List<ListLoadbalanceResponseContent> currLbList = rsp.getBody().getContent();
        if (!CollectionUtils.isEmpty(currLbList)) {
          lbList.addAll(currLbList);
          if (lbList.size() < rsp.getBody().getTotal()) {
            page++;
            continue;
          }
        }
      } else {
        log.error(
            "res is null or res_body is null or res_state is not OK,res detail={}",
            JSON.toJSONString(rsp));
      }
      break;
    }
    return lbList;
  }

  public static List<ListLoadBalanceListenerRespResponseContent> getListenerByLbList(
      Client client, List<String> lbIds) {
    List<ListLoadBalanceListenerRespResponseContent> listenerList = new ArrayList<>();
    if (lbIds == null || lbIds.isEmpty()) {
      return listenerList;
    }
    for (String lbId : lbIds) {
      int page = 1;
      int size = 50;
      while (true) {
        ListLoadBalanceListenerRespRequest request = new ListLoadBalanceListenerRespRequest();
        ListLoadBalanceListenerRespPath queryPath = new ListLoadBalanceListenerRespPath();
        queryPath.setLoadBalanceId(lbId);
        ListLoadBalanceListenerRespQuery query = new ListLoadBalanceListenerRespQuery();
        query.setPage(page);
        query.setPageSize(size);
        request.setListLoadBalanceListenerRespPath(queryPath);
        request.setListLoadBalanceListenerRespQuery(query);
        ListLoadBalanceListenerRespResponse rsp = null;
        try {
          rsp = client.listLoadBalanceListenerResp(request);
        } catch (Exception e) {
          log.error(e.getMessage());
        }
        if (rsp != null
            && rsp.getBody() != null
            && ListLoadBalanceListenerRespResponse.StateEnum.OK
                .getValue()
                .equals(rsp.getState().getValue())) {
          List<ListLoadBalanceListenerRespResponseContent> currLbList = rsp.getBody().getContent();
          if (!CollectionUtils.isEmpty(currLbList)) {
            listenerList.addAll(currLbList);
            if (listenerList.size() < rsp.getBody().getTotal()) {
              page++;
              continue;
            }
          }
        } else {
          log.error(
              "res is null or res_body is null or res_state is not OK,res detail={}",
              JSON.toJSONString(rsp));
        }
        break;
      }
    }
    return listenerList;
  }

  public static List<ListPoolRespResponseContent> getPoolByLbList(
      Client client, List<String> lbIds) {
    List<ListPoolRespResponseContent> poolList = new ArrayList<>();
    if (lbIds == null || lbIds.isEmpty()) {
      return poolList;
    }

    for (String lbId : lbIds) {
      int page = 1;
      int size = 50;
      while (true) {
        ListPoolRespRequest request = new ListPoolRespRequest();
        ListPoolRespQuery query = new ListPoolRespQuery();
        query.setPage(page);
        query.setPageSize(size);
        ListPoolRespPath queryPath = new ListPoolRespPath();
        queryPath.setLoadBalanceId(lbId);
        request.setListPoolRespQuery(query);
        request.setListPoolRespPath(queryPath);
        ListPoolRespResponse rsp = null;
        try {
          rsp = client.listPoolResp(request);
        } catch (Exception e) {
          log.error(e.getMessage());
        }
        if (rsp != null
            && rsp.getBody() != null
            && ListPoolRespResponse.StateEnum.OK.getValue().equals(rsp.getState().getValue())) {
          List<ListPoolRespResponseContent> currLbList = rsp.getBody().getContent();
          if (!CollectionUtils.isEmpty(currLbList)) {
            poolList.addAll(currLbList);
            if (poolList.size() < rsp.getBody().getTotal()) {
              page++;
              continue;
            }
          }
        } else {
          log.error(
              "res is null or res_body is null or res_state is not OK,res detail={}",
              JSON.toJSONString(rsp));
        }
        break;
      }
    }
    return poolList;
  }

  public static List<ListLoadBalancePoolMemberResponseContent> getMemberByPoolIdList(
      Client client, List<ListPoolRespResponseContent> poolList) {
    List<ListLoadBalancePoolMemberResponseContent> memberList = new ArrayList<>();
    if (poolList == null || poolList.isEmpty()) {
      return memberList;
    }

    for (ListPoolRespResponseContent pool : poolList) {
      String poolId = pool.getPoolId();
      int page = 1;
      int size = 50;
      while (true) {
        ListLoadBalancePoolMemberRequest request = new ListLoadBalancePoolMemberRequest();
        ListLoadBalancePoolMemberQuery query = new ListLoadBalancePoolMemberQuery();
        query.setPage(page);
        query.setPageSize(size);
        ListLoadBalancePoolMemberPath queryPath = new ListLoadBalancePoolMemberPath();
        queryPath.setPoolId(poolId);
        request.setListLoadBalancePoolMemberPath(queryPath);
        request.setListLoadBalancePoolMemberQuery(query);
        ListLoadBalancePoolMemberResponse rsp = null;
        try {
          rsp = client.listLoadBalancePoolMember(request);
        } catch (Exception e) {
          log.error(e.getMessage());
        }
        if (rsp != null
            && rsp.getBody() != null
            && ListLoadBalancePoolMemberResponse.StateEnum.OK
                .getValue()
                .equals(rsp.getState().getValue())) {
          List<ListLoadBalancePoolMemberResponseContent> currLbList = rsp.getBody().getContent();
          if (!CollectionUtils.isEmpty(currLbList)) {
            memberList.addAll(currLbList);
            if (memberList.size() < rsp.getBody().getTotal()) {
              page++;
              continue;
            }
          }
        } else {
          log.error(
              "res is null or res_body is null or res_state is not OK,res detail={}",
              JSON.toJSONString(rsp));
        }
        break;
      }
    }
    return memberList;
  }

  public static EcloudLoadBalancer createEcloudLoadBalancer(ListLoadbalanceResponseContent it) {
    EcloudLoadBalancer loadBalancer = new EcloudLoadBalancer();
    loadBalancer.setSubnetId(it.getSubnetId());
    loadBalancer.setVpcName(it.getVpcName());
    loadBalancer.setOrderId(it.getOrderId());
    loadBalancer.setPrivateIp(it.getPrivateIp());
    loadBalancer.setDescription(it.getDescription());
    loadBalancer.setNodeIp(it.getNodeIp());
    loadBalancer.setVipPortId(it.getVipPortId());
    loadBalancer.setSubnetName(it.getSubnetName());
    loadBalancer.setIsMultiAz(it.isgetIsMultiAz());
    loadBalancer.setIsExclusive(it.isgetIsExclusive());
    loadBalancer.setIpId(it.getIpId());
    loadBalancer.setProvider(it.getProvider() != null ? it.getProvider().getValue() : "");
    loadBalancer.setRouterId(it.getRouterId());
    loadBalancer.setCreateTime(it.getCreatedTime());
    loadBalancer.setId(it.getId());
    loadBalancer.setLoadBalancerId(it.getId());
    loadBalancer.setAdminStateUp(it.isgetAdminStateUp());
    loadBalancer.setMeasureType(it.getMeasureType());
    loadBalancer.setEcStatus(it.getEcStatus() != null ? it.getEcStatus().getValue() : "");
    loadBalancer.setVisible(it.isgetVisible());
    loadBalancer.setProposer(it.getProposer());
    loadBalancer.setPublicIp(it.getPublicIp());
    loadBalancer.setUserName(it.getUserName());
    loadBalancer.setFlavor(it.getFlavor());
    loadBalancer.setDeleted(it.isgetDeleted());
    loadBalancer.setIpVersion(it.getIpVersion() != null ? it.getIpVersion().getValue() : "");
    loadBalancer.setName(it.getName());
    loadBalancer.setLoadBalancerName(it.getName());
    loadBalancer.setOpStatus(it.getOpStatus() != null ? it.getOpStatus().getValue() : "");

    loadBalancer.setLoadBalancerSpec(LbSpecEnum.getDescByCode(it.getFlavor()));
    return loadBalancer;
  }

  public static EcloudLoadBalancerListener createEcloudLoadBalancerListener(
      ListLoadBalanceListenerRespResponseContent tempListener) {
    EcloudLoadBalancerListener e = new EcloudLoadBalancerListener();
    e.setHealthDelay(tempListener.getHealthDelay());
    e.setModifiedTime(tempListener.getModifiedTime());
    e.setGroupType(tempListener.getGroupType());
    e.setRedirectToListenerId(tempListener.getRedirectToListenerId());
    e.setSniContainerIds(tempListener.getSniContainerIds());
    e.setDescription(tempListener.getDescription());
    e.setIsMultiAz(tempListener.isgetIsMultiAz());
    e.setRedirectToListenerName(tempListener.getRedirectToListenerName());
    e.setProtocol(tempListener.getProtocol() != null ? tempListener.getProtocol().getValue() : "");
    e.setSniContainerIdList(tempListener.getSniContainerIdList());
    e.setCreatedTime(tempListener.getCreatedTime());
    e.setHttp2(tempListener.isgetHttp2());
    e.setId(tempListener.getId());
    e.setListenerId(tempListener.getId());
    e.setDefaultTlsContainerId(tempListener.getDefaultTlsContainerId());
    e.setMutualAuthenticationUp(tempListener.isgetMutualAuthenticationUp());
    e.setCookieName(tempListener.getCookieName());
    e.setPoolName(tempListener.getPoolName());
    e.setSniUp(tempListener.isgetSniUp());
    e.setLbAlgorithm(
        tempListener.getLbAlgorithm() != null ? tempListener.getLbAlgorithm().getValue() : "");
    e.setHealthHttpMethod(tempListener.getHealthHttpMethod());
    e.setHealthId(tempListener.getHealthId());
    e.setHealthType(
        tempListener.getHealthType() != null ? tempListener.getHealthType().getValue() : "");
    e.setLoadBalanceFlavor(tempListener.getLoadBalanceFlavor());
    e.setLoadBalanceId(tempListener.getLoadBalanceId());
    e.setProtocolPort(tempListener.getProtocolPort());
    e.setPort(tempListener.getProtocolPort());
    e.setHealthExpectedCode(tempListener.getHealthExpectedCode());
    e.setGroupName(tempListener.getGroupName());
    e.setConnectionLimit(tempListener.getConnectionLimit());
    e.setDeleted(tempListener.isgetDeleted());
    e.setHealthMaxRetries(tempListener.getHealthMaxRetries());
    e.setName(tempListener.getName());
    e.setListenerName(tempListener.getName());
    e.setPoolId(tempListener.getPoolId());
    e.setSessionPersistence(
        tempListener.getSessionPersistence() != null
            ? tempListener.getSessionPersistence().getValue()
            : "");
    e.setGroupEnabled(tempListener.isgetGroupEnabled());
    e.setHealthUrlPath(tempListener.getHealthUrlPath());
    e.setCaContainerId(tempListener.getCaContainerId());
    e.setOpStatus(tempListener.getOpStatus() != null ? tempListener.getOpStatus().getValue() : "");
    e.setControlGroupId(tempListener.getControlGroupId());
    e.setHealthTimeout(tempListener.getHealthTimeout());
    e.setMultiAzUuid(tempListener.getMultiAzUuid());
    return e;
  }

  public static EcloudLoadBalancerPool createEcloudLoadBalancerPool(
      ListPoolRespResponseContent pool) {
    EcloudLoadBalancerPool epool = new EcloudLoadBalancerPool();
    epool.setModifiedTime(pool.getModifiedTime());
    epool.setLbAlgorithm(pool.getLbAlgorithm() != null ? pool.getLbAlgorithm().getValue() : "");
    epool.setLoadBalanceId(pool.getLoadBalanceId());
    epool.setIsMultiAz(pool.isgetIsMultiAz());
    epool.setListenerId(pool.getListenerId());
    epool.setProtocol(pool.getProtocol() != null ? pool.getProtocol().getValue() : "");
    epool.setDeleted(pool.isgetDeleted());
    epool.setListenerName(pool.getListenerName());
    epool.setPoolId(pool.getPoolId());
    epool.setSessionPersistence(
        pool.getSessionPersistence() != null ? pool.getSessionPersistence().getValue() : "");
    epool.setCreatedTime(pool.getCreatedTime());
    epool.setMultiAzUuid(pool.getMultiAzUuid());
    epool.setCookieName(pool.getCookieName());
    epool.setPoolName(pool.getPoolName());

    return epool;
  }

  public static EcloudLoadBalancerL7Policy createEcloudLoadBalancerL7Policy(
      ListPoolRespResponseL7PolicyResps pL7) {
    EcloudLoadBalancerL7Policy eL7 = new EcloudLoadBalancerL7Policy();
    eL7.setModifiedTime(pL7.getModifiedTime());
    eL7.setDescription(pL7.getDescription());
    eL7.setL7PolicyDomainName(pL7.getL7PolicyDomainName());
    eL7.setIsMultiAz(pL7.isgetIsMultiAz());
    eL7.setL7RuleValue(pL7.getL7RuleValue());
    eL7.setListenerId(pL7.getListenerId());
    eL7.setCompareType(pL7.getCompareType() != null ? pL7.getCompareType().getValue() : "");
    eL7.setDeleted(pL7.isgetDeleted());
    eL7.setL7PolicyUrl(pL7.getL7PolicyUrl());
    eL7.setL7PolicyName(pL7.getL7PolicyName());
    eL7.setRuleType(pL7.getRuleType() != null ? pL7.getRuleType().getValue() : "");
    eL7.setPoolId(pL7.getPoolId());
    eL7.setCreatedTime(pL7.getCreatedTime());
    eL7.setL7PolicyId(pL7.getL7PolicyId());
    eL7.setPosition(pL7.getPosition());
    eL7.setAdminStateUp(pL7.isgetAdminStateUp());
    eL7.setMultiAzUuid(pL7.getMultiAzUuid());
    eL7.setPoolName(pL7.getPoolName());
    return eL7;
  }

  public static EcloudLoadBalancerMember createEcloudLoadBalancerMember(
      ListLoadBalancePoolMemberResponseContent mem) {
    EcloudLoadBalancerMember eMem = new EcloudLoadBalancerMember();
    eMem.setSubnetId(mem.getSubnetId());
    eMem.setVmName(mem.getVmName());
    eMem.setIsDelete(mem.getIsDelete());
    eMem.setProposer(mem.getProposer());
    eMem.setIp(mem.getIp());
    eMem.setDescription(mem.getDescription());
    eMem.setWeight(mem.getWeight());
    eMem.setType(mem.getType());
    eMem.setIsMultiAz(mem.isgetIsMultiAz());
    eMem.setHealthStatus(mem.getHealthStatus());
    eMem.setPort(mem.getPort());
    eMem.setPoolId(mem.getPoolId());
    eMem.setCreatedTime(mem.getCreatedTime());
    eMem.setId(mem.getId());
    eMem.setVmHostId(mem.getVmHostId());
    eMem.setMultiAzUuid(mem.getMultiAzUuid());
    eMem.setStatus(mem.getStatus());
    return eMem;
  }
}