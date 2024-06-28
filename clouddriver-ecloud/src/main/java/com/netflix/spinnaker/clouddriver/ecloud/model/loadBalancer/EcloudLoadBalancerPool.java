package com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer;

import com.netflix.spinnaker.clouddriver.model.LoadBalancerServerGroup;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-09
 */
@Getter
@Setter
public class EcloudLoadBalancerPool extends LoadBalancerServerGroup {
  private String modifiedTime;

  private String lbAlgorithm;

  private String loadBalanceId;

  private Boolean isMultiAz;

  private String listenerId;

  private EcloudLoadBalancerHealth healthMonitorResp;

  private String protocol;

  private Boolean deleted;

  private List<EcloudLoadBalancerL7Policy> l7PolicyResps;

  private String listenerName;

  private String poolId;

  private String sessionPersistence;

  private String createdTime;

  private String multiAzUuid;

  private String cookieName;

  private String poolName;

  private List<EcloudLoadBalancerMember> members;
}
