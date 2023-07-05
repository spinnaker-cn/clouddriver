package com.netflix.spinnaker.clouddriver.ctyun.provider.agent

import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys
import com.netflix.spinnaker.clouddriver.ctyun.client.CloudVirtualMachineClient
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunImage
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.MutableCacheData
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.IMAGES
import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.NAMED_IMAGES

@Slf4j
@InheritConstructors
class CtyunImageCachingAgent extends AbstractCtyunCachingAgent {

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(IMAGES.ns)
  ] as Set

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info "start load image data"

    Map<String, Collection<CacheData>> cacheResults = [:]
    Map<String, Map<String, CacheData>> namespaceCache = [:].withDefault {
      namespace -> [:].withDefault { id -> new MutableCacheData(id as String) }
    }
    Map<String, Collection<String>> evictions = [:].withDefault {
      namespace -> []
    }

    CloudVirtualMachineClient cvmClient = new CloudVirtualMachineClient(
      credentials.credentials.accessKey,
      credentials.credentials.securityKey,
      region
    )

    def result = []
    //0	private	私有镜像。
    //1	public	公共镜像。
    //2	shared	共享镜像。
    //3	safe	安全产品镜像。
    //4	community	甄选应用镜像。
    def visibilityList=[0,1,2,3,4]
    for(int visibility:visibilityList){
      result.addAll(cvmClient.getImages(visibility))
    }


    result.each {
      def ctyunImage = new CtyunImage(
        region: this.region,
        name: it.imageName,
        imageId: it.imageID,
        type: it.visibility,
        osPlatform: it.osType,
        createdTime: it.createdTime
      )

    /*  if (it.snapshotSet) {
        def snapshotSet = it.snapshotSet.collect {
          Map<String, Object> snapshot = objectMapper.convertValue it, ATTRIBUTES
          snapshot
        }
        ctyunImage.snapshotSet = snapshotSet
      }*/

      def images = namespaceCache[IMAGES.ns]
      def namedImages = namespaceCache[NAMED_IMAGES.ns]
      def imageKey = Keys.getImageKey ctyunImage.id, this.accountName, this.region
      def namedImageKey = Keys.getNamedImageKey ctyunImage.name, this.accountName
      images[imageKey].attributes.image = ctyunImage
      images[imageKey].attributes.snapshotSet = ctyunImage.snapshotSet
      images[imageKey].relationships[NAMED_IMAGES.ns].add namedImageKey

      def originImageCache = providerCache.get(IMAGES.ns, imageKey)
      if (originImageCache) {
        def imageNames = originImageCache.relationships[NAMED_IMAGES.ns]
        if (imageNames && imageNames[0] != namedImageKey) {
          evictions[NAMED_IMAGES.ns].addAll(originImageCache.relationships[NAMED_IMAGES.ns])
        }
      }

      namedImages[namedImageKey].attributes.imageName = ctyunImage.name
      namedImages[namedImageKey].attributes.type = ctyunImage.type
      namedImages[namedImageKey].attributes.osPlatform = ctyunImage.osPlatform
      namedImages[namedImageKey].attributes.snapshotSet = ctyunImage.snapshotSet
      namedImages[namedImageKey].attributes.createdTime = ctyunImage.createdTime
      namedImages[namedImageKey].attributes.imageId = ctyunImage.imageId
      namedImages[namedImageKey].relationships[IMAGES.ns].add imageKey
      null
    }

    namespaceCache.each { String namespace, Map<String, CacheData> cacheDataMap ->
      cacheResults[namespace] = cacheDataMap.values()
    }

    CacheResult defaultCacheResult = new DefaultCacheResult(cacheResults, evictions)
    log.info 'finish loads image data.'
    log.info "Caching ${namespaceCache[IMAGES.ns].size()} items in $agentType"
    defaultCacheResult
  }
}

