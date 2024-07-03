package com.netflix.spinnaker.clouddriver.ecloud.provider.agent;

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.agent.AgentDataType;
import com.netflix.spinnaker.cats.agent.CacheResult;
import com.netflix.spinnaker.cats.agent.DefaultCacheResult;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.DefaultCacheData;
import com.netflix.spinnaker.cats.provider.ProviderCache;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.client.openapi.EcloudOpenApiHelper;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudRequest;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudResponse;
import com.netflix.spinnaker.clouddriver.ecloud.security.EcloudCredentials;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.CollectionUtils;

/**
 * @author xu.dangling
 * @date 2024/5/16 @Description
 */
public class EcloudKeyPairCachingAgent extends AbstractEcloudCachingAgent {

  static final Collection<AgentDataType> types =
      Collections.unmodifiableCollection(
          new ArrayList<AgentDataType>() {
            {
              add(AUTHORITATIVE.forType(Keys.Namespace.KEY_PAIRS.ns));
            }
          });

  public EcloudKeyPairCachingAgent(
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
    List<CacheData> keyPairData = new ArrayList<>();
    EcloudRequest request =
        new EcloudRequest(
            "GET",
            region,
            "/api/openapi-ecs/customer/v3/keypair",
            account.getAccessKey(),
            account.getSecretKey());
    request.setVersion("2016-12-05");
    Map<String, String> pageQuery = new HashMap<>();
    request.setQueryParams(pageQuery);
    pageQuery.put("size", "50");
    int page = 1;
    int count = 0;
    while (true) {
      pageQuery.put("page", "" + page);
      EcloudResponse rsp = EcloudOpenApiHelper.execute(request);
      if (rsp.getBody() != null) {
        Map body = (Map) rsp.getBody();
        if (body != null) {
          int total = (int) body.get("total");
          List<Map> list = (List<Map>) body.get("content");
          if (!CollectionUtils.isEmpty(list)) {
            count += list.size();
            for (Map attributes : list) {
              String id = (String) attributes.get("id");
              attributes.put("provider", EcloudProvider.ID);
              attributes.put("account", account.getName());
              attributes.put("poolId", region);
              CacheData data =
                  new DefaultCacheData(
                      Keys.getKeyPairKey(id, account.getName(), region),
                      attributes,
                      new HashMap<>(16));
              keyPairData.add(data);
            }
            if (count < total) {
              page++;
              continue;
            }
          }
        }
        break;
      }
      break;
    }

    resultMap.put(Keys.Namespace.KEY_PAIRS.ns, keyPairData);

    return new DefaultCacheResult(resultMap);
  }
}
