package com.netflix.spinnaker.clouddriver.hecloud.provider.agent

import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.hecloud.cache.Keys
import com.netflix.spinnaker.clouddriver.hecloud.client.HeCloudImageClient
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudImage
import com.netflix.spinnaker.clouddriver.hecloud.provider.view.MutableCacheData
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.hecloud.cache.Keys.Namespace.IMAGES
import static com.netflix.spinnaker.clouddriver.hecloud.cache.Keys.Namespace.NAMED_IMAGES

@Slf4j
@InheritConstructors
class HeCloudImageCachingAgent extends AbstractHeCloudCachingAgent {

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(IMAGES.ns),
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

    HeCloudImageClient imsClient = new HeCloudImageClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )

    // 当前地域缓存的image数据
    Collection<String> evictableNamedImage = providerCache.getAll(IMAGES.ns, providerCache.filterIdentifiers(IMAGES.ns, Keys.getImageKey('*', accountName, region))).stream()
      .flatMap({ cacheData -> cacheData.getRelationships().get(NAMED_IMAGES.ns).stream() })
      .collect()

    def result = imsClient.getImages()

    result.each {
      def hecloudImage = new HeCloudImage(
        region: this.region,
        name: it.getName(),
        imageId: it.getId(),
        type: it.getImagetype().toString(),
        osPlatform: it.getOsType().toString(),
        createdTime: it.getCreatedAt()
      )

      def images = namespaceCache[IMAGES.ns]
      def namedImages = namespaceCache[NAMED_IMAGES.ns]
      def imageKey = Keys.getImageKey hecloudImage.id, this.accountName, this.region
      def namedImageKey = Keys.getNamedImageKey hecloudImage.name, this.accountName
      if (namedImages.containsKey(namedImageKey)){
        def attr = namedImages.get(namedImageKey).getAttributes()
        if (attr.createdTime < hecloudImage.createdTime) {
          namedImages.remove(namedImageKey)
          images.remove(attr.imageId)
        } else {
          return
        }
      }
      images[imageKey].attributes.image = hecloudImage
      images[imageKey].relationships[NAMED_IMAGES.ns].add namedImageKey
      evictableNamedImage.removeIf({e -> e.equals(namedImageKey)})

      def originImageCache = providerCache.get(IMAGES.ns, imageKey)
      if (originImageCache) {
        def imageNames = originImageCache.relationships[NAMED_IMAGES.ns]
        if (imageNames && imageNames[0] != namedImageKey) {
          evictions[NAMED_IMAGES.ns].addAll(originImageCache.relationships[NAMED_IMAGES.ns])
        }
      }

      namedImages[namedImageKey].attributes.imageName = hecloudImage.name
      namedImages[namedImageKey].attributes.type = hecloudImage.type
      namedImages[namedImageKey].attributes.osPlatform = hecloudImage.osPlatform
      namedImages[namedImageKey].attributes.createdTime = hecloudImage.createdTime
      namedImages[namedImageKey].attributes.imageId = hecloudImage.imageId
      namedImages[namedImageKey].relationships[IMAGES.ns].add imageKey
      null
    }

    namespaceCache.each { String namespace, Map<String, CacheData> cacheDataMap ->
      cacheResults[namespace] = cacheDataMap.values()
    }

    CacheResult defaultCacheResult = new DefaultCacheResult(cacheResults, evictions)
    log.info 'finish loads image data.'
    log.info "Caching ${namespaceCache[IMAGES.ns].size()} items in $agentType"
    defaultCacheResult.evictions[NAMED_IMAGES.ns] = evictableNamedImage
    defaultCacheResult
  }
  @Override
  Optional<Map<String, String>> getCacheKeyPatterns() {
    return [
      (IMAGES.ns): Keys.getImageKey('*', accountName, region)
    ]
  }
}

