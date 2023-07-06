package com.netflix.spinnaker.clouddriver.ctyun.provider.view

import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.CacheFilter
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.ClusterProvider
import com.netflix.spinnaker.clouddriver.ctyun.CtyunCloudProvider
import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunCluster
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunInstance
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunServerGroup
import com.netflix.spinnaker.clouddriver.ctyun.model.loadbalance.CtyunLoadBalancer
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.*

@Slf4j
@Component
class CtyunClusterProvider implements ClusterProvider<CtyunCluster> {
  @Autowired
  CtyunCloudProvider ctyunCloudProvider

  @Autowired
  CtyunInstanceProvider ctyunInstanceProvider

  @Autowired
  Cache cacheView

  @Override
  Map<String, Set<CtyunCluster>> getClusters() {
    Collection<CacheData> clusterData = cacheView.getAll(CLUSTERS.ns)
    Collection<CtyunCluster> clusters = translateClusters(clusterData, false)
    clusters.groupBy { it.accountName }.collectEntries { k, v ->
      [k, new HashSet(v)]
    } as Map<String, Set<CtyunCluster>>
  }

  @Override
  Map<String, Set<CtyunCluster>> getClusterSummaries(String applicationName) {
    CacheData application = cacheView.get(
      APPLICATIONS.ns,
      Keys.getApplicationKey(applicationName))

    if (application) {
      Collection<CtyunCluster> clusters = translateClusters(
        resolveRelationshipData(application, CLUSTERS.ns),
        false)
      clusters.groupBy { it.accountName }.collectEntries { k, v ->
        [k, new HashSet(v)]
      } as Map<String, Set<CtyunCluster>>
    } else {
      return null
    }
  }

  @Override
  Map<String, Set<CtyunCluster>> getClusterDetails(String applicationName) {
    CacheData application = cacheView.get(
      APPLICATIONS.ns,
      Keys.getApplicationKey(applicationName))

    if (application) {
      log.info("application is ${application.id}.")
      Collection<CtyunCluster> clusters = translateClusters(
        resolveRelationshipData(application, CLUSTERS.ns),
        true)
      clusters.groupBy {
        it.accountName
      }.collectEntries { k, v ->
        [k, new HashSet(v)]
      } as Map<String, Set<CtyunCluster>>
    } else {
      log.info("application is not found.")
      null
    }
  }

  @Override
  Set<CtyunCluster> getClusters(String applicationName, String account) {
    CacheData application = cacheView.get(
      APPLICATIONS.ns,
      Keys.getApplicationKey(applicationName),
      RelationshipCacheFilter.include(CLUSTERS.ns)
    )

    if (application) {
      Collection<String> clusterKeys = application.relationships[CLUSTERS.ns].findAll {
        Keys.parse(it).account == account
      }
      Collection<CacheData> clusters = cacheView.getAll(CLUSTERS.ns, clusterKeys)
      translateClusters(clusters, true) as Set<CtyunCluster>
    } else {
      null
    }
  }

  @Override
  CtyunCluster getCluster(
    String application, String account, String name, boolean includeDetails) {
    CacheData cluster = cacheView.get(
      CLUSTERS.ns,
      Keys.getClusterKey(name, application, account))

    cluster ? translateClusters([cluster], includeDetails)[0] : null
  }

  @Override
  CtyunCluster getCluster(String applicationName, String accountName, String clusterName) {
    getCluster(applicationName, accountName, clusterName, true)
  }

  @Override
  CtyunServerGroup getServerGroup(
    String account, String region, String name, boolean includeDetails) {
    String serverGroupKey = Keys.getServerGroupKey name, account, region
    CacheData serverGroupData = cacheView.get SERVER_GROUPS.ns, serverGroupKey
    if (serverGroupData) {
      String imageId = Optional.ofNullable(serverGroupData.attributes)
        .map({ attribute -> attribute.launchConfig })
        .map({ launch -> launch["imageID"] })
        .orElse(null)
//      String imageId = serverGroupData.attributes.launchConfig["imageId"]
      CacheData imageConfig = imageId ? cacheView.get(
        IMAGES.ns,
        Keys.getImageKey(imageId, account, region)
      ) : null


      def serverGroup = new CtyunServerGroup(serverGroupData.attributes)
      serverGroup.accountName = account
      serverGroup.image = imageConfig ? imageConfig.attributes.image as Map : null

      if (includeDetails) {
        // show instances info
        serverGroup.instances = getServerGroupInstances(account, region, serverGroupData)
      }
      serverGroup.loadBlanders.each {
        String loadBalancerKey = Keys.getLoadBalancerKey it.lbID, account, region
        CacheData loadBalancersData = cacheView.get LOAD_BALANCERS.ns, loadBalancerKey
        if(serverGroup.loadBlanders?.size()>0){
          Map<String,Object> map=loadBalancersData.attributes.listeners.find {ss->
            ss.targetGroupId==it.hostGroupID
          }
          if(map!=null&&map.get("listenerId")!=null&&String.valueOf(map.get("listenerId")).size()>0){
            map.remove("rules");
            it.put("listener",map)
          }else {
            //查转发规则
            Collection<Map> listeners = loadBalancersData.attributes.listeners.findAll { liss->
              liss.rules.size()>0
            }
            Map<String,Object> listenermap = listeners.find {li->
              def ru = li.rules.find{rule->
                rule.ruleTargetGroupId == it.hostGroupID
              }
              if(ru!=null&&ru.get("locationId")!=null && String.valueOf(ru.get("locationId")).size()>0){
                li.put("rule",ru)
                return true
              }
            }
            listenermap.remove("rules");
            it.put("listener",listenermap)
          }
        }
      }
      serverGroup
    } else {
      null
    }
  }

