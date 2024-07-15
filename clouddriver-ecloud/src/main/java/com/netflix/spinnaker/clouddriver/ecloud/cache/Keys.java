package com.netflix.spinnaker.clouddriver.ecloud.cache;

import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.APPLICATIONS;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.CLUSTERS;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.HEALTH_CHECKS;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.IMAGES;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.LOAD_BALANCERS;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.NAMED_IMAGES;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.SERVER_GROUPS;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.VPCS;

import com.google.common.base.CaseFormat;
import com.netflix.frigga.Names;
import com.netflix.spinnaker.clouddriver.cache.KeyParser;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("eCloudKeys")
public class Keys implements KeyParser {

  private static final String SEPARATOR = ":";

  public enum Namespace {
    APPLICATIONS,
    CLUSTERS,
    IMAGES,
    NAMED_IMAGES,
    INSTANCES,
    INSTANCE_TYPES,
    KEY_PAIRS,
    LOAD_BALANCERS,
    NETWORKS,
    SECURITY_GROUPS,
    SERVER_GROUPS,
    SUBNETS,
    VPCS,
    HEALTH_CHECKS,
    ON_DEMAND;

    public final String ns;

    Namespace() {
      ns = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());
    }

    public static Namespace fromString(String name) {
      try {
        for (Namespace namespace : Namespace.values()) {
          if (namespace.ns.equals(name)) {
            return namespace;
          }
        }
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("No matching namespace with name " + name + " exists");
      }
      return null;
    }

