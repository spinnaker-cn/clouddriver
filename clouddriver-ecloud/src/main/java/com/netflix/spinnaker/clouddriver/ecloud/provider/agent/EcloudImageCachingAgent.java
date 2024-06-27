package com.netflix.spinnaker.clouddriver.ecloud.provider.agent;

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE;
import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.IMAGES;

import com.ecloud.sdk.ims.v1.model.ListImageRespV2ResponseContent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.agent.AccountAware;
import com.netflix.spinnaker.cats.agent.AgentDataType;
import com.netflix.spinnaker.cats.agent.CacheResult;
import com.netflix.spinnaker.cats.agent.CachingAgent;
import com.netflix.spinnaker.cats.agent.DefaultCacheResult;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.provider.ProviderCache;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.client.EcloudImageClient;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudImage;
import com.netflix.spinnaker.clouddriver.ecloud.provider.EcloudSearchableProvider;
import com.netflix.spinnaker.clouddriver.ecloud.provider.view.MutableCacheData;
import com.netflix.spinnaker.clouddriver.ecloud.security.EcloudCredentials;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EcloudImageCachingAgent implements CachingAgent, AccountAware {
  protected EcloudCredentials account;
  protected String region;
  protected ObjectMapper objectMapper;
  static final Collection<AgentDataType> types =
      Collections.unmodifiableCollection(
          new ArrayList<AgentDataType>() {
            {
              add(AUTHORITATIVE.forType(IMAGES.ns));
              /*  add(AUTHORITATIVE.forType(NAMED_IMAGES.ns));*/
            }
          });

  public EcloudImageCachingAgent(
      EcloudCredentials account, String region, ObjectMapper objectMapper) {
    this.account = account;
    this.region = region;
    this.objectMapper = objectMapper;
  }

  @Override
  public Collection<AgentDataType> getProvidedDataTypes() {
    return types;
  }

  @Override
  public CacheResult loadData(ProviderCache providerCache) {
    log.info("start load image data");
    EcloudImageClient client = new EcloudImageClient(account, region);
    Map<String, Collection<CacheData>> cacheResults = new HashMap<>();
    Map<String, Map<String, CacheData>> namespaceCache = new HashMap<>();
    Map<String, Collection<String>> evictions = new HashMap<>();

    List<ListImageRespV2ResponseContent> result = client.getImages();

    for (ListImageRespV2ResponseContent item : result) {
      EcloudImage ecloudImage =
          new EcloudImage(
              region,
              item.getName(),
              item.getImageId(),
              item.getImageType().toString(),
              item.getOsName(),
              item.getCreateTime(),
              item.getSnapshotId(),
              item.getIsPublic());

      Map<String, CacheData> images =
          namespaceCache.computeIfAbsent(Keys.Namespace.IMAGES.ns, k -> new HashMap<>());
      Map<String, CacheData> namedImages =
          namespaceCache.computeIfAbsent(Keys.Namespace.NAMED_IMAGES.ns, k -> new HashMap<>());
      String imageKey = Keys.getImageKey(ecloudImage.getId(), account.getName(), region);
      String namedImageKey = Keys.getNamedImageKey(ecloudImage.getName(), account.getName());

      MutableCacheData imageCacheData = new MutableCacheData(imageKey);
      imageCacheData.getAttributes().put("image", ecloudImage);
      imageCacheData.getAttributes().put("snapshotId", ecloudImage.getSnapshotId());
      imageCacheData
          .getRelationships()
          .computeIfAbsent(Keys.Namespace.NAMED_IMAGES.ns, k -> new ArrayList<>())
          .add(namedImageKey);
      images.put(imageKey, imageCacheData);

      CacheData originImageCache = providerCache.get(Keys.Namespace.IMAGES.ns, imageKey);
      if (originImageCache != null) {
        List<String> imageNames =
            new ArrayList<>(
                originImageCache.getRelationships().get(Keys.Namespace.NAMED_IMAGES.ns));
        if (imageNames != null
            && !imageNames.isEmpty()
            && !imageNames.get(0).equals(namedImageKey)) {
          evictions
              .computeIfAbsent(Keys.Namespace.NAMED_IMAGES.ns, k -> new ArrayList<>())
              .addAll(imageNames);
        }
      }
      MutableCacheData namedImageCacheData = new MutableCacheData(namedImageKey);
      namedImageCacheData.getAttributes().put("imageName", ecloudImage.getName());
      namedImageCacheData.getAttributes().put("type", ecloudImage.getType());
      namedImageCacheData.getAttributes().put("osPlatform", ecloudImage.getOsPlatform());
      namedImageCacheData.getAttributes().put("snapshotId", ecloudImage.getSnapshotId());
      namedImageCacheData.getAttributes().put("createdTime", ecloudImage.getCreatedTime());
      namedImageCacheData.getAttributes().put("imageId", ecloudImage.getImageId());
      namedImageCacheData.getAttributes().put("isPublic", ecloudImage.getIsPublic());
      namedImageCacheData
          .getRelationships()
          .computeIfAbsent(Keys.Namespace.IMAGES.ns, k -> new ArrayList<>())
          .add(imageKey);
      namedImages.put(namedImageKey, namedImageCacheData);
    }

    namespaceCache.forEach(
        (namespace, cacheDataMap) -> cacheResults.put(namespace, cacheDataMap.values()));

    CacheResult defaultCacheResult = new DefaultCacheResult(cacheResults, evictions);
    log.info("finish loads image data.");
    log.info(
        "Caching "
            + namespaceCache.get(Keys.Namespace.IMAGES.ns).size()
            + " items in "
            + this.getAgentType());
    return defaultCacheResult;
  }

  @Override
  public String getAgentType() {
    return account.getName() + "/" + region + "/" + this.getClass().getSimpleName();
  }

  @Override
  public String getProviderName() {
    return EcloudSearchableProvider.class.getName();
  }

  @Override
  public String getAccountName() {
    return account.getName();
  }
}
