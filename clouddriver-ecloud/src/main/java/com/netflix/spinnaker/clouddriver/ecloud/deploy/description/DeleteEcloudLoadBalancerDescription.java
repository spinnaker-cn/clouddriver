package com.netflix.spinnaker.clouddriver.ecloud.deploy.description;

import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerListener;
import com.netflix.spinnaker.clouddriver.ecloud.model.loadBalancer.EcloudLoadBalancerPool;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-11
 */
@Getter
@Setter
public class DeleteEcloudLoadBalancerDescription extends AbstractEcloudCredentialsDescription {
  private String application;

  private String accountName;

  private String region;

  private String loadBalancerId;

  private List<EcloudLoadBalancerListener> listener;

  private List<EcloudLoadBalancerPool> pools;
}