    @Override
    public String toString() {
      return ns;
    }
  }

  @Override
  public String getCloudProvider() {
    return EcloudProvider.ID;
  }

  @Override
  public Boolean canParseType(final String type) {
    try {
      Namespace.fromString(type);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public Boolean canParseField(String field) {
    return false;
  }

  @Override
  public Map<String, String> parseKey(String key) {
    return parse(key);
  }

  public static Map<String, String> parse(String key) {
    String[] parts = key.split(SEPARATOR);

    if (parts.length < 2 || !parts[0].equals(EcloudProvider.ID)) {
      return null;
    }

    Map<String, String> result = new HashMap<>();
    result.put("provider", parts[0]);
    result.put("type", parts[1]);
    Namespace namespace = Namespace.fromString(result.get("type"));

    if (namespace == null) {
      return null;
    }
    switch (namespace) {
      case APPLICATIONS:
        result.put("application", parts[2]);
        result.put("name", parts[2]);
        break;
      case CLUSTERS:
        Names names = Names.parseName(parts[4]);
        result.put("application", parts[3]);
        result.put("account", parts[2]);
        result.put("name", parts[4]);
        result.put("cluster", parts[4]);
        result.put("stack", names.getStack());
        result.put("detail", names.getDetail());
        break;
      case IMAGES:
        result.put("account", parts[2]);
        result.put("region", parts[3]);
        result.put("imageId", parts[4]);
        break;
      case NAMED_IMAGES:
        result.put("account", parts[2]);
        result.put("imageName", parts[3]);
        break;
      case LOAD_BALANCERS:
      case NETWORKS:
      case SUBNETS:
        result.put("account", parts[2]);
        result.put("region", parts[3]);
        result.put("id", parts[4]);
        break;
      case SECURITY_GROUPS:
        result.put("application", Names.parseName(parts[2]).getApp());
        result.put("name", parts[2]);
        result.put("account", parts[3]);
        result.put("region", parts[4]);
        result.put("id", parts[5]);
        break;
      case SERVER_GROUPS:
        result.put("account", parts[2]);
        result.put("region", parts[3]);
        result.put("cluster", parts[4]);
        result.put("name", parts[5]);
        break;
      case INSTANCES:
        result.put("account", parts[2]);
        result.put("region", parts[3]);
        result.put("id", parts[4]);
        break;
      case VPCS:
        result.put("account", parts[2]);
        result.put("region", parts[3]);
        result.put("id", parts[4]);
        break;
      case HEALTH_CHECKS:
        result.put("account", parts[2]);
        result.put("region", parts[3]);
        result.put("id", parts[4]);
        break;
      default:
        return null;
    }

    return result;
  }

  public static String getApplicationKey(String application) {
    return EcloudProvider.ID + SEPARATOR + APPLICATIONS + SEPARATOR + application.toLowerCase();
  }

  public static String getClusterKey(String clusterName, String application, String account) {
    return EcloudProvider.ID
        + SEPARATOR
        + CLUSTERS
        + SEPARATOR
        + account
        + SEPARATOR
        + application.toLowerCase()
        + SEPARATOR
        + clusterName;
  }

  public static String getServerGroupKey(String serverGroupName, String account, String region) {
    Names names = Names.parseName(serverGroupName);
    return getServerGroupKey(names.getCluster(), names.getGroup(), account, region);
  }

  public static String getServerGroupKey(
      String cluster, String serverGroupName, String account, String region) {
    return EcloudProvider.ID
        + SEPARATOR
        + SERVER_GROUPS
        + SEPARATOR
        + account
        + SEPARATOR
        + region
        + SEPARATOR
        + cluster
        + SEPARATOR
        + serverGroupName;
  }

  public static String getInstanceKey(String instanceId, String account, String region) {
    return EcloudProvider.ID
        + SEPARATOR
        + Namespace.INSTANCES
        + SEPARATOR
        + account
        + SEPARATOR
        + region
        + SEPARATOR
        + instanceId;
  }

  public static String getImageKey(String imageId, String account, String region) {
    return EcloudProvider.ID
        + SEPARATOR
        + IMAGES
        + SEPARATOR
        + account
        + SEPARATOR
        + region
        + SEPARATOR
        + imageId;
  }

  public static String getNamedImageKey(String imageName, String account) {
    return EcloudProvider.ID
        + SEPARATOR
        + NAMED_IMAGES
        + SEPARATOR
        + account
        + SEPARATOR
        + imageName;
  }

  public static String getKeyPairKey(String keyId, String account, String region) {
    return EcloudProvider.ID
        + SEPARATOR
        + Namespace.KEY_PAIRS
        + SEPARATOR
        + account
        + SEPARATOR
        + region
        + SEPARATOR
        + keyId;
  }

  public static String getInstanceTypeKey(
      String account, String region, String zone, String instanceType) {
    return EcloudProvider.ID
        + SEPARATOR
        + Namespace.INSTANCE_TYPES
        + SEPARATOR
        + account
        + SEPARATOR
        + region
        + SEPARATOR
        + zone
        + SEPARATOR
        + instanceType;
  }

  public static String getLoadBalancerKey(String loadBalancerId, String account, String region) {
    return EcloudProvider.ID
        + SEPARATOR
        + LOAD_BALANCERS
        + SEPARATOR
        + account
        + SEPARATOR
        + region
        + SEPARATOR
        + loadBalancerId;
  }

  public static String getSecurityGroupKey(
      String securityGroupId, String securityGroupName, String account, String region) {
    return EcloudProvider.ID
        + SEPARATOR
        + Namespace.SECURITY_GROUPS
        + SEPARATOR
        + securityGroupName
        + SEPARATOR
        + account
        + SEPARATOR
        + region
        + SEPARATOR
        + securityGroupId;
  }

  public static String getNetworkKey(String networkId, String account, String region) {
    return EcloudProvider.ID
        + SEPARATOR
        + Namespace.NETWORKS
        + SEPARATOR
        + account
        + SEPARATOR
        + region
        + SEPARATOR
        + networkId;
  }

  public static String getSubnetKey(String subnetId, String account, String region) {
    return EcloudProvider.ID
        + SEPARATOR
        + Namespace.SUBNETS
        + SEPARATOR
        + account
        + SEPARATOR
        + region
        + SEPARATOR
        + subnetId;
  }

  public static String getVpcKey(String vpcId, String account, String region) {
    return EcloudProvider.ID
        + SEPARATOR
        + VPCS
        + SEPARATOR
        + account
        + SEPARATOR
        + region
        + SEPARATOR
        + vpcId;
  }

  public static String getTargetHealthKey(
      String loadBalancerId, String poolId, String instanceId, String account, String region) {
    return EcloudProvider.ID
        + SEPARATOR
        + HEALTH_CHECKS
        + SEPARATOR
        + account
        + SEPARATOR
        + region
        + SEPARATOR
        + loadBalancerId
        + SEPARATOR
        + poolId
        + SEPARATOR
        + instanceId;
  }
}
