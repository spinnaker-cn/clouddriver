package com.netflix.spinnaker.clouddriver.ecloud.deploy.description;

import com.netflix.spinnaker.clouddriver.deploy.DeployDescription;
import com.netflix.spinnaker.clouddriver.security.resources.ServerGroupsNameable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xu.dangling
 * @date 2024/5/13 @Description
 */
@Slf4j
@Getter
@Setter
public class EcloudDeployDescription extends AbstractEcloudCredentialsDescription
    implements DeployDescription, Cloneable {

  private String application;
  private String stack;
  private String detail;
  private String region;
  private String accountName;
  private String serverGroupName;
  private String scalingConfigId;
  private String scalingGroupId;

  /** scaling config */
  private List<InstanceTypeRela> instanceTypeRelas;

  private String imageId;
  private int isPublic;
  private Set<String> securityGroups;
  private Boolean securityReinforce;
  private Disk systemDisk;
  private List<Disk> dataDisks;
  private Internet internet;
  private String keyPairName;
  private String roleName;

  /** auto-scaling */
  private Integer maxSize;

  private Integer minSize;
  private Integer desiredCapacity;
  private String routerId;
  private String multiRegionCreatePolicy;
  private List<SubnetRela> subnets;
  private List<ForwardLoadBalancer> forwardLoadBalancers;
  private String terminationPolicy;
  private Map<String, String> tags;

  private Source source;
  private Boolean copySourceScalingPoliciesAndActions = true;

  @Override
  public EcloudDeployDescription clone() {
    try {
      EcloudDeployDescription cloned = (EcloudDeployDescription) super.clone();

      // 对于简单类型和不可变类型的字段，浅拷贝是足够的
      // 对于复杂的可变类型，需要进行深拷贝
      if (instanceTypeRelas != null) {
        cloned.instanceTypeRelas = new ArrayList<>();
        for (InstanceTypeRela rela : instanceTypeRelas) {
          cloned.instanceTypeRelas.add(rela.clone());
        }
      }

      if (securityGroups != null) {
        cloned.securityGroups = new HashSet<>(securityGroups);
      }

      if (systemDisk != null) {
        cloned.systemDisk = systemDisk.clone();
      }

      if (dataDisks != null) {
        cloned.dataDisks = new ArrayList<>();
        for (Disk disk : dataDisks) {
          cloned.dataDisks.add(disk.clone());
        }
      }

      if (subnets != null) {
        cloned.subnets = new ArrayList<>();
        for (SubnetRela subnet : subnets) {
          cloned.subnets.add(subnet.clone());
        }
      }

      if (forwardLoadBalancers != null) {
        cloned.forwardLoadBalancers = new ArrayList<>();
        for (ForwardLoadBalancer balancer : forwardLoadBalancers) {
          cloned.forwardLoadBalancers.add(balancer.clone());
        }
      }

      if (tags != null) {
        cloned.tags = new HashMap<>(tags);
      }

      if (source != null) {
        cloned.source = source.clone();
      }

      return cloned;
    } catch (CloneNotSupportedException e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }

  @Getter
  @Setter
  public static class InstanceTypeRela implements Cloneable {
    private String instanceType;
    private int cpu;
    private int mem;
    private Integer index;

    @Override
    public InstanceTypeRela clone() throws CloneNotSupportedException {
      return (InstanceTypeRela) super.clone();
    }
  }

  @Getter
  @Setter
  public static class SubnetRela implements Cloneable {
    private String subnetId;
    private String networkId;
    private String zone;
    private Integer index;

    @Override
    public SubnetRela clone() throws CloneNotSupportedException {
      return (SubnetRela) super.clone();
    }
  }

  @Getter
  @Setter
  public static class ForwardLoadBalancer implements Cloneable {
    private String loadBalancerId;
    private String poolId;
    private Integer port;
    private Integer weight;
    private String subnetId;
    private String zone;

    @Override
    public ForwardLoadBalancer clone() throws CloneNotSupportedException {
      return (ForwardLoadBalancer) super.clone();
    }
  }

  @Getter
  @Setter
  public static class Internet implements Cloneable {
    private Boolean usePublicIp;
    private String chargeType;
    private Integer bandwidthSize;
    private String fipType;

    @Override
    public Internet clone() throws CloneNotSupportedException {
      return (Internet) super.clone();
    }
  }

  @Getter
  @Setter
  public static class Disk implements Cloneable {
    private String diskType;
    private Integer diskSize;

    @Override
    public Disk clone() throws CloneNotSupportedException {
      return (Disk) super.clone();
    }
  }

  @Getter
  @Setter
  public static class Source implements ServerGroupsNameable, Cloneable {
    private String region;
    private String serverGroupName;
    private Boolean useSourceCapacity = false;

    @Override
    public Collection<String> getServerGroupNames() {
      return Collections.singletonList(serverGroupName);
    }

    @Override
    public Source clone() throws CloneNotSupportedException {
      return (Source) super.clone();
    }
  }
}
