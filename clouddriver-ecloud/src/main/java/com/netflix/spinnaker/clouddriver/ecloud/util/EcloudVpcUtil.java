package com.netflix.spinnaker.clouddriver.ecloud.util;

import com.alibaba.fastjson2.JSON;
import com.ecloud.sdk.vpc.v1.Client;
import com.ecloud.sdk.vpc.v1.model.GetVpcDetailRespByRouterIdPath;
import com.ecloud.sdk.vpc.v1.model.GetVpcDetailRespByRouterIdRequest;
import com.ecloud.sdk.vpc.v1.model.GetVpcDetailRespByRouterIdResponse;
import com.ecloud.sdk.vpc.v1.model.GetVpcDetailRespByRouterIdResponseBody;
import com.ecloud.sdk.vpc.v1.model.ListSubnetsQuery;
import com.ecloud.sdk.vpc.v1.model.ListSubnetsRequest;
import com.ecloud.sdk.vpc.v1.model.ListSubnetsResponse;
import com.ecloud.sdk.vpc.v1.model.ListSubnetsResponseContent;
import com.ecloud.sdk.vpc.v1.model.ListVpcQuery;
import com.ecloud.sdk.vpc.v1.model.ListVpcRequest;
import com.ecloud.sdk.vpc.v1.model.ListVpcResponse;
import com.ecloud.sdk.vpc.v1.model.ListVpcResponseContent;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-21
 */
@Slf4j
public final class EcloudVpcUtil {
  private EcloudVpcUtil() {}

  public static List<ListVpcResponseContent> getVpcList(Client client) {
    List<ListVpcResponseContent> vpcList = new ArrayList<>();
    int page = 1;
    int size = 50;
    while (true) {
      ListVpcRequest request = ListVpcRequest.builder().build();
      ListVpcQuery listVpcQuery = new ListVpcQuery();
      listVpcQuery.setPage(page);
      listVpcQuery.setPageSize(size);
      request.setListVpcQuery(listVpcQuery);
      ListVpcResponse result = client.listVpc(request);

      if (result != null && result.getBody() != null) {
        List<ListVpcResponseContent> temVpcList = result.getBody().getContent();
        if (!CollectionUtils.isEmpty(temVpcList)) {
          vpcList.addAll(temVpcList);
          if (vpcList.size() < result.getBody().getTotal()) {
            page++;
            continue;
          }
        }
      } else {
        log.error(
            "res is null or res_body is null or res_state is not OK,res detail={}",
            JSON.toJSONString(result));
      }
      break;
    }
    return vpcList;
  }

  public static List<ListSubnetsResponseContent> getSubnetsByVpcId(String vpcId, Client client) {
    List<ListSubnetsResponseContent> orginSubNets = new ArrayList<>();
    int page = 1;
    int size = 50;
    while (true) {
      ListSubnetsRequest request = new ListSubnetsRequest();
      ListSubnetsQuery query = new ListSubnetsQuery();
      query.setVpcId(vpcId);
      query.setPage(page);
      query.setPageSize(size);
      request.setListSubnetsQuery(query);
      ListSubnetsResponse result = client.listSubnets(request);

      if (result != null && result.getBody() != null) {
        List<ListSubnetsResponseContent> temSubNets = result.getBody().getContent();
        if (!CollectionUtils.isEmpty(temSubNets)) {
          orginSubNets.addAll(temSubNets);
          if (orginSubNets.size() < result.getBody().getTotal()) {
            page++;
            continue;
          }
        }
      } else {
        log.error(
            "res is null or res_body is null or res_state is not OK,res detail={}",
            JSON.toJSONString(result));
      }
      break;
    }
    return orginSubNets;
  }

  public static GetVpcDetailRespByRouterIdResponseBody getVpcInfoByRouteId(
      String routeId, Client client) {
    GetVpcDetailRespByRouterIdRequest request = new GetVpcDetailRespByRouterIdRequest();
    GetVpcDetailRespByRouterIdPath query = new GetVpcDetailRespByRouterIdPath();
    query.setRouterId(routeId);
    request.setGetVpcDetailRespByRouterIdPath(query);
    GetVpcDetailRespByRouterIdResponse result = client.getVpcDetailRespByRouterId(request);
    if (result != null
        && GetVpcDetailRespByRouterIdResponse.StateEnum.OK
            .getValue()
            .equals(result.getState().getValue())
        && result.getBody() != null) {
      return result.getBody();
    } else {
      log.error(
          "res is null or res_body is null or res_state is not OK,res detail={}",
          JSON.toJSONString(result));
    }
    return null;
  }
}
