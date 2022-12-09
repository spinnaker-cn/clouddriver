package com.netflix.spinnaker.clouddriver.hecloud.provider.agent;

import com.netflix.spinnaker.cats.agent.AgentDataType;
import com.netflix.spinnaker.cats.agent.CacheResult;
import com.netflix.spinnaker.cats.agent.CachingAgent;
import com.netflix.spinnaker.cats.agent.DefaultCacheResult;
import com.netflix.spinnaker.cats.provider.ProviderCache;
import com.netflix.spinnaker.clouddriver.hecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.hecloud.provider.HeCloudInfrastructureProvider;
import com.netflix.spinnaker.clouddriver.hecloud.security.HeCloudNamedAccountCredentials;
import com.netflix.spinnaker.kork.jedis.RedisClientDelegate;
import groovy.util.logging.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

import static com.netflix.spinnaker.clouddriver.hecloud.cache.Keys.Namespace.*;

@Slf4j
public class HeCloudRedisCheckCachingAgent implements CachingAgent {
  final HeCloudNamedAccountCredentials credentials;
  final String providerName = HeCloudInfrastructureProvider.class.getName();
  final RedisClientDelegate redisClientDelegate;

  public HeCloudRedisCheckCachingAgent(HeCloudNamedAccountCredentials credentials, RedisClientDelegate redisClientDelegate) {
    this.credentials = credentials;
    this.redisClientDelegate = redisClientDelegate;
  }


  @Override
  public String getAgentType() {
    return String.format("%s/%s/%s", credentials.getAccountId(), "*", this.getClass().getSimpleName());
  }

  @Override
  public String getProviderName() {
    return providerName;
  }

  @Override
  public Collection<AgentDataType> getProvidedDataTypes() {
    return new ArrayList<>();
  }

  @Override
  public CacheResult loadData(ProviderCache providerCache) {
    HashMap<String, Collection<String>> evition = new HashMap<>();
    // cluster脏数据处理(cluster attribute存在，对应的serverGroup中的relationShip中不存在，就删掉)
    Collection<String> clusterCaches = providerCache.getAll(SERVER_GROUPS.ns).stream().flatMap(cacheData -> cacheData.getRelationships().get(CLUSTERS.ns).stream()).collect(Collectors.toList());
    evition.put(CLUSTERS.ns, providerCache.filterIdentifiers(CLUSTERS.ns, Keys.getClusterKey("*", "*", credentials.getName())).stream()
      .filter(cluster -> clusterCaches.stream().noneMatch(ship -> ship.equals(cluster))).collect(Collectors.toList()));

    // namedImage脏数据处理(namedImage attribute存在，对应的image中的relationShip中不存在，就删掉)
    Collection<String> namedImageCaches = providerCache.getAll(IMAGES.ns).stream().flatMap(cacheData -> cacheData.getRelationships().get(NAMED_IMAGES.ns).stream()).collect(Collectors.toList());
    evition.put(NAMED_IMAGES.ns, providerCache.filterIdentifiers(NAMED_IMAGES.ns, Keys.getNamedImageKey("*", credentials.getName())).stream()
      .filter(namedImage -> namedImageCaches.stream().noneMatch(ship -> ship.equals(namedImage))).collect(Collectors.toList()));


    // HEALTH_CHECK脏数据处理
    redisClientDelegate.withCommandsClient(c -> {
      String key = hashKey(providerName, HEALTH_CHECKS.ns);
      String prefix = prefix(HEALTH_CHECKS.ns);
      if (c.pfcount(prefix) != c.hkeys(key).stream().filter(s -> s.startsWith(prefix)).count()) {
        c.del(key);
      }
    });
    return new DefaultCacheResult(new HashMap<>(), evition);
  }

  private String hashKey(String providerName, String type) {
    return String.format("%s:%s:hashes", providerName, type);
  }

  private String prefix(String type) {
    return String.format("%s:%s:attributes:%s:%s:%s:", providerName, type, credentials.getCloudProvider(), type, credentials.getName());
  }
}
