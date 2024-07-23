package com.netflix.spinnaker.clouddriver.ecloud.util;

import com.alibaba.fastjson2.JSON;
import com.ecloud.sdk.vlb.v1.Client;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalanceListenersRespPath;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalanceListenersRespQuery;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalanceListenersRespRequest;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalanceListenersRespResponse;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalanceListenersRespResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalancerPoolMemberPath;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalancerPoolMemberQuery;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalancerPoolMemberRequest;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalancerPoolMemberResponse;
import com.ecloud.sdk.vlb.v1.model.ListLoadBalancerPoolMemberResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListLoadbalanceRespQuery;
import com.ecloud.sdk.vlb.v1.model.ListLoadbalanceRespRequest;
import com.ecloud.sdk.vlb.v1.model.ListLoadbalanceRespResponse;
import com.ecloud.sdk.vlb.v1.model.ListLoadbalanceRespResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListPoolPath;
import com.ecloud.sdk.vlb.v1.model.ListPoolQuery;
import com.ecloud.sdk.vlb.v1.model.ListPoolRequest;
import com.ecloud.sdk.vlb.v1.model.ListPoolResponse;
import com.ecloud.sdk.vlb.v1.model.ListPoolResponseContent;
import com.ecloud.sdk.vlb.v1.model.ListPoolResponseL7PolicyResps;
import com.netflix.spinnaker.clouddriver.ecloud.enums.LbSpecEnum;
import com.netflix.spinnaker.clouddriver.ecloud.exception.EcloudException;
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

  public static List<ListLoadbalanceRespResponseContent> getAllLoadBalancer(Client client) {
    List<ListLoadbalanceRespResponseContent> lbList = new ArrayList<>();
    int page = 1;
    int size = 50;
    while (true) {
      ListLoadbalanceRespRequest request = new ListLoadbalanceRespRequest();
      ListLoadbalanceRespQuery query = new ListLoadbalanceRespQuery();
      query.setPage(page);
      query.setPageSize(size);
      request.setListLoadbalanceRespQuery(query);
      ListLoadbalanceRespResponse rsp = null;
      rsp = client.listLoadbalanceResp(request);
      if (rsp != null
          && rsp.getBody() != null
          && ListLoadbalanceRespResponse.StateEnum.OK
              .getValue()
              .equals(rsp.getState().getValue())) {
        List<ListLoadbalanceRespResponseContent> currLbList = rsp.getBody().getContent();
        if (!CollectionUtils.isEmpty(currLbList)) {
          lbList.addAll(currLbList);
          if (lbList.size() < rsp.getBody().getTotal()) {
            page++;
            continue;
          }
        }
      } else {
        log.error(
            "GetLoadBalancer return null or res_body is null or res_state is not OK,res detail={}",
            JSON.toJSONString(rsp));
        throw new EcloudException(
            "GetLoadBalancer return null or body is null or res_state is not OK");
      }
      break;
    }
    return lbList;
  }

  public static List<ListLoadBalanceListenersRespResponseContent> getListenerByLbList(
      Client client, List<String> lbIds) {
    List<ListLoadBalanceListenersRespResponseContent> listenerList = new ArrayList<>();
    if (lbIds == null || lbIds.isEmpty()) {
      return listenerList;
    }
    for (String lbId : lbIds) {
      int page = 1;
      int size = 50;
      while (true) {
        ListLoadBalanceListenersRespRequest request = new ListLoadBalanceListenersRespRequest();
        ListLoadBalanceListenersRespPath queryPath = new ListLoadBalanceListenersRespPath();
        queryPath.setLoadBalanceId(lbId);
        ListLoadBalanceListenersRespQuery query = new ListLoadBalanceListenersRespQuery();
        query.setPage(page);
        query.setPageSize(size);
        request.setListLoadBalanceListenersRespPath(queryPath);
        request.setListLoadBalanceListenersRespQuery(query);
        ListLoadBalanceListenersRespResponse rsp = null;
        rsp = client.listLoadBalanceListenersResp(request);
        if (rsp != null
            && rsp.getBody() != null
            && ListLoadBalanceListenersRespResponse.StateEnum.OK
                .getValue()
                .equals(rsp.getState().getValue())) {
          List<ListLoadBalanceListenersRespResponseContent> currLbList = rsp.getBody().getContent();
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
          throw new EcloudException(
              "GetListener return null or body is null or res_state is not OK");
        }
        break;
      }
    }
    return listenerList;
  }

  public static List<ListPoolResponseContent> getPoolByLbList(Client client, List<String> lbIds) {
    List<ListPoolResponseContent> poolList = new ArrayList<>();
    if (lbIds == null || lbIds.isEmpty()) {
      return poolList;
    }

    for (String lbId : lbIds) {
      int page = 1;
      int size = 50;
      while (true) {
        ListPoolRequest request = new ListPoolRequest();
        ListPoolQuery query = new ListPoolQuery();
        query.setPage(page);
        query.setPageSize(size);
        ListPoolPath queryPath = new ListPoolPath();
        queryPath.setLoadBalanceId(lbId);
        request.setListPoolQuery(query);
        request.setListPoolPath(queryPath);
        ListPoolResponse rsp = null;
        rsp = client.listPool(request);
        if (rsp != null
            && rsp.getBody() != null
            && ListPoolResponse.StateEnum.OK.getValue().equals(rsp.getState().getValue())) {
          List<ListPoolResponseContent> currLbList = rsp.getBody().getContent();
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
          throw new EcloudException("GetPool return null or body is null or res_state is not OK");
        }
        break;
      }
    }
    return poolList;
  }

  public static List<ListLoadBalancerPoolMemberResponseContent> getMemberByPoolIdList(
      Client client, List<ListPoolResponseContent> poolList) {
    List<ListLoadBalancerPoolMemberResponseContent> memberList = new ArrayList<>();
    if (poolList == null || poolList.isEmpty()) {
      return memberList;
    }

    for (ListPoolResponseContent pool : poolList) {
      String poolId = pool.getPoolId();
      int page = 1;
      int size = 50;
      while (true) {
        ListLoadBalancerPoolMemberRequest request = new ListLoadBalancerPoolMemberRequest();
        ListLoadBalancerPoolMemberQuery query = new ListLoadBalancerPoolMemberQuery();
        query.setPage(page);
        query.setPageSize(size);
        ListLoadBalancerPoolMemberPath queryPath = new ListLoadBalancerPoolMemberPath();
        queryPath.setPoolId(poolId);
        request.setListLoadBalancerPoolMemberPath(queryPath);
        request.setListLoadBalancerPoolMemberQuery(query);
        ListLoadBalancerPoolMemberResponse rsp = null;
        rsp = client.listLoadBalancerPoolMember(request);
        if (rsp != null
            && rsp.getBody() != null
            && ListLoadBalancerPoolMemberResponse.StateEnum.OK
                .getValue()
                .equals(rsp.getState().getValue())) {
          List<ListLoadBalancerPoolMemberResponseContent> currLbList = rsp.getBody().getContent();
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
          throw new EcloudException("GetPool return null or body is null or res_state is not OK");
        }
        break;
      }
    }
    return memberList;
  }

  public static EcloudLoadBalancer createEcloudLoadBalancer(ListLoadbalanceRespResponseContent it) {
    EcloudLoadBalancer loadBalancer = new EcloudLoadBalancer();
    loadBalancer.setSubnetId(it.getSubnetId());
    loadBalancer.setVpcName(it.getVpcName());
    loadBalancer.setOrderId(it.getOrderId());
    loadBalancer.setPrivateIp(it.getPrivateIp());
    loadBalancer.setDescription(it.getDescription());
    loadBalancer.setNodeIp(it.getNodeIp());
    loadBalancer.setVipPortId(it.getVipPortId());
    loadBalancer.setSubnetName(it.getSubnetName());
    loadBalancer.setIsMultiAz(it.getIsMultiAz());
    loadBalancer.setIsExclusive(it.getIsExclusive());
    loadBalancer.setIpId(it.getIpId());
    loadBalancer.setProvider(it.getProvider() != null ? it.getProvider().getValue() : "");
    loadBalancer.setRouterId(it.getRouterId());
    loadBalancer.setCreateTime(it.getCreatedTime());
    loadBalancer.setId(it.getId());
    loadBalancer.setLoadBalancerId(it.getId());
    loadBalancer.setAdminStateUp(it.getAdminStateUp());
    loadBalancer.setMeasureType(it.getMeasureType());
    loadBalancer.setEcStatus(it.getEcStatus() != null ? it.getEcStatus().getValue() : "");
    loadBalancer.setVisible(it.getVisible());
    loadBalancer.setProposer(it.getProposer());
    loadBalancer.setPublicIp(it.getPublicIp());
    loadBalancer.setUserName(it.getUserName());
    loadBalancer.setFlavor(it.getFlavor());
    loadBalancer.setDeleted(it.getDeleted());
    loadBalancer.setIpVersion(it.getIpVersion().getValue());
    loadBalancer.setName(it.getName());
    loadBalancer.setLoadBalancerName(it.getName());
    loadBalancer.setOpStatus(it.getOpStatus() != null ? it.getOpStatus().getValue() : "");

    loadBalancer.setLoadBalancerSpec(LbSpecEnum.getDescByCode(it.getFlavor()));
    return loadBalancer;
  }

  public static EcloudLoadBalancerListener createEcloudLoadBalancerListener(
      ListLoadBalanceListenersRespResponseContent tempListener) {
    EcloudLoadBalancerListener e = new EcloudLoadBalancerListener();
    e.setHealthDelay(tempListener.getHealthDelay());
    e.setModifiedTime(tempListener.getModifiedTime());
    e.setGroupType(tempListener.getGroupType());
    e.setRedirectToListenerId(tempListener.getRedirectToListenerId());
    e.setSniContainerIds(tempListener.getSniContainerIds());
    e.setDescription(tempListener.getDescription());
    e.setIsMultiAz(tempListener.getIsMultiAz());
    e.setRedirectToListenerName(tempListener.getRedirectToListenerName());
    e.setProtocol(tempListener.getProtocol() != null ? tempListener.getProtocol().getValue() : "");
    e.setSniContainerIdList(tempListener.getSniContainerIdList());
    e.setCreatedTime(tempListener.getCreatedTime());
    e.setHttp2(tempListener.getHttp2());
    e.setId(tempListener.getId());
    e.setListenerId(tempListener.getId());
    e.setDefaultTlsContainerId(tempListener.getDefaultTlsContainerId());
    e.setMutualAuthenticationUp(tempListener.getMutualAuthenticationUp());
    e.setCookieName(tempListener.getCookieName());
    e.setPoolName(tempListener.getPoolName());
    e.setSniUp(tempListener.getSniUp());
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
    e.setDeleted(tempListener.getDeleted());
    e.setHealthMaxRetries(tempListener.getHealthMaxRetries());
    e.setName(tempListener.getName());
    e.setListenerName(tempListener.getName());
    e.setPoolId(tempListener.getPoolId());
    e.setSessionPersistence(
        tempListener.getSessionPersistence() != null
            ? tempListener.getSessionPersistence().getValue()
            : "");
    e.setGroupEnabled(tempListener.getGroupEnabled());
    e.setHealthUrlPath(tempListener.getHealthUrlPath());
    e.setCaContainerId(tempListener.getCaContainerId());
    e.setOpStatus(tempListener.getOpStatus() != null ? tempListener.getOpStatus().getValue() : "");
    e.setControlGroupId(tempListener.getControlGroupId());
    e.setHealthTimeout(tempListener.getHealthTimeout());
    e.setMultiAzUuid(tempListener.getMultiAzUuid());
    return e;
  }

  public static EcloudLoadBalancerPool createEcloudLoadBalancerPool(ListPoolResponseContent pool) {
    EcloudLoadBalancerPool epool = new EcloudLoadBalancerPool();
    epool.setModifiedTime(pool.getModifiedTime());
    epool.setLbAlgorithm(pool.getLbAlgorithm() != null ? pool.getLbAlgorithm().getValue() : "");
    epool.setLoadBalanceId(pool.getLoadBalanceId());
    epool.setIsMultiAz(pool.getIsMultiAz());
    epool.setListenerId(pool.getListenerId());
    epool.setProtocol(pool.getProtocol() != null ? pool.getProtocol().getValue() : "");
    epool.setDeleted(pool.getDeleted());
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
      ListPoolResponseL7PolicyResps pL7) {
    EcloudLoadBalancerL7Policy eL7 = new EcloudLoadBalancerL7Policy();
    eL7.setModifiedTime(pL7.getModifiedTime());
    eL7.setDescription(pL7.getDescription());
    eL7.setL7PolicyDomainName(pL7.getL7PolicyDomainName());
    eL7.setIsMultiAz(pL7.getIsMultiAz());
    eL7.setL7RuleValue(pL7.getL7RuleValue());
    eL7.setListenerId(pL7.getListenerId());
    eL7.setCompareType(pL7.getCompareType() != null ? pL7.getCompareType().getValue() : "");
    eL7.setDeleted(pL7.getDeleted());
    eL7.setL7PolicyUrl(pL7.getL7PolicyUrl());
    eL7.setL7PolicyName(pL7.getL7PolicyName());
    eL7.setRuleType(pL7.getRuleType() != null ? pL7.getRuleType().getValue() : "");
    eL7.setPoolId(pL7.getPoolId());
    eL7.setCreatedTime(pL7.getCreatedTime());
    eL7.setL7PolicyId(pL7.getL7PolicyId());
    eL7.setPosition(pL7.getPosition());
    eL7.setAdminStateUp(pL7.getAdminStateUp());
    eL7.setMultiAzUuid(pL7.getMultiAzUuid());
    eL7.setPoolName(pL7.getPoolName());
    return eL7;
  }

  public static EcloudLoadBalancerMember createEcloudLoadBalancerMember(
      ListLoadBalancerPoolMemberResponseContent mem) {
    EcloudLoadBalancerMember eMem = new EcloudLoadBalancerMember();
    eMem.setSubnetId(mem.getSubnetId());
    eMem.setVmName(mem.getVmName());
    eMem.setIsDelete(mem.getIsDelete());
    eMem.setProposer(mem.getProposer());
    eMem.setIp(mem.getIp());
    eMem.setDescription(mem.getDescription());
    eMem.setWeight(mem.getWeight());
    eMem.setType(mem.getType());
    eMem.setIsMultiAz(mem.getIsMultiAz());
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
