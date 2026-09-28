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
 * @Description: Loads and caches the ECS availability zones for an Ecloud resource pool.
 * @Author: han.pengfei-ai
 * @Date: 2024/04/08
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
            "/api/openapi-ecs/acl/v3/server/region",
            account.getAccessKey(),
            account.getSecretKey());
    if (log.isInfoEnabled()) {
      log.info(
          "[ECLOUD-SPEC-DIAG] zones request start, account={}, poolId={}",
          account.getName(),
          poolId);
    }
    EcloudResponse rsp = EcloudOpenApiHelper.execute(request);
    if (log.isInfoEnabled()) {
      log.info(
          "[ECLOUD-SPEC-DIAG] zones response, account={}, poolId={}, httpCode={}, errorCode={}, requestId={}, bodyNull={}",
          account.getName(),
          poolId,
          rsp.getHttpCode(),
          rsp.getErrorCode(),
          rsp.getRequestId(),
          rsp.getBody() == null);
    }
    // ECS V3 的可用区列表位于 body.regionList，复用原有可用区缓存对象。
    Map responseBody = rsp.getBody() instanceof Map ? (Map) rsp.getBody() : null;
    List<Map> body = null;
    if (responseBody != null && responseBody.get("regionList") instanceof List) {
      body = (List<Map>) responseBody.get("regionList");
    }
    Map<String, EcloudZone> availableZones = new HashMap<>();
    if (body != null && !body.isEmpty()) {
      for (Map map : body) {
        EcloudZone zone = new EcloudZone();
        // ECS V3 仅返回 region、name；未使用的元数据字段保留为 null。
        zone.setId(null);
        zone.setRegion((String) map.get("region"));
        zone.setName((String) map.get("name"));
        zone.setComponent(null);
        zone.setPoolId(null);
        // ECS V3 不返回 deleted、visible；本地按接口返回列表采集，
        // 对象中固定为未删除、可见。这是适配默认值，并非接口返回状态。
        zone.setDeleted(false);
        zone.setVisible(true);
        zone.setType(null);
        zone.setStatus(null);
        availableZones.put(zone.getRegion(), zone);
      }
    }
    map.put(key, availableZones);
    if (log.isInfoEnabled()) {
      log.info(
          "[ECLOUD-SPEC-DIAG] zones cached, account={}, poolId={}, count={}, zones={}",
          account.getName(),
          poolId,
          availableZones.size(),
          availableZones.keySet());
    }
  }
}