  @Override
  CtyunServerGroup getServerGroup(String account, String region, String name) {
    getServerGroup(account, region, name, true)
  }

  @Override
  String getCloudProviderId() {
    return ctyunCloudProvider.id
  }

  @Override
  boolean supportsMinimalClusters() {
    return true
  }

  Integer getServerGroupAsgId(String serverGroupName, String account, String region) {
    def serverGroup = getServerGroup(account, region, serverGroupName, false)
    serverGroup ? serverGroup.asg.groupID as Integer : null
  }

  private Collection<CtyunCluster> translateClusters(
    Collection<CacheData> clusterData,
    boolean includeDetails) {

    // todo test lb detail
    Map<String, CtyunLoadBalancer> loadBalancers
    Map<String, CtyunServerGroup> serverGroups

    if (includeDetails) {
      Collection<CacheData> allLoadBalancers = resolveRelationshipDataForCollection(
        clusterData,
        LOAD_BALANCERS.ns
      )
      Collection<CacheData> allServerGroups = resolveRelationshipDataForCollection(
        clusterData,
        SERVER_GROUPS.ns,
        RelationshipCacheFilter.include(INSTANCES.ns, LAUNCH_CONFIGS.ns)
      )
      loadBalancers = translateLoadBalancers(allLoadBalancers)
      serverGroups = translateServerGroups(allServerGroups)
    } else {
      Collection<CacheData> allServerGroups = resolveRelationshipDataForCollection(
        clusterData,
        SERVER_GROUPS.ns,
        RelationshipCacheFilter.include(INSTANCES.ns)
      )
      serverGroups = translateServerGroups(allServerGroups)
    }

    Collection<CtyunCluster> clusters = clusterData.collect { CacheData clusterDataEntry ->
      Map<String, String> clusterKey = Keys.parse(clusterDataEntry.id)
      CtyunCluster cluster = new CtyunCluster()
      cluster.accountName = clusterKey.account
      cluster.name = clusterKey.cluster
      cluster.serverGroups = clusterDataEntry.relationships[SERVER_GROUPS.ns]?.findResults {
        serverGroups.get(it)
      }

      if (includeDetails) {
        def lb = clusterDataEntry.relationships[LOAD_BALANCERS.ns]?.findResults {
          loadBalancers.get(it)
        }
        cluster.loadBalancers = lb
      } else {
        cluster.loadBalancers = clusterDataEntry.relationships[LOAD_BALANCERS.ns]?.collect { loadBalancerKey ->
          Map parts = Keys.parse(loadBalancerKey)
          new CtyunLoadBalancer(
            id: parts.id,
            accountName: parts.account,
            region: parts.region
          )
        }
      }
      cluster
    }
    clusters
  }

  private static Map<String, CtyunLoadBalancer> translateLoadBalancers(
    Collection<CacheData> loadBalancerData) {
    loadBalancerData.collectEntries { loadBalancerEntry ->
      Map<String, String> lbKey = Keys.parse(loadBalancerEntry.id)
      [(loadBalancerEntry.id): new CtyunLoadBalancer(
        id: lbKey.id, accountName: lbKey.account, region: lbKey.region)]
    }
  }

  private Map<String, CtyunServerGroup> translateServerGroups(
    Collection<CacheData> serverGroupData) {
    Map<String, CtyunServerGroup> serverGroups = serverGroupData.collectEntries { serverGroupEntry ->
      CtyunServerGroup serverGroup = new CtyunServerGroup(serverGroupEntry.attributes)

      def account = serverGroup.accountName
      def region = serverGroup.region

      serverGroup.instances = getServerGroupInstances(account, region, serverGroupEntry)

      String imageId = Optional.ofNullable(serverGroupEntry.attributes)
        .map({ attribute -> attribute.launchConfig })
        .map({ launch -> launch["imageID"] })
        .orElse(null)
      log.info("muyi ctyun cluster provider imageId:{}",imageId)
      if (org.springframework.util.StringUtils.isEmpty(imageId)){
        log.info("muyi attributes:{}",serverGroupEntry.attributes)
      }
//      String imageId = serverGroupEntry.attributes.launchConfig["imageId"]
      CacheData imageConfig = imageId ? cacheView.get(
        IMAGES.ns,
        Keys.getImageKey(imageId, account, region)
      ) : null

      serverGroup.image = imageConfig ? imageConfig.attributes.image as Map : null

      [(serverGroupEntry.id): serverGroup]
    }
    serverGroups
  }

  private Set<CtyunInstance> getServerGroupInstances(String account, String region, CacheData serverGroupData) {
    def instanceKeys = serverGroupData.relationships[INSTANCES.ns]
    Collection<CacheData> instances = cacheView.getAll(
      INSTANCES.ns,
      instanceKeys
    )

    instances.collect {
      ctyunInstanceProvider.instanceFromCacheData(account, region, it)
    }
  }

  private Collection<CacheData> resolveRelationshipData(CacheData source, String relationship) {
    resolveRelationshipData(source, relationship) { true }
  }

  private Collection<CacheData> resolveRelationshipData(
    CacheData source,
    String relationship,
    Closure<Boolean> relFilter,
    CacheFilter cacheFilter = null) {
    Collection<String> filteredRelationships = source.relationships[relationship]?.findAll(relFilter)
    filteredRelationships ? cacheView.getAll(relationship, filteredRelationships, cacheFilter) : []
  }

  private Collection<CacheData> resolveRelationshipDataForCollection(
    Collection<CacheData> sources,
    String relationship,
    CacheFilter cacheFilter = null) {

    Collection<String> relationships = sources?.findResults {
      it.relationships[relationship] ?: []
    }?.flatten() ?: []

    relationships ? cacheView.getAll(relationship, relationships, cacheFilter) : []
  }
}
