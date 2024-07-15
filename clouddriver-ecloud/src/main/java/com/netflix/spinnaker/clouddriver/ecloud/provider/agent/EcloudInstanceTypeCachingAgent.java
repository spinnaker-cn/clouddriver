package com.netflix.spinnaker.clouddriver.ecloud.provider.agent;

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.agent.AgentDataType;
import com.netflix.spinnaker.cats.agent.CacheResult;
import com.netflix.spinnaker.cats.agent.DefaultCacheResult;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.DefaultCacheData;
import com.netflix.spinnaker.cats.provider.ProviderCache;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.client.openapi.EcloudOpenApiHelper;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudInstanceType;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudRequest;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudResponse;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudZone;
import com.netflix.spinnaker.clouddriver.ecloud.security.EcloudCredentials;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * @author xu.dangling
 * @date 2024/4/8 @Description
 */
@Slf4j
public class EcloudInstanceTypeCachingAgent extends AbstractEcloudCachingAgent {

  static final Collection<AgentDataType> types =
      Collections.unmodifiableCollection(
          new ArrayList<AgentDataType>() {
            {
              add(AUTHORITATIVE.forType(Keys.Namespace.INSTANCE_TYPES.ns));
            }
          });

  public EcloudInstanceTypeCachingAgent(
      EcloudCredentials account, String region, ObjectMapper objectMapper) {
    super(account, region, objectMapper);
  }

  @Override
  public Collection<AgentDataType> getProvidedDataTypes() {
    return types;
  }

  @Override
  public CacheResult loadData(ProviderCache providerCache) {
    Map<String, Collection<CacheData>> resultMap = new HashMap<>(16);
    List<CacheData> instanceTypeData = new ArrayList<>();
    // Get Available Zones
    Collection<EcloudZone> zoneList = EcloudZoneHelper.getEcloudZones(account.getName(), region);
    // Get All ProductIds
    EcloudRequest prodRequest =
        new EcloudRequest(
            "GET",
            region,
            "/api/openapi-ecs/acl/v3/server/products",
            account.getAccessKey(),
            account.getSecretKey());
    prodRequest.setVersion("2016-12-05");
    EcloudResponse prodRsp = EcloudOpenApiHelper.execute(prodRequest);
    if (!CollectionUtils.isEmpty(zoneList) && prodRsp.getBody() != null) {
      List<Map> productBodyList = (List<Map>) prodRsp.getBody();
      for (EcloudZone zone : zoneList) {
        for (Map productBody : productBodyList) {
          // query by zone+product
          String offerId = (String) productBody.get("offerId");
          Map<String, String> queryParams = new HashMap<>();
          queryParams.put("offerId", offerId);
          queryParams.put("region", zone.getRegion());
          EcloudRequest request =
              new EcloudRequest(
                  "GET",
                  region,
                  "/api/openapi-ecs/acl/v3/server/specsName",
                  account.getAccessKey(),
                  account.getSecretKey());
          request.setQueryParams(queryParams);
          EcloudResponse flavorRsp = EcloudOpenApiHelper.execute(request);
          if (flavorRsp.getBody() != null) {
            List<Map> body = (List<Map>) flavorRsp.getBody();
            for (Map map : body) {
              String specsName = (String) map.get("specsName");
              if (specsName == null || specsName.split("\\.").length != 3) {
                log.warn("Ignore InstanceType of wrong format:" + specsName);
                continue;
              }
              String soldOut = (String) map.get("soldOut");
              if (!"1".equals(soldOut)) {
                EcloudInstanceType type = new EcloudInstanceType();
                type.setRegion(region);
                type.setAccount(account.getName());
                type.setName(specsName);
                type.setZone(zone.getRegion());
                String[] specs = specsName.split("\\.");
                int cpu = parseCpu(specs[1]);
                type.setCpu(cpu);
                type.setMem(cpu * Integer.parseInt(specs[2]));
                Map attributes = objectMapper.convertValue(type, Map.class);
                CacheData data =
                    new DefaultCacheData(
                        Keys.getInstanceTypeKey(
                            account.getName(), region, type.getZone(), type.getName()),
                        attributes,
                        new HashMap<>(16));
                instanceTypeData.add(data);
              }
            }
          }
        }
      }
    }
    resultMap.put(Keys.Namespace.INSTANCE_TYPES.ns, instanceTypeData);
    return new DefaultCacheResult(resultMap);
  }

  private int parseCpu(String large) {
    if ("medium".equalsIgnoreCase(large)) {
      return 1;
    } else if ("large".equalsIgnoreCase(large)) {
      return 2;
    } else if ("xlarge".equalsIgnoreCase(large)) {
      return 4;
    } else if (large.endsWith("xlarge")) {
      // nxLarge
      int nx = Integer.parseInt(large.substring(0, large.indexOf("xlarge")));
      return nx * 4;
    }
    return -1;
  }
}
