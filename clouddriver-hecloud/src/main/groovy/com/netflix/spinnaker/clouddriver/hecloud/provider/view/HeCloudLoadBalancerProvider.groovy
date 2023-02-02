package com.netflix.spinnaker.clouddriver.hecloud.provider.view

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.LoadBalancerInstance
import com.netflix.spinnaker.clouddriver.model.LoadBalancerServerGroup
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudProvider
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.clouddriver.hecloud.model.loadbalance.HeCloudLoadBalancer
import com.netflix.spinnaker.clouddriver.model.LoadBalancerProvider
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudTargetHealth
import com.netflix.spinnaker.clouddriver.hecloud.model.loadbalance.HeCloudLoadBalancerListener
import com.netflix.spinnaker.clouddriver.hecloud.model.loadbalance.HeCloudLoadBalancerPool
import com.netflix.spinnaker.clouddriver.hecloud.model.loadbalance.HeCloudLoadBalancerTargetHealth
import com.netflix.spinnaker.clouddriver.hecloud.provider.HeCloudInfrastructureProvider
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.netflix.spinnaker.clouddriver.hecloud.cache.Keys

import static com.netflix.spinnaker.clouddriver.hecloud.cache.Keys.Namespace.APPLICATIONS
import static com.netflix.spinnaker.clouddriver.hecloud.cache.Keys.Namespace.LOAD_BALANCERS
import static com.netflix.spinnaker.clouddriver.hecloud.cache.Keys.Namespace.SERVER_GROUPS
import static com.netflix.spinnaker.clouddriver.hecloud.cache.Keys.Namespace.HEALTH_CHECKS


@Slf4j
@Component
class HeCloudLoadBalancerProvider implements LoadBalancerProvider<HeCloudLoadBalancer> {
  @Autowired
  HeCloudInstanceProvider heCloudInstanceProvider

  final String cloudProvider = HeCloudProvider.ID

  private final Cache cacheView
  final ObjectMapper objectMapper
  private final HeCloudInfrastructureProvider hecloudProvider

  @Autowired
  public HeCloudLoadBalancerProvider(Cache cacheView, HeCloudInfrastructureProvider tProvider, ObjectMapper objectMapper) {
    this.cacheView = cacheView
    this.hecloudProvider = tProvider
    this.objectMapper = objectMapper
  }

  @Override
  Set<HeCloudLoadBalancer> getApplicationLoadBalancers(String applicationName) {
    log.info("Enter hecloud getApplicationLoadBalancers " + applicationName)

    CacheData application = cacheView.get(
      APPLICATIONS.ns,
      Keys.getApplicationKey(applicationName),
      RelationshipCacheFilter.include(LOAD_BALANCERS.ns)
    )

    if (application) {
      def loadBalancerKeys = application.relationships[LOAD_BALANCERS.ns]
      if (loadBalancerKeys) {
        Collection<CacheData> loadBalancers = cacheView.getAll(LOAD_BALANCERS.ns, loadBalancerKeys)
        def loadBalancerSet = translateLoadBalancersFromCacheData(loadBalancers)
        loadBalancerSet.each {
          it.setApplication(applicationName)
        }
        return loadBalancerSet
      }
    }else {
      null
    }
  }

  Set<HeCloudLoadBalancer> getAll() {
    getAllMatchingKeyPattern(Keys.getLoadBalancerKey("*","*","*"))
  }

  Set<HeCloudLoadBalancer> getAllMatchingKeyPattern(String pattern) {
    log.info("Enter getAllMatchingKeyPattern patten = " + pattern)
    loadResults(cacheView.filterIdentifiers(Keys.Namespace.LOAD_BALANCERS.ns, pattern))
  }

  Set<HeCloudLoadBalancer> loadResults(Collection<String> identifiers) {
    log.info("Enter loadResults id = " + identifiers)
    def data = cacheView.getAll(Keys.Namespace.LOAD_BALANCERS.ns, identifiers, RelationshipCacheFilter.none())
    def transformed = data?.collect(this.&fromCacheData)
    return transformed
  }

