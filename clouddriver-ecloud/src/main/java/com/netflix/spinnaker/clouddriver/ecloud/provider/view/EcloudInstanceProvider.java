package com.netflix.spinnaker.clouddriver.ecloud.provider.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudInstance;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudTag;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudZone;
import com.netflix.spinnaker.clouddriver.ecloud.provider.agent.EcloudZoneHelper;
import com.netflix.spinnaker.clouddriver.model.HealthState;
import com.netflix.spinnaker.clouddriver.model.InstanceProvider;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author xu.dangling
 * @date 2024/4/11 @Description
 */
@Slf4j
@Component
public class EcloudInstanceProvider implements InstanceProvider<EcloudInstance, String> {

  private final ObjectMapper objectMapper;
  private final Cache cacheView;
  private final EcloudProvider provider;

  @Autowired
  public EcloudInstanceProvider(
      ObjectMapper objectMapper, Cache cacheView, EcloudProvider provider) {
    this.objectMapper = objectMapper;
    this.cacheView = cacheView;
    this.provider = provider;
  }

  @Override
  public EcloudInstance getInstance(String account, String region, String id) {
    CacheData instanceEntry =
        cacheView.get(Keys.Namespace.INSTANCES.ns, Keys.getInstanceKey(id, account, region));
    if (instanceEntry == null) {
      return null;
    }
    return this.instanceFromCacheData(instanceEntry, account, region);
  }

  @Override
  public String getConsoleOutput(String account, String region, String id) {
    return null;
  }

  @Override
  public String getCloudProvider() {
    return EcloudProvider.ID;
  }

  public EcloudInstance instanceFromCacheData(CacheData cacheData, String account, String region) {
    EcloudInstance instance = new EcloudInstance();
    instance.setCloudProvider(EcloudProvider.ID);
    Map<String, Object> attributes = cacheData.getAttributes();
    String id = (String) attributes.get("id");
    instance.setName(id);
    // TargetHealth
    HealthState healthState = null;
    if (attributes.get("serverGroupName") != null) {
      String asgName = (String) attributes.get("serverGroupName");
      CacheData serverGroupEntry =
          cacheView.get(
              Keys.Namespace.SERVER_GROUPS.ns, Keys.getServerGroupKey(asgName, account, region));
      if (serverGroupEntry != null) {
        List<Map> lbInfos = (List<Map>) serverGroupEntry.getAttributes().get("loadBalancers");
        if (lbInfos != null && !lbInfos.isEmpty()) {
          Map<String, String> lbMemberMap = new HashMap<>();
          for (Map lbInfo : lbInfos) {
            String lbId = (String) lbInfo.get("loadBalancerId");
            String poolId = (String) lbInfo.get("loadBalancerPoolId");
            String healthCheckKey = Keys.getTargetHealthKey(lbId, poolId, id, account, region);
            CacheData health = cacheView.get(Keys.Namespace.HEALTH_CHECKS.ns, healthCheckKey);
            if (health != null) {
              Map targetHealth = (Map) health.getAttributes().get("targetHealth");
              if (targetHealth != null) {
                lbMemberMap.put(poolId, (String) targetHealth.get("memberId"));
                if (!HealthState.Down.equals(healthState)) {
                  healthState = HealthState.fromString((String) targetHealth.get("healthStatus"));
                }
              }
            }
          }
          if (healthState == null) {
            // the instance may be removed from lb, regarded as down
            healthState = HealthState.Down;
          }
          instance.setLbMemberMap(lbMemberMap);
        }
        instance.setVpcId((String) serverGroupEntry.getAttributes().get("realVpcId"));
        Map sc = (Map) serverGroupEntry.getAttributes().get("scalingConfig");
        if (sc != null) {
          List<String> securityGroupIds = new ArrayList<>();
          if (sc.get("securityGroupInfoRespList") != null) {
            List<Map> secGrps = (List<Map>) sc.get("securityGroupInfoRespList");
            securityGroupIds =
                secGrps.stream()
                    .map(one -> (String) one.get("securityGroupId"))
                    .collect(Collectors.toList());
            instance.setSecurityGroupIds(securityGroupIds);
          }
        }
      }
    }
    if (healthState == null) {
      // lb not found
      healthState = HealthState.Down;
      int status = (int) attributes.get("status");
      if (status == 1) {
        healthState = HealthState.Up;
      } else if (status == 18) {
        healthState = HealthState.Unknown;
      }
    }
    instance.setHealthState(healthState);
    List<Map<String, Object>> health = new ArrayList<>();
    Map<String, Object> m = new HashMap<>();
    m.put("type", provider.getDisplayName());
    m.put("healthClass", "platform");
    m.put("state", healthState);
    health.add(m);
    instance.setHealth(health);
    String zone = (String) attributes.get("region");
    EcloudZone obj = EcloudZoneHelper.getEcloudZone(account, region, zone);
    if (obj != null) {
      // use name
      zone = obj.getName();
    }
    instance.setZone(zone);
    Long createdTime = null;
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Date created = sdf.parse((String) attributes.get("createdTime"));
      createdTime = created.getTime();
    } catch (ParseException e) {
      log.error(e.getMessage(), e);
    }
    instance.setLaunchTime(createdTime);
    instance.setInstanceType((String) attributes.get("specsName"));
    instance.setImageId((String) attributes.get("imageRef"));
    List<Map> tagList = (List<Map>) attributes.get("tags");
    if (attributes.get("tags") != null) {
      List<EcloudTag> tags = new ArrayList<>();
      for (Map one : tagList) {
        EcloudTag tag = new EcloudTag();
        tag.setKey((String) one.get("tagKey"));
        tag.setValue((String) one.get("tagValue"));
        tags.add(tag);
      }
      instance.setTags(tags);
    }
    List<String> publicIps = new ArrayList<>();
    List<String> privateIps = new ArrayList<>();
    List<Map> portDetails = (List<Map>) attributes.get("portDetail");
    if (portDetails != null) {
      for (Map portDetail : portDetails) {
        List<Map> ipDetails = (List<Map>) portDetail.get("fixedIpDetailResps");
        if (ipDetails != null) {
          for (Map ipDetail : ipDetails) {
            if (ipDetail.get("publicIp") != null) {
              publicIps.add((String) ipDetail.get("publicIp"));
            }
            if (ipDetail.get("ipAddress") != null) {
              privateIps.add((String) ipDetail.get("ipAddress"));
            }
          }
        }
      }
    }
    instance.setPublicIpAddresses(publicIps);
    instance.setPrivateIpAddresses(privateIps);
    instance.setAsgNodeId((String) attributes.get("asgNodeId"));
    instance.setServerGroupName((String) attributes.get("serverGroupName"));
    return instance;
  }
}
