package com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer;

import groovy.transform.Canonical;
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
@Canonical
public class EcloudLoadBalancerListener {
  private Integer healthDelay;

  private String modifiedTime;

  private String groupType;

  private String redirectToListenerId;

  private String sniContainerIds;

  private String description;

  private Boolean isMultiAz;

  private String redirectToListenerName;

  private String protocol;

  private List<String> sniContainerIdList;

  private String createdTime;

  private Boolean http2;

  private String id;

  private String listenerId;

  private String defaultTlsContainerId;

  private Boolean mutualAuthenticationUp;

  private String cookieName;

  private String poolName;

  private Boolean sniUp;

  private String lbAlgorithm;

  private String healthHttpMethod;

  private String healthId;

  private String healthType;

  private Integer loadBalanceFlavor;

  private String loadBalanceId;

  private Integer protocolPort;

  private String healthExpectedCode;

  private String groupName;

  private Integer connectionLimit;

  private Boolean deleted;

  private Integer healthMaxRetries;

  private String name;

  private String listenerName;

  private String poolId;

  private String sessionPersistence;

  private Boolean groupEnabled;

  private String healthUrlPath;

  private String caContainerId;

  private String opStatus;

  private String controlGroupId;

  private Integer healthTimeout;

  private String multiAzUuid;

  private Integer port;

  private List<EcloudLoadBalancerRule> rules;
}