  private LoadBalancerServerGroup getLoadBalancerServerGroup(CacheData loadBalancerCache, HeCloudLoadBalancer loadBalancerDesc) {
    def serverGroupKeys = loadBalancerCache?.relationships[SERVER_GROUPS.ns]
    if (serverGroupKeys) {
      def serverGroupKey = serverGroupKeys[0]
      if (serverGroupKey) {
        log.info("loadBalancer ${loadBalancerDesc.loadBalancerId} bind serverGroup ${serverGroupKey}")
        def parts = Keys.parse(serverGroupKey)
        def lbServerGroup = new LoadBalancerServerGroup(name: parts.name, account: parts.account, region: parts.region)
        def serverGroup = cacheView.get(SERVER_GROUPS.ns, serverGroupKey)
        if (serverGroup) {
          def asgInfo = serverGroup?.attributes?.asg as Map
          def lbInfo = asgInfo?.get("forwardLoadBalancerSet") as List
          def instances = []
          if (lbInfo) {
            def lbId = loadBalancerDesc?.loadBalancerId
            def lbHealthKey = Keys.getTargetHealthKey(
              lbId, "*", "*", "*", parts.account, parts.region)
            def identifiers = cacheView.filterIdentifiers(HEALTH_CHECKS.ns, lbHealthKey)
            def lbHealthCaches = cacheView.getAll(HEALTH_CHECKS.ns, identifiers, RelationshipCacheFilter.none())
            // some hack here, use fakeId as we just need the total count of instances here
            def fakeId = 0

            lbHealthCaches.each { lbHealthCache ->
              def loadBalancerTargetHealth = lbHealthCache?.attributes?.targetHealth as HeCloudLoadBalancerTargetHealth
              if (loadBalancerTargetHealth) {
                def targetHealth = new HeCloudTargetHealth(loadBalancerTargetHealth.healthStatus)
                def healthStatus = targetHealth.targetHealthStatus
                def instance = new LoadBalancerInstance(
                  id: fakeId.toString(),
                  health:
                    [
                      type: "LoadBalancer",
                      state: healthStatus.toServiceStatus().toString(),
                    ]
                )
                fakeId = fakeId + 1
                instances.add(instance)
              }
            }
            lbServerGroup.instances = instances
          }
        }
        return lbServerGroup
      }
    }
    null
  }

  HeCloudLoadBalancer fromCacheData(CacheData cacheData) {
    //log.info("Enter formCacheDate data = $cacheData.attributes")
    HeCloudLoadBalancer loadBalancerDescription = objectMapper.convertValue(cacheData.attributes, HeCloudLoadBalancer)
    def serverGroup = getLoadBalancerServerGroup(cacheData, loadBalancerDescription)
    if (serverGroup) {
      loadBalancerDescription.serverGroups.add(serverGroup)
    }
    return loadBalancerDescription
  }

  @Override
  List<HeCloudLoadBalancerDetail> byAccountAndRegionAndName(String account, String region, String id) {
    log.info("Get loadBalancer byAccountAndRegionAndName: account=${account},region=${region},id=${id}")
    def lbKey = Keys.getLoadBalancerKey(id, account, region)
    Collection<CacheData> lbCache = cacheView.getAll(LOAD_BALANCERS.ns, lbKey)

    def lbDetails = lbCache.collect {
      def lbDetail = new HeCloudLoadBalancerDetail()
      lbDetail.id = it.attributes.id
      lbDetail.name = it.attributes.name
      lbDetail.account = account
      lbDetail.region = region
      lbDetail.subnetId = it.attributes.subnetId
      lbDetail.vpcId = it.attributes.vpcId
      lbDetail.createTime = it.attributes.createTime
      lbDetail.loadBalancerVip = it.attributes.loadBalancerVip
      lbDetail.listeners = it.attributes.listeners
      lbDetail.pools = it.attributes.pools
      lbDetail
    }
    return lbDetails
  }

  @Override
  List<LoadBalancerProvider.Item> list() {
    log.info("Enter list loadBalancer")
    def searchKey = Keys.getLoadBalancerKey('*', '*', '*')
    Collection<String> identifiers = cacheView.filterIdentifiers(LOAD_BALANCERS.ns, searchKey)
    getSummaryForLoadBalancers(identifiers).values() as List
  }

