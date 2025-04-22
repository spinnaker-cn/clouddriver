package com.netflix.spinnaker.clouddriver.ecloud.provider.agent;

import com.netflix.spinnaker.clouddriver.ecloud.client.openapi.EcloudOpenApiHelper;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudRequest;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudResponse;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudZone;
import com.netflix.spinnaker.clouddriver.ecloud.security.EcloudCredentials;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xu.dangling
 * @date 2024/4/8 @Description
 */
@Slf4j
public class EcloudZoneHelper {

  private static ConcurrentHashMap<String, Map<String, EcloudZone>> map = new ConcurrentHashMap<>();

  public static Collection<EcloudZone> getEcloudZones(String account, String poolId) {
    String key = account + "-" + poolId;
    if (map.get(key) != null) {
      return map.get(key).values();
    }
    return null;
  }

  public static EcloudZone getEcloudZone(String account, String poolId, String zone) {
    String key = account + "-" + poolId;
    if (map.get(key) != null) {
      return map.get(key).get(zone);
    }
    return null;
  }

  public static void loadZones(EcloudCredentials account, String poolId) {
    String key = account.getName() + "-" + poolId;
    if (map.get(key) != null) {
      return;
    }
    EcloudRequest request =
        new EcloudRequest(
            "GET",
            poolId,
            "/api/metadata/v3/region",
            account.getAccessKey(),
            account.getSecretKey());
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("component", "NOVA");
    request.setQueryParams(queryParams);
    EcloudResponse rsp = EcloudOpenApiHelper.execute(request);
    List<Map> body = (List<Map>) rsp.getBody();
    Map<String, EcloudZone> availableZones = new HashMap<>();
    if (body != null && !body.isEmpty()) {
      for (Map map : body) {
        EcloudZone zone = new EcloudZone();
        zone.setId(Long.parseLong("" + map.get("id")));
        zone.setRegion((String) map.get("region"));
        zone.setName((String) map.get("name"));
        zone.setComponent((String) map.get("component"));
        zone.setPoolId((String) map.get("poolId"));
        zone.setDeleted((Boolean) map.get("deleted"));
        zone.setVisible((Boolean) map.get("visible"));
        zone.setType((String) map.get("type"));
        zone.setStatus((String) map.get("status"));
        if (!zone.getDeleted() && zone.getVisible()) {
          availableZones.put(zone.getRegion(), zone);
        }
      }
    }
    map.put(key, availableZones);
  }
}
