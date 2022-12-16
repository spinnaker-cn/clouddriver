package com.netflix.spinnaker.clouddriver.hecloud.client

import com.hecloud.sdk.elb.ElbClient
import com.hecloud.sdk.elb.model.*
import com.hecloud.sdk.elb.utils.LinkUtils
import com.netflix.spinnaker.clouddriver.hecloud.constants.HeCloudConstants
import com.netflix.spinnaker.clouddriver.hecloud.exception.HeCloudOperationException

import com.huaweicloud.sdk.core.auth.BasicCredentials
import com.huaweicloud.sdk.core.exception.ServiceResponseException
import com.huaweicloud.sdk.core.http.HttpConfig
import com.huaweicloud.sdk.core.region.Region
import groovy.util.logging.Slf4j

@Slf4j
class HeCloudLoadBalancerClient {
  private final DEFAULT_LIMIT = 100
  String region
  String account
  ElbClient client

  HeCloudLoadBalancerClient(String accessKeyId, String accessSecretKey, String region, String account) {
    def auth = new BasicCredentials().withAk(accessKeyId).withSk(accessSecretKey).withIamEndpoint(HeCloudConstants.Region.getIamEndPoint(region))
    def regionId = new Region(region, "https://elb." + region + "." + HeCloudConstants.END_POINT_SUFFIX)
    def config = HttpConfig.getDefaultHttpConfig()
    this.region = region
    this.account = account
    client = ElbClient.newBuilder()
      .withHttpConfig(config)
      .withCredential(auth)
      .withRegion(regionId)
      .build()
  }

  List<LoadBalancer> getAllLoadBalancer() {
    List<LoadBalancer> loadBalancerAll = []
    def req = new ListLoadBalancersRequest().withLimit(DEFAULT_LIMIT)
    def resp
    try {
      resp = client.listLoadBalancers(req)
    } catch (ServiceResponseException e) {
      log.error(
        "Unable to listLoadBalancers (limit: {}, region: {}, account: {})",
        String.valueOf(DEFAULT_LIMIT),
        region,
        account,
        e
      )
    }
    def loadBalancers = resp.getLoadbalancers()
    if (loadBalancers != null) {
      loadBalancerAll.addAll(loadBalancers)
    }
    while (LinkUtils.hasNextOrPrevious(resp.getLoadbalancersLinks(), Link.RelEnum.NEXT)) {
      req.setMarker(LinkUtils.getMarker(resp.getLoadbalancersLinks(), Link.RelEnum.NEXT));
      try {
        resp = client.listLoadBalancers(req)
      } catch (ServiceResponseException e) {
        log.error(
          "Unable to listLoadBalancers (limit: {}, marker: {}, region: {}, account: {})",
          String.valueOf(DEFAULT_LIMIT),
          req.getMarker()?.toString(),
          region,
          account,
          e
        )
      }
      loadBalancers = resp.getLoadbalancers()
      loadBalancerAll.addAll(loadBalancers)
    }
    return loadBalancerAll

  }