  @Override
  LoadBalancerProvider.Item get(String id) {
    log.info("Enter Get loadBalancer id ${id}")
    def searchKey = Keys.getLoadBalancerKey(id, '*', '*')
    Collection<String> identifiers = cacheView.filterIdentifiers(LOAD_BALANCERS.ns, searchKey).findAll {
      def key = Keys.parse(it)
      key.id == id
    }
    getSummaryForLoadBalancers(identifiers).get(id)
  }


  private Map<String, HeCloudLoadBalancerSummary> getSummaryForLoadBalancers(Collection<String> loadBalancerKeys) {
    Map<String, HeCloudLoadBalancerSummary> map = [:]
    Collection<CacheData> loadBalancerData = cacheView.getAll(LOAD_BALANCERS.ns, loadBalancerKeys)
    Map<String, CacheData> loadBalancers = loadBalancerData?.collectEntries { [(it.id): it] }

    for (lb in loadBalancerKeys) {
      CacheData loadBalancerFromCache = loadBalancers[lb]
      if (loadBalancerFromCache) {
        def parts = Keys.parse(lb)
        def attributes = loadBalancerFromCache.attributes
        String name = ""
        if (attributes){
          name = loadBalancerFromCache.attributes.name    //loadBalancerId
        }
        String region = parts.region
        String account = parts.account
        def summary = map.get(name)
        if (!summary) {
          summary = new HeCloudLoadBalancerSummary(name: name)
          map.put name, summary
        }
        def loadBalancer = new HeCloudLoadBalancerDetail()
        loadBalancer.account = parts.account
        loadBalancer.region = parts.region
        loadBalancer.id = parts.id
        loadBalancer.subnetId = loadBalancerFromCache.attributes.subnetId
        loadBalancer.vpcId = loadBalancerFromCache.attributes.vpcId
        loadBalancer.name = loadBalancerFromCache.attributes.name

        summary.getOrCreateAccount(account).getOrCreateRegion(region).loadBalancers << loadBalancer
      }
    }
    return map
  }


  private Set<HeCloudLoadBalancer> translateLoadBalancersFromCacheData(Collection<CacheData> loadBalancerData) {

    def transformed = loadBalancerData.collect(this.&fromCacheData)
    return transformed

  }

  // view models...

  static class HeCloudLoadBalancerSummary implements LoadBalancerProvider.Item {
    private Map<String, HeCloudLoadBalancerAccount> mappedAccounts = [:]
    String name

    HeCloudLoadBalancerAccount getOrCreateAccount(String name) {
      if (!mappedAccounts.containsKey(name)) {
        mappedAccounts.put(name, new HeCloudLoadBalancerAccount(name: name))
      }
      mappedAccounts[name]
    }

    @JsonProperty("accounts")
    List<HeCloudLoadBalancerAccount> getByAccounts() {
      mappedAccounts.values() as List
    }
  }

  static class HeCloudLoadBalancerAccount implements LoadBalancerProvider.ByAccount {
    private Map<String, HeCloudLoadBalancerAccountRegion> mappedRegions = [:]
    String name

    HeCloudLoadBalancerAccountRegion getOrCreateRegion(String name) {
      if (!mappedRegions.containsKey(name)) {
        mappedRegions.put(name, new HeCloudLoadBalancerAccountRegion(name: name, loadBalancers: []))
      }
      mappedRegions[name]
    }

    @JsonProperty("regions")
    List<HeCloudLoadBalancerAccountRegion> getByRegions() {
      mappedRegions.values() as List
    }
  }

  static class HeCloudLoadBalancerAccountRegion implements LoadBalancerProvider.ByRegion {
    String name
    List<HeCloudLoadBalancerSummary> loadBalancers
  }

  static class HeCloudLoadBalancerDetail implements LoadBalancerProvider.Details {
    String account
    String region
    String name
    String id      //locadBalancerId
    String type = HeCloudProvider.ID
    String subnetId
    String vpcId
    String createTime
    String loadBalancerVip
    List<HeCloudLoadBalancerListener> listeners
    List<HeCloudLoadBalancerPool> pools
  }

}
