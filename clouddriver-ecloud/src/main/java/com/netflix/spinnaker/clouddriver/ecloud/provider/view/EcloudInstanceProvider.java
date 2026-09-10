package com.netflix.spinnaker.clouddriver.ecloud.provider.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudInstance;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudTag;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudZone;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancer;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerMember;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerPool;
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
 * @Description: 从移动云缓存组装实例及负载均衡健康视图，并记录转换诊断步骤。
 * @Author: han.pengfei-ai
 * @Date: 2024/04/11
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
    long diagnosticStarted = System.nanoTime();
    String diagnosticContext =
        new StringBuilder()
            .append("cacheId=")
            .append(cacheData == null ? null : cacheData.getId())
            .append(" account=")
            .append(account)
            .append(" region=")
            .append(region)
            .toString();
    String diagnosticStep = "PREPARE";
    diagnosticInfo("TRANSLATE_INSTANCE", "BEGIN", diagnosticContext);
    try {
      diagnosticStep = diagnosticNextStep(diagnosticStep, "INSTANCE_BASIC_ID", diagnosticContext);
      EcloudInstance instance = new EcloudInstance();
      instance.setCloudProvider(EcloudProvider.ID);
      Map<String, Object> attributes = cacheData.getAttributes();
      String id = (String) attributes.get("id");
      instance.setName(id);
      diagnosticContext =
          new StringBuilder()
              .append(diagnosticContext)
              .append(" instanceId=")
              .append(id)
              .append(" serverGroup=")
              .append(attributes.get("serverGroupName"))
              .toString();
      // TargetHealth
      HealthState healthState = null;
      if (attributes.get("serverGroupName") != null) {
        String asgName = (String) attributes.get("serverGroupName");
        diagnosticStep = diagnosticNextStep(diagnosticStep, "INSTANCE_SG_CACHE", diagnosticContext);
        CacheData serverGroupEntry =
            cacheView.get(
                Keys.Namespace.SERVER_GROUPS.ns, Keys.getServerGroupKey(asgName, account, region));
        diagnosticInfo(diagnosticStep, "DATA", new StringBuilder()
                .append(diagnosticContext)
                .append(" hit=")
                .append(serverGroupEntry != null)
                .toString());
        diagnosticStep = diagnosticNextStep(diagnosticStep, "INSTANCE_SG_CONVERT", diagnosticContext);
        if (serverGroupEntry != null) {
          List<Map> lbInfos = (List<Map>) serverGroupEntry.getAttributes().get("loadBalancers");
          if (lbInfos != null && !lbInfos.isEmpty()) {
            Map<String, String> lbMemberMap = new HashMap<>();
            for (Map lbInfo : lbInfos) {
              String lbId = (String) lbInfo.get("loadBalancerId");
              diagnosticStep = diagnosticNextStep(diagnosticStep, "LB_CACHE", diagnosticContext);
              diagnosticInfo(diagnosticStep, "DATA", new StringBuilder()
                      .append(diagnosticContext)
                      .append(" lbId=")
                      .append(lbId)
                      .toString());
              CacheData lbCache =
                  cacheView.get(
                      Keys.Namespace.LOAD_BALANCERS.ns,
                      Keys.getLoadBalancerKey(lbId, account, region));
              diagnosticInfo(diagnosticStep, "DATA", new StringBuilder()
                      .append(diagnosticContext)
                      .append(" lbId=")
                      .append(lbId)
                      .append(" hit=")
                      .append(lbCache != null)
                      .toString());
              if (lbCache == null) {
                log.error("LoadBalance Not Found, serverGroupName=" + asgName + ", lbId=" + lbId);
                continue;
              }
              diagnosticStep = diagnosticNextStep(diagnosticStep, "LB_CONVERT", diagnosticContext);
              EcloudLoadBalancer loadBalancer =
                  objectMapper.convertValue(lbCache.getAttributes(), EcloudLoadBalancer.class);
              diagnosticInfo(diagnosticStep, "DATA", new StringBuilder()
                      .append(diagnosticContext)
                      .append(" lbId=")
                      .append(lbId)
                      .append(" poolCount=")
                      .append(loadBalancer == null || loadBalancer.getPools() == null ? 0 : loadBalancer.getPools().size())
                      .toString());
              diagnosticStep = diagnosticNextStep(diagnosticStep, "LB_POOLS", diagnosticContext);
              if (loadBalancer != null && loadBalancer.getPools() != null) {
                for (EcloudLoadBalancerPool pool : loadBalancer.getPools()) {
                  diagnosticStep = diagnosticNextStep(diagnosticStep, "LB_MEMBER_MATCH", diagnosticContext);
                  diagnosticInfo(diagnosticStep, "DATA", new StringBuilder()
                          .append(diagnosticContext)
                          .append(" lbId=")
                          .append(lbId)
                          .append(" poolId=")
                          .append(pool.getPoolId())
                          .append(" memberCount=")
                          .append(pool.getMembers() == null ? 0 : pool.getMembers().size())
                          .toString());
                  if (pool.getMembers() != null) {
                    for (EcloudLoadBalancerMember member : pool.getMembers()) {
                      if (member.getVmHostId() == null) {
                        diagnosticInfo(diagnosticStep, "INVALID_DATA", new StringBuilder()
                                .append(diagnosticContext)
                                .append(" lbId=")
                                .append(lbId)
                                .append(" poolId=")
                                .append(pool.getPoolId())
                                .append(" memberId=")
                                .append(member.getId())
                                .append(" vmHostId=null")
                                .toString());
                      }
                      if (member.getVmHostId().equals(id)) {
                        lbMemberMap.put(pool.getPoolId(), member.getId());
                        break;
                      }
                    }
                  }
                }
              }
            }
            diagnosticInfo(diagnosticStep, "DATA", new StringBuilder()
                    .append(diagnosticContext)
                    .append(" matchedPoolCount=")
                    .append(lbMemberMap.size())
                    .toString());
            instance.setLbMemberMap(lbMemberMap);
            boolean allup = true;
            for (Map lbInfo : lbInfos) {
              String lbId = (String) lbInfo.get("loadBalancerId");
              String poolId = (String) lbInfo.get("loadBalancerPoolId");
              diagnosticStep = diagnosticNextStep(diagnosticStep, "TARGET_HEALTH_CACHE", diagnosticContext);
              diagnosticInfo(diagnosticStep, "DATA", new StringBuilder()
                      .append(diagnosticContext)
                      .append(" lbId=")
                      .append(lbId)
                      .append(" poolId=")
                      .append(poolId)
                      .toString());
              CacheData health =
                  cacheView.get(
                      Keys.Namespace.HEALTH_CHECKS.ns,
                      Keys.getTargetHealthKey(lbId, poolId, id, account, region));
              diagnosticInfo(diagnosticStep, "DATA", new StringBuilder()
                      .append(diagnosticContext)
                      .append(" hit=")
                      .append(health != null)
                      .toString());
              diagnosticStep = diagnosticNextStep(diagnosticStep, "TARGET_HEALTH_CONVERT", diagnosticContext);
              if (health != null) {
                Map targetHealth = (Map) health.getAttributes().get("targetHealth");
                if (targetHealth != null) {
                  String healthStatus = (String) targetHealth.get("healthStatus");
                  diagnosticInfo(diagnosticStep, "DATA", new StringBuilder()
                          .append(diagnosticContext)
                          .append(" lbId=")
                          .append(lbId)
                          .append(" poolId=")
                          .append(poolId)
                          .append(" healthStatus=")
                          .append(healthStatus)
                          .toString());
                  if ("DOWN".equalsIgnoreCase(healthStatus)) {
                    healthState = HealthState.Down;
                    allup = false;
                    break;
                  } else if (!"UP".equalsIgnoreCase(healthStatus)) {
                    allup = false;
                    healthState = HealthState.fromString((String) targetHealth.get("healthStatus"));
                  } else if (allup) {
                    // healthState UP only when all lb are up
                    healthState = HealthState.Up;
                  }
                } else {
                  allup = false;
                  healthState = HealthState.Down;
                }
              } else {
                // the instance may be removed from lb, regarded as down
                allup = false;
                healthState = HealthState.Down;
              }
            }
            if (healthState == null) {
              healthState = HealthState.Unknown;
            }
          }
          diagnosticStep = diagnosticNextStep(diagnosticStep, "INSTANCE_NETWORK_CONFIG", diagnosticContext);
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
      diagnosticStep = diagnosticNextStep(diagnosticStep, "INSTANCE_BASIC", diagnosticContext);
      if (healthState == null) {
        // lb not found
        healthState = HealthState.Unknown;
        int status = (int) attributes.get("status");
        if (status == 2 || status == 3) {
          healthState = HealthState.Starting;
        } else if (status == 1 || status == 18) {
          healthState = HealthState.Unknown;
        } else if (status == 15) {
          healthState = HealthState.Down;
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
      diagnosticInfo(diagnosticStep, "END", diagnosticContext);
      diagnosticInfo("TRANSLATE_INSTANCE", "SUCCESS", diagnosticContext);
      return instance;
    } catch (RuntimeException e) {
      log.error("[ECLOUD_DIAG] step=TRANSLATE_INSTANCE event=ERROR phase={} {}",
          diagnosticStep, diagnosticContext, e);
      throw e;
    } finally {
      diagnosticInfo("TRANSLATE_INSTANCE", "EXIT", new StringBuilder()
              .append(diagnosticContext)
              .append(" elapsedMs=")
              .append((System.nanoTime() - diagnosticStarted) / 1_000_000)
              .toString());
    }
  }

  private static void diagnosticInfo(String step, String event, String context) {
    log.info("[ECLOUD_DIAG] step={} event={} {}",
        step, event, context);
  }

  private static String diagnosticNextStep(String previous, String next, String context) {
    if (!"PREPARE".equals(previous)) {
      diagnosticInfo(previous, "END", context);
    }
    diagnosticInfo(next, "BEGIN", context);
    return next;
  }
}
