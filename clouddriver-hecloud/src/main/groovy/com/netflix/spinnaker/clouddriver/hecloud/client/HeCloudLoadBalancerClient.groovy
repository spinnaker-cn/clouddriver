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
  ElbClient client

  HeCloudLoadBalancerClient(String accessKeyId, String accessSecretKey, String region){
    def auth = new BasicCredentials().withAk(accessKeyId).withSk(accessSecretKey).withIamEndpoint(HeCloudConstants.Region.getIamEndPoint(region))
    def regionId = new Region(region, "https://elb." + region + "." + HeCloudConstants.END_POINT_SUFFIX)
    def config = HttpConfig.getDefaultHttpConfig()
    client = ElbClient.newBuilder()
        .withHttpConfig(config)
        .withCredential(auth)
        .withRegion(regionId)
        .build()
  }

  List<LoadBalancer> getAllLoadBalancer() {
    List<LoadBalancer> loadBalancerAll = []
    try {
      def req = new ListLoadBalancersRequest().withLimit(DEFAULT_LIMIT)
      def resp = client.listLoadBalancers(req)
      def loadBalancers = resp.getLoadbalancers()
      if(loadBalancers != null){
        loadBalancerAll.addAll(loadBalancers)
      }
      while (LinkUtils.hasNextOrPrevious(resp.getLoadbalancersLinks(),Link.RelEnum.NEXT)){
        req.setMarker(LinkUtils.getMarker(resp.getLoadbalancersLinks(),Link.RelEnum.NEXT));
        resp = client.listLoadBalancers(req)
        loadBalancers = resp.getLoadbalancers()
        loadBalancerAll.addAll(loadBalancers)
      }
      return loadBalancerAll
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }



  List<LoadBalancer> getLoadBalancerById(String id) {
    try{
      def req = new ListLoadBalancersRequest().withId(id)
      def resp = client.listLoadBalancers(req);
      return resp.getLoadbalancers()
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }


  List<LBListener> getAllLBListener(List<String> loadbalancerId) {
    List<LBListener> listenerAll = []

    try {
      def req = new ListLBListenerRequest().withLimit(DEFAULT_LIMIT)
      if (loadbalancerId.size() > 0) {
        req.setLoadbalancerId(loadbalancerId)
      }
      def resp = client.listListeners(req)
      def listeners = resp.getLblistener()
      if(listeners != null){
        listenerAll.addAll(listeners)
      }

      while (LinkUtils.hasNextOrPrevious(resp.getLblistenerLinks(),Link.RelEnum.NEXT)) {
        req.setMarker(LinkUtils.getMarker(resp.getLblistenerLinks(),Link.RelEnum.NEXT))
        resp = client.listListeners(req)
        listeners = resp.getLblistener()
        listenerAll.addAll(listeners)
      }
      return listenerAll
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }

  List<L7policyResp> getAllL7policies(List<String> listenerId) {
    List<L7policyResp> l7policiesAll = []
    try {
      def req = new ListL7policiesRequest().withLimit(DEFAULT_LIMIT)
      if (listenerId.size() > 0) {
        req.setListenerId(listenerId)
      }
      def resp = client.listL7policies(req)
      def l7policies = resp.getL7policies()
      if(l7policies != null){
        l7policiesAll.addAll(l7policies)
      }
      while (LinkUtils.hasNextOrPrevious(resp.getL7policiesLinks(),Link.RelEnum.NEXT)) {
        req.setMarker(LinkUtils.getMarker(resp.getL7policiesLinks(),Link.RelEnum.NEXT))
        resp = client.listL7policies(req)
        l7policies = resp.getL7policies()
        l7policiesAll.addAll(l7policies)
      }
      return l7policiesAll
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }

  List<Pool> getAllPools(List<String> loadbalancerId) {
    List<Pool> poolsAll = []
    try {
      def req = new ListPoolsRequest().withLimit(DEFAULT_LIMIT)
      if (loadbalancerId.size() > 0) {
        req.setLoadbalancerId(loadbalancerId)
      }
      def resp = client.listPools(req)
      def pools = resp.getPools()
      if(pools != null)(
        poolsAll.addAll(pools)
      )

      while (LinkUtils.hasNextOrPrevious(resp.getPoolsLinks(),Link.RelEnum.NEXT)) {
        req.setMarker(LinkUtils.getMarker(resp.getPoolsLinks(),Link.RelEnum.NEXT))
        resp = client.listPools(req)
        pools = resp.getPools()
        poolsAll.addAll(pools)
      }
      return poolsAll
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }

  Pool getPool(String poolId) {
    try{
      def req = new ShowPoolRequest().withPoolId(poolId)
      def resp = client.showPool(req);
      return resp.getPool()
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }

  List<Healthmonitors> getAllHealthMonitors() {
    List<Healthmonitors> healthMonitorsAll = []
    try {
      def req = new HealthmonitorsRequest().withLimit(DEFAULT_LIMIT)
      def resp = client.healthmonitors(req)
      def healthMonitors = resp.getHealthmonitors()
      if(healthMonitors != null){
        healthMonitorsAll.addAll(healthMonitors)
      }
      while (LinkUtils.hasNextOrPrevious(resp.getHealthmonitorsLinks(),Link.RelEnum.NEXT)) {
        req.setMarker(LinkUtils.getMarker(resp.getHealthmonitorsLinks(),Link.RelEnum.NEXT))
        resp = client.healthmonitors(req)
        healthMonitors = resp.getHealthmonitors()
        healthMonitorsAll.addAll(healthMonitors)
      }
      return healthMonitorsAll
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }

  List<Member> getAllMembers() {
    List<Member> membersAll = []
    try{
      List<Pool> pools = this.getAllPools()
      pools.each {
        def poolId = it.getId()
        def req = new ListMembersRequest().withPoolId(poolId).withLimit(DEFAULT_LIMIT)
        def resp = client.listMembers(req)
        resp.getMembers().collect{
          it.setPoolId(poolId)
        }
        def members = resp.getMembers()
        if(members != null ){
          membersAll.addAll(members)
        }
        while (LinkUtils.hasNextOrPrevious(resp.getMembersLinks(),Link.RelEnum.NEXT)) {
          req.setMarker(LinkUtils.getMarker(resp.getMembersLinks(),Link.RelEnum.NEXT))
          resp = client.listMembers(req)
          members = resp.getMembers()
          membersAll.addAll(members)
        }
      }

      return membersAll
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }

  List<Pool> getAllPools() {
    List<Pool> poolsAll = []
    try {
      def req = new ListPoolsRequest().withLimit(DEFAULT_LIMIT)
      def resp = client.listPools(req)
      def pools = resp.getPools()
      if(pools != null){
        poolsAll.addAll(pools)
      }
      while (LinkUtils.hasNextOrPrevious(resp.getPoolsLinks(),Link.RelEnum.NEXT)) {
        req.setMarker(LinkUtils.getMarker(resp.getPoolsLinks(),Link.RelEnum.NEXT))
        resp = client.listPools(req)
        pools = resp.getPools()
        poolsAll.addAll(pools)
      }
      return poolsAll
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }

}