  List<LoadBalancer> getLoadBalancerById(String id) {
    try {
      def req = new ListLoadBalancersRequest().withId(id)
      def resp = client.listLoadBalancers(req);
      return resp.getLoadbalancers()
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }


  List<LBListener> getAllLBListener(List<String> loadbalancerId) {
    List<LBListener> listenerAll = []

    def req = new ListLBListenerRequest().withLimit(DEFAULT_LIMIT)
    if (loadbalancerId.size() > 0) {
      req.setLoadbalancerId(loadbalancerId)
    }
    def resp
    try {
      resp = client.listListeners(req)
    } catch (ServiceResponseException e) {
      log.error(
        "Unable to listListeners (limit: {}, marker: {}, region: {}, account: {})",
        String.valueOf(DEFAULT_LIMIT),
        req.getMarker()?.toString(),
        region,
        account,
        e
      )
    }
    def listeners = resp.getLblistener()
    if (listeners != null) {
      listenerAll.addAll(listeners)
    }

    while (LinkUtils.hasNextOrPrevious(resp.getLblistenerLinks(), Link.RelEnum.NEXT)) {
      req.setMarker(LinkUtils.getMarker(resp.getLblistenerLinks(), Link.RelEnum.NEXT))
      try {
        resp = client.listListeners(req)
      } catch (ServiceResponseException e) {
        log.error(
          "Unable to listListeners (limit: {}, marker: {}, region: {}, account: {})",
          String.valueOf(DEFAULT_LIMIT),
          req.getMarker()?.toString(),
          region,
          account,
          e
        )
      }
      listeners = resp.getLblistener()
      listenerAll.addAll(listeners)
    }
    return listenerAll

  }

  List<L7policyResp> getAllL7policies(List<String> listenerId) {
    List<L7policyResp> l7policiesAll = []
    def req = new ListL7policiesRequest().withLimit(DEFAULT_LIMIT)
    if (listenerId.size() > 0) {
      req.setListenerId(listenerId)
    }
    def resp
    try {
      resp = client.listL7policies(req)
    } catch (ServiceResponseException e) {
      log.error(
        "Unable to listL7policies (limit: {}, marker: {}, region: {}, account: {})",
        String.valueOf(DEFAULT_LIMIT),
        req.getMarker()?.toString(),
        region,
        account,
        e
      )
    }
    def l7policies = resp.getL7policies()
    if (l7policies != null) {
      l7policiesAll.addAll(l7policies)
    }
    while (LinkUtils.hasNextOrPrevious(resp.getL7policiesLinks(), Link.RelEnum.NEXT)) {
      req.setMarker(LinkUtils.getMarker(resp.getL7policiesLinks(), Link.RelEnum.NEXT))
      try {
        resp = client.listL7policies(req)
      } catch (ServiceResponseException e) {
        log.error(
          "Unable to listL7policies (limit: {}, marker: {}, region: {}, account: {})",
          String.valueOf(DEFAULT_LIMIT),
          req.getMarker()?.toString(),
          region,
          account,
          e
        )
      }
      l7policies = resp.getL7policies()
      l7policiesAll.addAll(l7policies)
    }
    return l7policiesAll

  }

  List<Pool> getAllPools(List<String> loadbalancerId) {
    List<Pool> poolsAll = []
    def req = new ListPoolsRequest().withLimit(DEFAULT_LIMIT)
    if (loadbalancerId.size() > 0) {
      req.setLoadbalancerId(loadbalancerId)
    }
    def resp
    try {
      resp = client.listPools(req)
    } catch (ServiceResponseException e) {
      log.error(
        "Unable to listPools (limit: {}, marker: {}, region: {}, account: {})",
        String.valueOf(DEFAULT_LIMIT),
        req.getMarker()?.toString(),
        region,
        account,
        e
      )
    }
    def pools = resp.getPools()
    if (pools != null) (
      poolsAll.addAll(pools)
    )

    while (LinkUtils.hasNextOrPrevious(resp.getPoolsLinks(), Link.RelEnum.NEXT)) {
      req.setMarker(LinkUtils.getMarker(resp.getPoolsLinks(), Link.RelEnum.NEXT))
      try {
        resp = client.listPools(req)
      } catch (ServiceResponseException e) {
        log.error(
          "Unable to listPools (limit: {}, marker: {}, region: {}, account: {})",
          String.valueOf(DEFAULT_LIMIT),
          req.getMarker()?.toString(),
          region,
          account,
          e
        )
      }
      pools = resp.getPools()
      poolsAll.addAll(pools)
    }
    return poolsAll

  }

  Pool getPool(String poolId) {
    try {
      def req = new ShowPoolRequest().withPoolId(poolId)
      def resp = client.showPool(req);
      return resp.getPool()
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }

  List<Healthmonitors> getAllHealthMonitors() {
    List<Healthmonitors> healthMonitorsAll = []

    def req = new HealthmonitorsRequest().withLimit(DEFAULT_LIMIT)
    def resp
    try {
      resp = client.healthmonitors(req)
    } catch (ServiceResponseException e) {
      log.error(
        "Unable to healthmonitors (limit: {}, marker: {}, region: {}, account: {})",
        String.valueOf(DEFAULT_LIMIT),
        req.getMarker()?.toString(),
        region,
        account,
        e
      )
    }
    def healthMonitors = resp.getHealthmonitors()
    if (healthMonitors != null) {
      healthMonitorsAll.addAll(healthMonitors)
    }
    while (LinkUtils.hasNextOrPrevious(resp.getHealthmonitorsLinks(), Link.RelEnum.NEXT)) {
      req.setMarker(LinkUtils.getMarker(resp.getHealthmonitorsLinks(), Link.RelEnum.NEXT))
      try {
        resp = client.healthmonitors(req)
      } catch (ServiceResponseException e) {
        log.error(
          "Unable to healthmonitors (limit: {}, marker: {}, region: {}, account: {})",
          String.valueOf(DEFAULT_LIMIT),
          req.getMarker()?.toString(),
          region,
          account,
          e
        )
      }
      healthMonitors = resp.getHealthmonitors()
      healthMonitorsAll.addAll(healthMonitors)
    }
    return healthMonitorsAll

  }

  List<Member> getAllMembers() {
    List<Member> membersAll = []
    List<Pool> pools = this.getAllPools()
    pools.each {
      def poolId = it.getId()
      def req = new ListMembersRequest().withPoolId(poolId).withLimit(DEFAULT_LIMIT)
      def resp
      try {
        resp = client.listMembers(req)
      } catch (ServiceResponseException e) {
        log.error(
          "Unable to listMembers (limit: {}, marker: {}, region: {}, account: {})",
          String.valueOf(DEFAULT_LIMIT),
          req.getMarker()?.toString(),
          region,
          account,
          e
        )
      }

      def members = resp.getMembers()
      if (members != null) {
        members.each {
          it.setPoolId(poolId)
        }
        membersAll.addAll(members)
      }
      while (LinkUtils.hasNextOrPrevious(resp.getMembersLinks(), Link.RelEnum.NEXT)) {
        req.setMarker(LinkUtils.getMarker(resp.getMembersLinks(), Link.RelEnum.NEXT))
        try {
          resp = client.listMembers(req)
        } catch (ServiceResponseException e) {
          log.error(
            "Unable to listMembers (limit: {}, marker: {}, region: {}, account: {})",
            String.valueOf(DEFAULT_LIMIT),
            req.getMarker()?.toString(),
            region,
            account,
            e
          )
        }
        members = resp.getMembers()
        if (members != null) {
          members.each {
            it.setPoolId(poolId)
          }
          membersAll.addAll(members)
        }
      }
    }

    return membersAll

  }

  List<Pool> getAllPools() {
    List<Pool> poolsAll = []
    def req = new ListPoolsRequest().withLimit(DEFAULT_LIMIT)
    def resp
    try {
      resp = client.listPools(req)
    } catch (ServiceResponseException e) {
      log.error(
        "Unable to listPools (limit: {}, marker: {}, region: {}, account: {})",
        String.valueOf(DEFAULT_LIMIT),
        req.getMarker()?.toString(),
        region,
        account,
        e
      )
    }
    def pools = resp.getPools()
    if (pools != null) {
      poolsAll.addAll(pools)
    }
    while (LinkUtils.hasNextOrPrevious(resp.getPoolsLinks(), Link.RelEnum.NEXT)) {
      req.setMarker(LinkUtils.getMarker(resp.getPoolsLinks(), Link.RelEnum.NEXT))
      try {
        resp = client.listPools(req)
      } catch (ServiceResponseException e) {
        log.error(
          "Unable to listPools (limit: {}, marker: {}, region: {}, account: {})",
          String.valueOf(DEFAULT_LIMIT),
          req.getMarker()?.toString(),
          region,
          account,
          e
        )
      }
      pools = resp.getPools()
      poolsAll.addAll(pools)
    }
    return poolsAll

  }



}
